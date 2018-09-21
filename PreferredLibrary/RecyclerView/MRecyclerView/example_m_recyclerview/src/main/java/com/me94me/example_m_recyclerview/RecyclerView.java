
package com.me94me.example_m_recyclerview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.FocusFinder;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.Interpolator;
import android.widget.EdgeEffect;
import android.widget.LinearLayout;
import android.widget.OverScroller;

import com.me94me.example_m_recyclerview.other.AdapterHelper;
import com.me94me.example_m_recyclerview.other.ChildHelper;
import com.me94me.example_m_recyclerview.other.DefaultItemAnimator;
import com.me94me.example_m_recyclerview.other.FastScroller;
import com.me94me.example_m_recyclerview.other.ViewInfoStore;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.os.TraceCompat;
import androidx.core.util.Preconditions;
import androidx.core.view.InputDeviceCompat;
import androidx.core.view.MotionEventCompat;
import androidx.core.view.NestedScrollingChild2;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.ScrollingView;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewConfigurationCompat;
import androidx.core.view.accessibility.AccessibilityEventCompat;
import androidx.core.widget.EdgeEffectCompat;
import androidx.recyclerview.R;
import androidx.recyclerview.widget.RecyclerView.ItemAnimator.ItemHolderInfo;
import androidx.recyclerview.widget.RecyclerViewAccessibilityDelegate;
import androidx.viewpager.widget.ViewPager;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.core.view.ViewCompat.TYPE_NON_TOUCH;
import static androidx.core.view.ViewCompat.TYPE_TOUCH;

public class RecyclerView extends ViewGroup implements ScrollingView, NestedScrollingChild2 {

    static final String TAG = "RecyclerView";

    static final boolean DEBUG = false;

    static final boolean VERBOSE_TRACING = false;

    private static final int[] NESTED_SCROLLING_ATTRS = {16843830 /* android.R.attr.nestedScrollingEnabled */};

    private static final int[] CLIP_TO_PADDING_ATTR = {android.R.attr.clipToPadding};


    static final boolean FORCE_INVALIDATE_DISPLAY_LIST = Build.VERSION.SDK_INT == 18 || Build.VERSION.SDK_INT == 19 || Build.VERSION.SDK_INT == 20;

    static final boolean ALLOW_SIZE_IN_UNSPECIFIED_SPEC = Build.VERSION.SDK_INT >= 23;

    static final boolean POST_UPDATES_ON_ANIMATION = Build.VERSION.SDK_INT >= 16;


    /**
     * On L+, with RenderThread, the UI thread has idle time after it has passed a frame off to
     * RenderThread but before the next frame begins. We schedule prefetch work in this window.
     */
    static final boolean ALLOW_THREAD_GAP_WORK = Build.VERSION.SDK_INT >= 21;

    /**
     * FocusFinder#findNextFocus is broken on ICS MR1 and older for View.FOCUS_BACKWARD direction.
     * We convert it to an absolute direction such as FOCUS_DOWN or FOCUS_LEFT.
     */
    private static final boolean FORCE_ABS_FOCUS_SEARCH_DIRECTION = Build.VERSION.SDK_INT <= 15;

    /**
     * on API 15-, a focused child can still be considered a focused child of RV even after
     * it's being removed or its focusable flag is set to false. This is because when this focused
     * child is detached, the reference to this child is not removed in clearFocus. API 16 and above
     * properly handle this case by calling ensureInputFocusOnFirstFocusable or rootViewRequestFocus
     * to request focus on a new child, which will clear the focus on the old (detached) child as a
     * side-effect.
     */
    private static final boolean IGNORE_DETACHED_FOCUSED_CHILD = Build.VERSION.SDK_INT <= 15;

    static final boolean DISPATCH_TEMP_DETACH = false;


    @RestrictTo(LIBRARY_GROUP)
    @IntDef({HORIZONTAL, VERTICAL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Orientation {
    }

    public static final int HORIZONTAL = LinearLayout.HORIZONTAL;
    public static final int VERTICAL = LinearLayout.VERTICAL;


    static final int DEFAULT_ORIENTATION = VERTICAL;
    public static final int NO_POSITION = -1;
    public static final long NO_ID = -1;
    public static final int INVALID_TYPE = -1;

    /**
     * Constant for use with {@link #setScrollingTouchSlop(int)}. Indicates
     * that the RecyclerView should use the standard touch slop for smooth,
     * continuous scrolling.
     */
    public static final int TOUCH_SLOP_DEFAULT = 0;

    /**
     * Constant for use with {@link #setScrollingTouchSlop(int)}. Indicates
     * that the RecyclerView should use the standard touch slop for scrolling
     * widgets that snap to a page or other coarse-grained barrier.
     */
    public static final int TOUCH_SLOP_PAGING = 1;

    static final int MAX_SCROLL_DURATION = 2000;

    /**
     * RecyclerView is calculating a scroll.
     * If there are too many of these in Systrace, some Views inside RecyclerView might be causing
     * it. Try to avoid using EditText, focusable views or handle them with care.
     */
    static final String TRACE_SCROLL_TAG = "RV Scroll";

    /**
     * OnLayout has been called by the View system.
     * If this shows up too many times in Systrace, make sure the children of RecyclerView do not
     * update themselves directly. This will cause a full re-layout but when it happens via the
     * Adapter notifyItemChanged, RecyclerView can avoid full layout calculation.
     */
    private static final String TRACE_ON_LAYOUT_TAG = "RV OnLayout";

    /**
     * NotifyDataSetChanged or equal has been called.
     * If this is taking a long time, try sending granular notify adapter changes instead of just
     * calling notifyDataSetChanged or setAdapter / swapAdapter. Adding stable ids to your adapter
     * might help.
     */
    private static final String TRACE_ON_DATA_SET_CHANGE_LAYOUT_TAG = "RV FullInvalidate";

    /**
     * RecyclerView is doing a layout for partial adapter updates (we know what has changed)
     * If this is taking a long time, you may have dispatched too many Adapter updates causing too
     * many Views being rebind. Make sure all are necessary and also prefer using notify*Range
     * methods.
     */
    private static final String TRACE_HANDLE_ADAPTER_UPDATES_TAG = "RV PartialInvalidate";

    /**
     * RecyclerView is rebinding a View.
     * If this is taking a lot of time, consider optimizing your layout or make sure you are not
     * doing extra operations in onBindViewHolder call.
     */
    static final String TRACE_BIND_VIEW_TAG = "RV OnBindView";

    /**
     * RecyclerView is attempting to pre-populate off screen views.
     */
    static final String TRACE_PREFETCH_TAG = "RV Prefetch";

    /**
     * RecyclerView is attempting to pre-populate off screen itemviews within an off screen
     * RecyclerView.
     */
    static final String TRACE_NESTED_PREFETCH_TAG = "RV Nested Prefetch";

    /**
     * RecyclerView is creating a new View.
     * If too many of these present in Systrace:
     * - There might be a problem in Recycling (e.g. custom Animations that set transient state and
     * prevent recycling or ItemAnimator not implementing the contract properly. ({@link
     * > Adapter#onFailedToRecycleView(ViewHolder)})
     * <p>
     * - There might be too many item view types.
     * > Try merging them
     * <p>
     * - There might be too many itemChange animations and not enough space in RecyclerPool.
     * >Try increasing your pool size and item cache size.
     */
    static final String TRACE_CREATE_VIEW_TAG = "RV CreateView";

    //LayoutManager的构造
    private static final Class<?>[] LAYOUT_MANAGER_CONSTRUCTOR_SIGNATURE = new Class[]{Context.class, AttributeSet.class, int.class, int.class};

    private final RecyclerViewDataObserver mObserver = new RecyclerViewDataObserver();

    final Recycler mRecycler = new Recycler();

    private SavedState mPendingSavedState;

    /**
     * Handles adapter updates
     */
    public AdapterHelper mAdapterHelper;

    /**
     * Handles abstraction between LayoutManager children and RecyclerView children
     */
    ChildHelper mChildHelper;

    /**
     * Keeps data about views to be used for animations
     */
    final ViewInfoStore mViewInfoStore = new ViewInfoStore();

    /**
     * Prior to L, there is no way to query this variable which is why we override the setter and
     * track it here.
     */
    boolean mClipToPadding;

    /**
     * Note: this Runnable is only ever posted if:
     * 1) We've been through first layout
     * 2) We know we have a fixed size (mHasFixedSize)
     * 3) We're attached
     */
    final Runnable mUpdateChildViewsRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mFirstLayoutComplete || isLayoutRequested()) {
                // a layout request will happen, we should not do layout here.
                return;
            }
            if (!mIsAttached) {
                requestLayout();
                // if we are not attached yet, mark us as requiring layout and skip
                return;
            }
            if (mLayoutFrozen) {
                mLayoutWasDefered = true;
                return; //we'll process updates when ice age ends.
            }
            consumePendingUpdateOperations();
        }
    };

    final Rect mTempRect = new Rect();
    private final Rect mTempRect2 = new Rect();
    final RectF mTempRectF = new RectF();
    Adapter mAdapter;
    @VisibleForTesting
    LayoutManager mLayout;
    RecyclerListener mRecyclerListener;
    final ArrayList<ItemDecoration> mItemDecorations = new ArrayList<>();
    private final ArrayList<OnItemTouchListener> mOnItemTouchListeners =
            new ArrayList<>();
    private OnItemTouchListener mActiveOnItemTouchListener;
    boolean mIsAttached;
    boolean mHasFixedSize;

    boolean mEnableFastScroller;
    @VisibleForTesting
    boolean mFirstLayoutComplete;

    /**
     * The current depth of nested calls to {@link #startInterceptRequestLayout()} (number of
     * calls to {@link #startInterceptRequestLayout()} - number of calls to
     * {@link #stopInterceptRequestLayout(boolean)} .  This is used to signal whether we
     * should defer layout operations caused by layout requests from children of
     * {@link RecyclerView}.
     */
    private int mInterceptRequestLayoutDepth = 0;

    /**
     * True if a call to requestLayout was intercepted and prevented from executing like normal and
     * we plan on continuing with normal execution later.
     */
    boolean mLayoutWasDefered;

    boolean mLayoutFrozen;
    private boolean mIgnoreMotionEventTillDown;

    // binary OR of change events that were eaten during a layout or scroll.
    private int mEatenAccessibilityChangeFlags;
    boolean mAdapterUpdateDuringMeasure;

    private final AccessibilityManager mAccessibilityManager;
    private List<OnChildAttachStateChangeListener> mOnChildAttachStateListeners;

    /**
     * True after an event occurs that signals that the entire data set has changed. In that case,
     * we cannot run any animations since we don't know what happened until layout.
     * <p>
     * Attached items are invalid until next layout, at which point layout will animate/replace
     * items as necessary, building up content from the (effectively) new adapter from scratch.
     * <p>
     * Cached items must be discarded when setting this to true, so that the cache may be freely
     * used by prefetching until the next layout occurs.
     *
     * @see #processDataSetCompletelyChanged(boolean)
     */
    boolean mDataSetHasChangedAfterLayout = false;

    /**
     * True after the data set has completely changed and
     * {@link LayoutManager#onItemsChanged(RecyclerView)} should be called during the subsequent
     * measure/layout.
     *
     * @see #processDataSetCompletelyChanged(boolean)
     */
    boolean mDispatchItemsChangedEvent = false;

    /**
     * This variable is incremented during a dispatchLayout and/or scroll.
     * Some methods should not be called during these periods (e.g. adapter data change).
     * Doing so will create hard to find bugs so we better check it and throw an exception.
     *
     * @see #assertInLayoutOrScroll(String)
     * @see #assertNotInLayoutOrScroll(String)
     */
    private int mLayoutOrScrollCounter = 0;

    /**
     * Similar to mLayoutOrScrollCounter but logs a warning instead of throwing an exception
     * (for API compatibility).
     * <p>
     * It is a bad practice for a developer to update the data in a scroll callback since it is
     * potentially called during a layout.
     */
    private int mDispatchScrollCounter = 0;

    @NonNull
    private EdgeEffectFactory mEdgeEffectFactory = new EdgeEffectFactory();
    private EdgeEffect mLeftGlow, mTopGlow, mRightGlow, mBottomGlow;

    ItemAnimator mItemAnimator = new DefaultItemAnimator();

    private static final int INVALID_POINTER = -1;

    /**
     * The RecyclerView is not currently scrolling.
     *
     * @see #getScrollState()
     */
    public static final int SCROLL_STATE_IDLE = 0;

    /**
     * The RecyclerView is currently being dragged by outside input such as user touch input.
     *
     * @see #getScrollState()
     */
    public static final int SCROLL_STATE_DRAGGING = 1;

    /**
     * The RecyclerView is currently animating to a final position while not under
     * outside control.
     *
     * @see #getScrollState()
     */
    public static final int SCROLL_STATE_SETTLING = 2;

    static final long FOREVER_NS = Long.MAX_VALUE;

    // Touch/scrolling handling

    private int mScrollState = SCROLL_STATE_IDLE;
    private int mScrollPointerId = INVALID_POINTER;
    private VelocityTracker mVelocityTracker;
    private int mInitialTouchX;
    private int mInitialTouchY;
    private int mLastTouchX;
    private int mLastTouchY;
    private int mTouchSlop;
    private OnFlingListener mOnFlingListener;
    private final int mMinFlingVelocity;
    private final int mMaxFlingVelocity;

    // This value is used when handling rotary encoder generic motion events.
    private float mScaledHorizontalScrollFactor = Float.MIN_VALUE;
    private float mScaledVerticalScrollFactor = Float.MIN_VALUE;

    private boolean mPreserveFocusAfterLayout = true;

    final ViewFlinger mViewFlinger = new ViewFlinger();

    GapWorker mGapWorker;
    GapWorker.LayoutPrefetchRegistryImpl mPrefetchRegistry =
            ALLOW_THREAD_GAP_WORK ? new GapWorker.LayoutPrefetchRegistryImpl() : null;

    final State mState = new State();

    private OnScrollListener mScrollListener;
    private List<OnScrollListener> mScrollListeners;

    // For use in item animations
    boolean mItemsAddedOrRemoved = false;
    boolean mItemsChanged = false;

    //ItemAnimator监听器
    private ItemAnimator.ItemAnimatorListener mItemAnimatorListener = new ItemAnimatorRestoreListener();

    boolean mPostedAnimatorRunner = false;
    RecyclerViewAccessibilityDelegate mAccessibilityDelegate;
    private ChildDrawingOrderCallback mChildDrawingOrderCallback;

    // simple array to keep min and max child position during a layout calculation
    // preserved not to create a new one in each layout pass
    private final int[] mMinMaxLayoutPositions = new int[2];

    private NestedScrollingChildHelper mScrollingChildHelper;
    private final int[] mScrollOffset = new int[2];
    final int[] mScrollConsumed = new int[2];
    private final int[] mNestedOffsets = new int[2];

    /**
     * Reusable int array for use in calls to {@link #scrollStep(int, int, int[])} so that the
     * method may mutate it to "return" 2 ints.
     */
    final int[] mScrollStepConsumed = new int[2];

    /**
     * These are views that had their a11y importance changed during a layout. We defer these events
     * until the end of the layout because a11y service may make sync calls back to the RV while
     * the View's state is undefined.
     */
    @VisibleForTesting
    final List<ViewHolder> mPendingAccessibilityImportanceChange = new ArrayList<>();

    private Runnable mItemAnimatorRunner = new Runnable() {
        @Override
        public void run() {
            if (mItemAnimator != null) {
                mItemAnimator.runPendingAnimations();
            }
            mPostedAnimatorRunner = false;
        }
    };

    static final Interpolator sQuinticInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    /**
     * The callback to convert view info diffs into animations.
     */
    private final ViewInfoStore.ProcessCallback mViewInfoProcessCallback =
            new ViewInfoStore.ProcessCallback() {
                @Override
                public void processDisappeared(ViewHolder viewHolder, @NonNull ItemHolderInfo info,
                                               @Nullable ItemHolderInfo postInfo) {
                    mRecycler.unscrapView(viewHolder);
                    animateDisappearance(viewHolder, info, postInfo);
                }

                @Override
                public void processAppeared(ViewHolder viewHolder,
                                            ItemHolderInfo preInfo, ItemHolderInfo info) {
                    animateAppearance(viewHolder, preInfo, info);
                }

                @Override
                public void processPersistent(ViewHolder viewHolder,
                                              @NonNull ItemHolderInfo preInfo, @NonNull ItemHolderInfo postInfo) {
                    viewHolder.setIsRecyclable(false);
                    if (mDataSetHasChangedAfterLayout) {
                        // since it was rebound, use change instead as we'll be mapping them from
                        // stable ids. If stable ids were false, we would not be running any
                        // animations
                        if (mItemAnimator.animateChange(viewHolder, viewHolder, preInfo,
                                postInfo)) {
                            postAnimationRunner();
                        }
                    } else if (mItemAnimator.animatePersistence(viewHolder, preInfo, postInfo)) {
                        postAnimationRunner();
                    }
                }

                @Override
                public void unused(ViewHolder viewHolder) {
                    mLayout.removeAndRecycleView(viewHolder.itemView, mRecycler);
                }
            };

    /**
     * 构造方法
     */
    public RecyclerView(@NonNull Context context) {
        this(context, null);
    }

    public RecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        //clipToPadding
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, CLIP_TO_PADDING_ATTR, defStyle, 0);
            mClipToPadding = a.getBoolean(0, true);
            a.recycle();
        } else {
            mClipToPadding = true;
        }

        //是否作为可滚动容器
        setScrollContainer(true);

        //触摸模式下可获得焦点
        setFocusableInTouchMode(true);

        //表示滑动的时候，手的移动要大于这个距离才开始移动控件
        final ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();

        //MotionEventCompat.ACTION_SCROLL
        //为响应水平事件而滚动的数量。将此值乘以事件的轴值，以获得要滚动的像素数。
        mScaledHorizontalScrollFactor = ViewConfigurationCompat.getScaledHorizontalScrollFactor(vc, context);
        mScaledVerticalScrollFactor = ViewConfigurationCompat.getScaledVerticalScrollFactor(vc, context);

        //滚动的最小速度，以每秒像素数为单位。
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();

        //是否绘制
        setWillNotDraw(getOverScrollMode() == View.OVER_SCROLL_NEVER);

        //设置动画监听器
        mItemAnimator.setListener(mItemAnimatorListener);

        //初始化AdapterManager
        initAdapterManager();

        //初始化ChildrenHelper
        initChildrenHelper();

        //不支持自动填充
        initAutofill();

        //设置ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES
        // If not explicitly specified this view is important for accessibility.
        if (ViewCompat.getImportantForAccessibility(this) == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            ViewCompat.setImportantForAccessibility(this,ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }

        //获取AccessibilityManager
        mAccessibilityManager = (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);

        //设置RecyclerView的AccessibilityDelegate
        setAccessibilityDelegateCompat(new RecyclerViewAccessibilityDelegate(this));

        // 如果指定了LayoutManager就创建

        //默认支持嵌套滑动
        boolean nestedScrollingEnabled = true;

        if (attrs != null) {
            int defStyleRes = 0;
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RecyclerView, defStyle, defStyleRes);
            //获取layoutManager
            String layoutManagerName = a.getString(R.styleable.RecyclerView_layoutManager);

            //获取descendantFocusability
            //afterDescendants/beforeDescendants/blocksDescendants
            int descendantFocusability = a.getInt(R.styleable.RecyclerView_android_descendantFocusability, -1);
            //如果没有设置就设为afterDescendants
            if (descendantFocusability == -1) {
                setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            }

            //是否支持快速滑动
            mEnableFastScroller = a.getBoolean(R.styleable.RecyclerView_fastScrollEnabled, false);
            if (mEnableFastScroller) {
                //Vertical
                StateListDrawable verticalThumbDrawable = (StateListDrawable) a.getDrawable(R.styleable.RecyclerView_fastScrollVerticalThumbDrawable);
                Drawable verticalTrackDrawable = a.getDrawable(R.styleable.RecyclerView_fastScrollVerticalTrackDrawable);
                //Horizontal
                StateListDrawable horizontalThumbDrawable = (StateListDrawable) a.getDrawable(R.styleable.RecyclerView_fastScrollHorizontalThumbDrawable);
                Drawable horizontalTrackDrawable = a.getDrawable(R.styleable.RecyclerView_fastScrollHorizontalTrackDrawable);
                //初始化
                initFastScroller(verticalThumbDrawable, verticalTrackDrawable, horizontalThumbDrawable, horizontalTrackDrawable);
            }
            a.recycle();

            //创建LayoutManager
            createLayoutManager(context, layoutManagerName, attrs, defStyle, defStyleRes);

            if (Build.VERSION.SDK_INT >= 21) {
                //是否支持嵌套滑动
                a = context.obtainStyledAttributes(attrs, NESTED_SCROLLING_ATTRS, defStyle, defStyleRes);
                nestedScrollingEnabled = a.getBoolean(0, true);
                a.recycle();
            }
        } else {
            //设置afterDescendants
            setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        }
        //所有Api支持嵌套滑动
        // Re-set whether nested scrolling is enabled so that it is set on all API levels
        setNestedScrollingEnabled(nestedScrollingEnabled);
    }

    /**
     * Label appended to all public exception strings, used to help find which RV in an app is
     * hitting an exception.
     */
    String exceptionLabel() {
        return " " + super.toString()
                + ", adapter:" + mAdapter
                + ", layout:" + mLayout
                + ", context:" + getContext();
    }

    /**
     * 如果没有明确规定，该view和该view的children不支持自动填充
     * This is done because autofill's means of uniquely identifying views doesn't work out of the box with View recycling.
     */
    @SuppressLint("InlinedApi")
    private void initAutofill() {
        if (ViewCompat.getImportantForAutofill(this) == View.IMPORTANT_FOR_AUTOFILL_AUTO) {
            ViewCompat.setImportantForAutofill(this, View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        }
    }

    /**
     * Returns the accessibility delegate compatibility implementation used by the RecyclerView.
     *
     * @return An instance of AccessibilityDelegateCompat used by RecyclerView
     */
    @Nullable
    public RecyclerViewAccessibilityDelegate getCompatAccessibilityDelegate() {
        return mAccessibilityDelegate;
    }

    /**
     * Sets the accessibility delegate compatibility implementation used by RecyclerView.
     * @param accessibilityDelegate The accessibility delegate to be used by RecyclerView.
     */
    public void setAccessibilityDelegateCompat(@Nullable RecyclerViewAccessibilityDelegate accessibilityDelegate) {
        mAccessibilityDelegate = accessibilityDelegate;
        ViewCompat.setAccessibilityDelegate(this, mAccessibilityDelegate);
    }


    /**
     * 如果在xml指定了实例化并创建一个LayoutManager
     * Instantiate and set a LayoutManager, if specified in the attributes.
     */
    private void createLayoutManager(Context context, String className, AttributeSet attrs,
                                     int defStyleAttr, int defStyleRes) {
        if (className != null) {
            className = className.trim();
            if (!className.isEmpty()) {
                //获取完整路径
                className = getFullClassName(context, className);
                try {
                    ClassLoader classLoader;
                    if (isInEditMode()) {
                        // Stupid layoutlib cannot handle simple class loaders.
                        classLoader = this.getClass().getClassLoader();
                    } else {
                        classLoader = context.getClassLoader();
                    }
                    Class<? extends LayoutManager> layoutManagerClass = classLoader.loadClass(className).asSubclass(LayoutManager.class);
                    Constructor<? extends LayoutManager> constructor;
                    Object[] constructorArgs = null;
                    try {
                        constructor = layoutManagerClass.getConstructor(LAYOUT_MANAGER_CONSTRUCTOR_SIGNATURE);
                        constructorArgs = new Object[]{context, attrs, defStyleAttr, defStyleRes};
                    } catch (NoSuchMethodException e) {
                        try {
                            constructor = layoutManagerClass.getConstructor();
                        } catch (NoSuchMethodException e1) {
                            e1.initCause(e);
                            throw new IllegalStateException(attrs.getPositionDescription()
                                    + ": Error creating LayoutManager " + className, e1);
                        }
                    }
                    constructor.setAccessible(true);
                    //反射实例化并为RecyclerView设置LayoutManager
                    setLayoutManager(constructor.newInstance(constructorArgs));
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(attrs.getPositionDescription()
                            + ": Unable to find LayoutManager " + className, e);
                } catch (InvocationTargetException e) {
                    throw new IllegalStateException(attrs.getPositionDescription()
                            + ": Could not instantiate the LayoutManager: " + className, e);
                } catch (InstantiationException e) {
                    throw new IllegalStateException(attrs.getPositionDescription()
                            + ": Could not instantiate the LayoutManager: " + className, e);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(attrs.getPositionDescription()
                            + ": Cannot access non-public constructor " + className, e);
                } catch (ClassCastException e) {
                    throw new IllegalStateException(attrs.getPositionDescription()
                            + ": Class is not a LayoutManager " + className, e);
                }
            }
        }
    }

    /**
     * 获取LayoutManager的完整路径
     */
    private String getFullClassName(Context context, String className) {
        if (className.charAt(0) == '.') {
            return context.getPackageName() + className;
        }
        if (className.contains(".")) {
            return className;
        }
        return RecyclerView.class.getPackage().getName() + '.' + className;
    }


    /**
     * 初始化ChildrenHelper
     */
    private void initChildrenHelper() {
        mChildHelper = new ChildHelper(new ChildHelper.Callback() {
            @Override
            public int getChildCount() {
                return RecyclerView.this.getChildCount();
            }

            @Override
            public void addView(View child, int index) {
                if (VERBOSE_TRACING) {
                    TraceCompat.beginSection("RV addView");
                }
                RecyclerView.this.addView(child, index);
                if (VERBOSE_TRACING) {
                    TraceCompat.endSection();
                }
                dispatchChildAttached(child);
            }

            @Override
            public int indexOfChild(View view) {
                return RecyclerView.this.indexOfChild(view);
            }

            @Override
            public void removeViewAt(int index) {
                final View child = RecyclerView.this.getChildAt(index);
                if (child != null) {
                    dispatchChildDetached(child);

                    // Clear any android.view.animation.Animation that may prevent the item from
                    // detaching when being removed. If a child is re-added before the
                    // lazy detach occurs, it will receive invalid attach/detach sequencing.
                    child.clearAnimation();
                }
                if (VERBOSE_TRACING) {
                    TraceCompat.beginSection("RV removeViewAt");
                }
                RecyclerView.this.removeViewAt(index);
                if (VERBOSE_TRACING) {
                    TraceCompat.endSection();
                }
            }

            @Override
            public View getChildAt(int offset) {
                return RecyclerView.this.getChildAt(offset);
            }

            @Override
            public void removeAllViews() {
                final int count = getChildCount();
                for (int i = 0; i < count; i++) {
                    View child = getChildAt(i);
                    dispatchChildDetached(child);

                    // Clear any android.view.animation.Animation that may prevent the item from
                    // detaching when being removed. If a child is re-added before the
                    // lazy detach occurs, it will receive invalid attach/detach sequencing.
                    child.clearAnimation();
                }
                RecyclerView.this.removeAllViews();
            }

            @Override
            public ViewHolder getChildViewHolder(View view) {
                return getChildViewHolderInt(view);
            }

            @Override
            public void attachViewToParent(View child, int index,
                                           ViewGroup.LayoutParams layoutParams) {
                final ViewHolder vh = getChildViewHolderInt(child);
                if (vh != null) {
                    if (!vh.isTmpDetached() && !vh.shouldIgnore()) {
                        throw new IllegalArgumentException("Called attach on a child which is not"
                                + " detached: " + vh + exceptionLabel());
                    }
                    if (DEBUG) {
                        Log.d(TAG, "reAttach " + vh);
                    }
                    vh.clearTmpDetachFlag();
                }
                RecyclerView.this.attachViewToParent(child, index, layoutParams);
            }

            @Override
            public void detachViewFromParent(int offset) {
                final View view = getChildAt(offset);
                if (view != null) {
                    final ViewHolder vh = getChildViewHolderInt(view);
                    if (vh != null) {
                        if (vh.isTmpDetached() && !vh.shouldIgnore()) {
                            throw new IllegalArgumentException("called detach on an already"
                                    + " detached child " + vh + exceptionLabel());
                        }
                        if (DEBUG) {
                            Log.d(TAG, "tmpDetach " + vh);
                        }
                        vh.addFlags(ViewHolder.FLAG_TMP_DETACHED);
                    }
                }
                RecyclerView.this.detachViewFromParent(offset);
            }

            @Override
            public void onEnteredHiddenState(View child) {
                final ViewHolder vh = getChildViewHolderInt(child);
                if (vh != null) {
                    vh.onEnteredHiddenState(RecyclerView.this);
                }
            }

            @Override
            public void onLeftHiddenState(View child) {
                final ViewHolder vh = getChildViewHolderInt(child);
                if (vh != null) {
                    vh.onLeftHiddenState(RecyclerView.this);
                }
            }
        });
    }


    /**
     * 初始化AdapterManager
     */
    void initAdapterManager() {
        mAdapterHelper = new AdapterHelper(new AdapterHelper.Callback() {
            @Override
            public ViewHolder findViewHolder(int position) {
                final ViewHolder vh = findViewHolderForPosition(position, true);
                if (vh == null) {
                    return null;
                }
                if (mChildHelper.isHidden(vh.itemView)) {
                    return null;
                }
                return vh;
            }

            @Override
            public void offsetPositionsForRemovingInvisible(int start, int count) {
                offsetPositionRecordsForRemove(start, count, true);
                mItemsAddedOrRemoved = true;
                mState.mDeletedInvisibleItemCountSincePreviousLayout += count;
            }

            @Override
            public void offsetPositionsForRemovingLaidOutOrNewView(
                    int positionStart, int itemCount) {
                offsetPositionRecordsForRemove(positionStart, itemCount, false);
                mItemsAddedOrRemoved = true;
            }


            @Override
            public void markViewHoldersUpdated(int positionStart, int itemCount, Object payload) {
                viewRangeUpdate(positionStart, itemCount, payload);
                mItemsChanged = true;
            }

            @Override
            public void onDispatchFirstPass(AdapterHelper.UpdateOp op) {
                dispatchUpdate(op);
            }

            void dispatchUpdate(AdapterHelper.UpdateOp op) {
                switch (op.cmd) {
                    case AdapterHelper.UpdateOp.ADD:
                        mLayout.onItemsAdded(RecyclerView.this, op.positionStart, op.itemCount);
                        break;
                    case AdapterHelper.UpdateOp.REMOVE:
                        mLayout.onItemsRemoved(RecyclerView.this, op.positionStart, op.itemCount);
                        break;
                    case AdapterHelper.UpdateOp.UPDATE:
                        mLayout.onItemsUpdated(RecyclerView.this, op.positionStart, op.itemCount,
                                op.payload);
                        break;
                    case AdapterHelper.UpdateOp.MOVE:
                        mLayout.onItemsMoved(RecyclerView.this, op.positionStart, op.itemCount, 1);
                        break;
                }
            }

            @Override
            public void onDispatchSecondPass(AdapterHelper.UpdateOp op) {
                dispatchUpdate(op);
            }

            @Override
            public void offsetPositionsForAdd(int positionStart, int itemCount) {
                offsetPositionRecordsForInsert(positionStart, itemCount);
                mItemsAddedOrRemoved = true;
            }

            @Override
            public void offsetPositionsForMove(int from, int to) {
                offsetPositionRecordsForMove(from, to);
                // should we create mItemsMoved ?
                mItemsAddedOrRemoved = true;
            }
        });
    }




    /**
     * RecyclerView can perform several optimizations if it can know in advance that RecyclerView's
     * size is not affected by the adapter contents. RecyclerView can still change its size based
     * on other factors (e.g. its parent's size) but this size calculation cannot depend on the
     * size of its children or contents of its adapter (except the number of items in the adapter).
     * <p>
     * If your use of RecyclerView falls into this category, set this to {@code true}. It will allow
     * RecyclerView to avoid invalidating the whole layout when its adapter contents change.
     *
     * @param hasFixedSize true if adapter changes cannot affect the size of the RecyclerView.
     */
    public void setHasFixedSize(boolean hasFixedSize) {
        mHasFixedSize = hasFixedSize;
    }

    /**
     * @return true if the app has specified that changes in adapter content cannot change
     * the size of the RecyclerView itself.
     */
    public boolean hasFixedSize() {
        return mHasFixedSize;
    }

    @Override
    public void setClipToPadding(boolean clipToPadding) {
        if (clipToPadding != mClipToPadding) {
            invalidateGlows();
        }
        mClipToPadding = clipToPadding;
        super.setClipToPadding(clipToPadding);
        if (mFirstLayoutComplete) {
            requestLayout();
        }
    }

    /**
     * Returns whether this RecyclerView will clip its children to its padding, and resize (but
     * not clip) any EdgeEffect to the padded region, if padding is present.
     * <p>
     * By default, children are clipped to the padding of their parent
     * RecyclerView. This clipping behavior is only enabled if padding is non-zero.
     *
     * @return true if this RecyclerView clips children to its padding and resizes (but doesn't
     * clip) any EdgeEffect to the padded region, false otherwise.
     * @attr name android:clipToPadding
     */
    @Override
    public boolean getClipToPadding() {
        return mClipToPadding;
    }

    /**
     * Configure the scrolling touch slop for a specific use case.
     * <p>
     * Set up the RecyclerView's scrolling motion threshold based on common usages.
     * Valid arguments are {@link #TOUCH_SLOP_DEFAULT} and {@link #TOUCH_SLOP_PAGING}.
     *
     * @param slopConstant One of the <code>TOUCH_SLOP_</code> constants representing
     *                     the intended usage of this RecyclerView
     */
    public void setScrollingTouchSlop(int slopConstant) {
        final ViewConfiguration vc = ViewConfiguration.get(getContext());
        switch (slopConstant) {
            default:
                Log.w(TAG, "setScrollingTouchSlop(): bad argument constant "
                        + slopConstant + "; using default value");
                // fall-through
            case TOUCH_SLOP_DEFAULT:
                mTouchSlop = vc.getScaledTouchSlop();
                break;

            case TOUCH_SLOP_PAGING:
                mTouchSlop = vc.getScaledPagingTouchSlop();
                break;
        }
    }

    /**
     * Swaps the current adapter with the provided one. It is similar to
     * {@link #setAdapter(Adapter)} but assumes existing adapter and the new adapter uses the same
     * {@link ViewHolder} and does not clear the RecycledViewPool.
     * <p>
     * Note that it still calls onAdapterChanged callbacks.
     *
     * @param adapter                       The new adapter to set, or null to set no adapter.
     * @param removeAndRecycleExistingViews If set to true, RecyclerView will recycle all existing
     *                                      Views. If adapters have stable ids and/or you want to
     *                                      animate the disappearing views, you may prefer to set
     *                                      this to false.
     * @see #setAdapter(Adapter)
     */
    public void swapAdapter(@Nullable Adapter adapter, boolean removeAndRecycleExistingViews) {
        // bail out if layout is frozen
        setLayoutFrozen(false);
        setAdapterInternal(adapter, true, removeAndRecycleExistingViews);
        processDataSetCompletelyChanged(true);
        requestLayout();
    }

    /**
     * Set a new adapter to provide child views on demand.
     * <p>
     * When adapter is changed, all existing views are recycled back to the pool. If the pool has
     * only one adapter, it will be cleared.
     *
     * @param adapter The new adapter to set, or null to set no adapter.
     * @see #swapAdapter(Adapter, boolean)
     */
    public void setAdapter(@Nullable Adapter adapter) {
        // bail out if layout is frozen
        setLayoutFrozen(false);
        setAdapterInternal(adapter, false, true);
        processDataSetCompletelyChanged(false);
        requestLayout();
    }

    /**
     * Removes and recycles all views - both those currently attached, and those in the Recycler.
     */
    void removeAndRecycleViews() {
        // end all running animations
        if (mItemAnimator != null) {
            mItemAnimator.endAnimations();
        }
        // Since animations are ended, mLayout.children should be equal to
        // recyclerView.children. This may not be true if item animator's end does not work as
        // expected. (e.g. not release children instantly). It is safer to use mLayout's child
        // count.
        if (mLayout != null) {
            mLayout.removeAndRecycleAllViews(mRecycler);
            mLayout.removeAndRecycleScrapInt(mRecycler);
        }
        // we should clear it here before adapters are swapped to ensure correct callbacks.
        mRecycler.clear();
    }

    /**
     * Replaces the current adapter with the new one and triggers listeners.
     *
     * @param adapter                The new adapter
     * @param compatibleWithPrevious If true, the new adapter is using the same View Holders and
     *                               item types with the current adapter (helps us avoid cache
     *                               invalidation).
     * @param removeAndRecycleViews  If true, we'll remove and recycle all existing views. If
     *                               compatibleWithPrevious is false, this parameter is ignored.
     */
    private void setAdapterInternal(@Nullable Adapter adapter, boolean compatibleWithPrevious,
                                    boolean removeAndRecycleViews) {
        if (mAdapter != null) {
            mAdapter.unregisterAdapterDataObserver(mObserver);
            mAdapter.onDetachedFromRecyclerView(this);
        }
        if (!compatibleWithPrevious || removeAndRecycleViews) {
            removeAndRecycleViews();
        }
        mAdapterHelper.reset();
        final Adapter oldAdapter = mAdapter;
        mAdapter = adapter;
        if (adapter != null) {
            adapter.registerAdapterDataObserver(mObserver);
            adapter.onAttachedToRecyclerView(this);
        }
        if (mLayout != null) {
            mLayout.onAdapterChanged(oldAdapter, mAdapter);
        }
        mRecycler.onAdapterChanged(oldAdapter, mAdapter, compatibleWithPrevious);
        mState.mStructureChanged = true;
    }

    /**
     * Retrieves the previously set adapter or null if no adapter is set.
     *
     * @return The previously set adapter
     * @see #setAdapter(Adapter)
     */
    @Nullable
    public Adapter getAdapter() {
        return mAdapter;
    }

    /**
     * Register a listener that will be notified whenever a child view is recycled.
     *
     * <p>This listener will be called when a LayoutManager or the RecyclerView decides
     * that a child view is no longer needed. If an application associates expensive
     * or heavyweight data with item views, this may be a good place to release
     * or free those resources.</p>
     *
     * @param listener Listener to register, or null to clear
     */
    public void setRecyclerListener(@Nullable RecyclerListener listener) {
        mRecyclerListener = listener;
    }

    /**
     * <p>Return the offset of the RecyclerView's text baseline from the its top
     * boundary. If the LayoutManager of this RecyclerView does not support baseline alignment,
     * this method returns -1.</p>
     *
     * @return the offset of the baseline within the RecyclerView's bounds or -1
     * if baseline alignment is not supported
     */
    @Override
    public int getBaseline() {
        if (mLayout != null) {
            return mLayout.getBaseline();
        } else {
            return super.getBaseline();
        }
    }

    /**
     * Register a listener that will be notified whenever a child view is attached to or detached
     * from RecyclerView.
     *
     * <p>This listener will be called when a LayoutManager or the RecyclerView decides
     * that a child view is no longer needed. If an application associates expensive
     * or heavyweight data with item views, this may be a good place to release
     * or free those resources.</p>
     *
     * @param listener Listener to register
     */
    public void addOnChildAttachStateChangeListener(
            @NonNull OnChildAttachStateChangeListener listener) {
        if (mOnChildAttachStateListeners == null) {
            mOnChildAttachStateListeners = new ArrayList<>();
        }
        mOnChildAttachStateListeners.add(listener);
    }

    /**
     * Removes the provided listener from child attached state listeners list.
     *
     * @param listener Listener to unregister
     */
    public void removeOnChildAttachStateChangeListener(
            @NonNull OnChildAttachStateChangeListener listener) {
        if (mOnChildAttachStateListeners == null) {
            return;
        }
        mOnChildAttachStateListeners.remove(listener);
    }

    /**
     * Removes all listeners that were added via
     * {@link #addOnChildAttachStateChangeListener(OnChildAttachStateChangeListener)}.
     */
    public void clearOnChildAttachStateChangeListeners() {
        if (mOnChildAttachStateListeners != null) {
            mOnChildAttachStateListeners.clear();
        }
    }


    /**
     * 设置LayoutManager
     * @param layout LayoutManager to use
     */
    public void setLayoutManager(@Nullable LayoutManager layout) {
        if (layout == mLayout) {
            return;
        }
        stopScroll();
        // TODO We should do this switch a dispatchLayout pass and animate children. There is a good
        // chance that LayoutManagers will re-use views.
        if (mLayout != null) {
            // end all running animations
            if (mItemAnimator != null) {
                mItemAnimator.endAnimations();
            }
            mLayout.removeAndRecycleAllViews(mRecycler);
            mLayout.removeAndRecycleScrapInt(mRecycler);
            mRecycler.clear();

            if (mIsAttached) {
                mLayout.dispatchDetachedFromWindow(this, mRecycler);
            }
            mLayout.setRecyclerView(null);
            mLayout = null;
        } else {
            mRecycler.clear();
        }
        // this is just a defensive measure for faulty item animators.
        mChildHelper.removeAllViewsUnfiltered();
        mLayout = layout;
        if (layout != null) {
            if (layout.mRecyclerView != null) {
                throw new IllegalArgumentException("LayoutManager " + layout
                        + " is already attached to a RecyclerView:"
                        + layout.mRecyclerView.exceptionLabel());
            }
            mLayout.setRecyclerView(this);
            if (mIsAttached) {
                mLayout.dispatchAttachedToWindow(this);
            }
        }
        mRecycler.updateViewCacheSize();
        requestLayout();
    }

    /**
     * Set a {@link OnFlingListener} for this {@link RecyclerView}.
     * <p>
     * If the {@link OnFlingListener} is set then it will receive
     * calls to {@link #fling(int, int)} and will be able to intercept them.
     *
     * @param onFlingListener The {@link OnFlingListener} instance.
     */
    public void setOnFlingListener(@Nullable OnFlingListener onFlingListener) {
        mOnFlingListener = onFlingListener;
    }

    /**
     * Get the current {@link OnFlingListener} from this {@link RecyclerView}.
     *
     * @return The {@link OnFlingListener} instance currently set (can be null).
     */
    @Nullable
    public OnFlingListener getOnFlingListener() {
        return mOnFlingListener;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());
        if (mPendingSavedState != null) {
            state.copyFrom(mPendingSavedState);
        } else if (mLayout != null) {
            state.mLayoutState = mLayout.onSaveInstanceState();
        } else {
            state.mLayoutState = null;
        }

        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        mPendingSavedState = (SavedState) state;
        super.onRestoreInstanceState(mPendingSavedState.getSuperState());
        if (mLayout != null && mPendingSavedState.mLayoutState != null) {
            mLayout.onRestoreInstanceState(mPendingSavedState.mLayoutState);
        }
    }

    /**
     * Override to prevent freezing of any views created by the adapter.
     */
    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        dispatchFreezeSelfOnly(container);
    }

    /**
     * Override to prevent thawing of any views created by the adapter.
     */
    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }

    /**
     * Adds a view to the animatingViews list.
     * mAnimatingViews holds the child views that are currently being kept around
     * purely for the purpose of being animated out of view. They are drawn as a regular
     * part of the child list of the RecyclerView, but they are invisible to the LayoutManager
     * as they are managed separately from the regular child views.
     *
     * @param viewHolder The ViewHolder to be removed
     */
    private void addAnimatingView(ViewHolder viewHolder) {
        final View view = viewHolder.itemView;
        final boolean alreadyParented = view.getParent() == this;
        mRecycler.unscrapView(getChildViewHolder(view));
        if (viewHolder.isTmpDetached()) {
            // re-attach
            mChildHelper.attachViewToParent(view, -1, view.getLayoutParams(), true);
        } else if (!alreadyParented) {
            mChildHelper.addView(view, true);
        } else {
            mChildHelper.hide(view);
        }
    }

    /**
     * Removes a view from the animatingViews list.
     *
     * @param view The view to be removed
     * @return true if an animating view is removed
     * @see #addAnimatingView(RecyclerView.ViewHolder)
     */
    boolean removeAnimatingView(View view) {
        startInterceptRequestLayout();
        final boolean removed = mChildHelper.removeViewIfHidden(view);
        if (removed) {
            final ViewHolder viewHolder = getChildViewHolderInt(view);
            mRecycler.unscrapView(viewHolder);
            mRecycler.recycleViewHolderInternal(viewHolder);
            if (DEBUG) {
                Log.d(TAG, "after removing animated view: " + view + ", " + this);
            }
        }
        // only clear request eaten flag if we removed the view.
        stopInterceptRequestLayout(!removed);
        return removed;
    }

    /**
     * Return the {@link LayoutManager} currently responsible for
     * layout policy for this RecyclerView.
     *
     * @return The currently bound LayoutManager
     */
    @Nullable
    public LayoutManager getLayoutManager() {
        return mLayout;
    }

    /**
     * Retrieve this RecyclerView's {@link RecycledViewPool}. This method will never return null;
     * if no pool is set for this view a new one will be created. See
     * {@link #setRecycledViewPool(RecycledViewPool) setRecycledViewPool} for more information.
     *
     * @return The pool used to store recycled item views for reuse.
     * @see #setRecycledViewPool(RecycledViewPool)
     */
    @NonNull
    public RecycledViewPool getRecycledViewPool() {
        return mRecycler.getRecycledViewPool();
    }

    /**
     * Recycled view pools allow multiple RecyclerViews to share a common pool of scrap views.
     * This can be useful if you have multiple RecyclerViews with adapters that use the same
     * view types, for example if you have several data sets with the same kinds of item views
     * displayed by a {@link ViewPager ViewPager}.
     *
     * @param pool Pool to set. If this parameter is null a new pool will be created and used.
     */
    public void setRecycledViewPool(@Nullable RecycledViewPool pool) {
        mRecycler.setRecycledViewPool(pool);
    }

    /**
     * Sets a new {@link ViewCacheExtension} to be used by the Recycler.
     *
     * @param extension ViewCacheExtension to be used or null if you want to clear the existing one.
     * @see ViewCacheExtension#getViewForPositionAndType(Recycler, int, int)
     */
    public void setViewCacheExtension(@Nullable ViewCacheExtension extension) {
        mRecycler.setViewCacheExtension(extension);
    }

    /**
     * Set the number of offscreen views to retain before adding them to the potentially shared
     * {@link #getRecycledViewPool() recycled view pool}.
     *
     * <p>The offscreen view cache stays aware of changes in the attached adapter, allowing
     * a LayoutManager to reuse those views unmodified without needing to return to the adapter
     * to rebind them.</p>
     *
     * @param size Number of views to cache offscreen before returning them to the general
     *             recycled view pool
     */
    public void setItemViewCacheSize(int size) {
        mRecycler.setViewCacheSize(size);
    }

    /**
     * Return the current scrolling state of the RecyclerView.
     *
     * @return {@link #SCROLL_STATE_IDLE}, {@link #SCROLL_STATE_DRAGGING} or
     * {@link #SCROLL_STATE_SETTLING}
     */
    public int getScrollState() {
        return mScrollState;
    }

    void setScrollState(int state) {
        if (state == mScrollState) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "setting scroll state to " + state + " from " + mScrollState,
                    new Exception());
        }
        mScrollState = state;
        if (state != SCROLL_STATE_SETTLING) {
            stopScrollersInternal();
        }
        dispatchOnScrollStateChanged(state);
    }

    /**
     * Add an {@link ItemDecoration} to this RecyclerView. Item decorations can
     * affect both measurement and drawing of individual item views.
     *
     * <p>Item decorations are ordered. Decorations placed earlier in the list will
     * be run/queried/drawn first for their effects on item views. Padding added to views
     * will be nested; a padding added by an earlier decoration will mean further
     * item decorations in the list will be asked to draw/pad within the previous decoration's
     * given area.</p>
     *
     * @param decor Decoration to add
     * @param index Position in the decoration chain to insert this decoration at. If this value
     *              is negative the decoration will be added at the end.
     */
    public void addItemDecoration(@NonNull ItemDecoration decor, int index) {
        if (mLayout != null) {
            mLayout.assertNotInLayoutOrScroll("Cannot add item decoration during a scroll  or"
                    + " layout");
        }
        if (mItemDecorations.isEmpty()) {
            setWillNotDraw(false);
        }
        if (index < 0) {
            mItemDecorations.add(decor);
        } else {
            mItemDecorations.add(index, decor);
        }
        markItemDecorInsetsDirty();
        requestLayout();
    }

    /**
     * Add an {@link ItemDecoration} to this RecyclerView. Item decorations can
     * affect both measurement and drawing of individual item views.
     *
     * <p>Item decorations are ordered. Decorations placed earlier in the list will
     * be run/queried/drawn first for their effects on item views. Padding added to views
     * will be nested; a padding added by an earlier decoration will mean further
     * item decorations in the list will be asked to draw/pad within the previous decoration's
     * given area.</p>
     *
     * @param decor Decoration to add
     */
    public void addItemDecoration(@NonNull ItemDecoration decor) {
        addItemDecoration(decor, -1);
    }

    /**
     * Returns an {@link ItemDecoration} previously added to this RecyclerView.
     *
     * @param index The index position of the desired ItemDecoration.
     * @return the ItemDecoration at index position
     * @throws IndexOutOfBoundsException on invalid index
     */
    @NonNull
    public ItemDecoration getItemDecorationAt(int index) {
        final int size = getItemDecorationCount();
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(index + " is an invalid index for size " + size);
        }

        return mItemDecorations.get(index);
    }

    /**
     * Returns the number of {@link ItemDecoration} currently added to this RecyclerView.
     *
     * @return number of ItemDecorations currently added added to this RecyclerView.
     */
    public int getItemDecorationCount() {
        return mItemDecorations.size();
    }

    /**
     * Removes the {@link ItemDecoration} associated with the supplied index position.
     *
     * @param index The index position of the ItemDecoration to be removed.
     */
    public void removeItemDecorationAt(int index) {
        final int size = getItemDecorationCount();
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(index + " is an invalid index for size " + size);
        }

        removeItemDecoration(getItemDecorationAt(index));
    }

    /**
     * Remove an {@link ItemDecoration} from this RecyclerView.
     *
     * <p>The given decoration will no longer impact the measurement and drawing of
     * item views.</p>
     *
     * @param decor Decoration to remove
     * @see #addItemDecoration(ItemDecoration)
     */
    public void removeItemDecoration(@NonNull ItemDecoration decor) {
        if (mLayout != null) {
            mLayout.assertNotInLayoutOrScroll("Cannot remove item decoration during a scroll  or"
                    + " layout");
        }
        mItemDecorations.remove(decor);
        if (mItemDecorations.isEmpty()) {
            setWillNotDraw(getOverScrollMode() == View.OVER_SCROLL_NEVER);
        }
        markItemDecorInsetsDirty();
        requestLayout();
    }

    /**
     * Sets the {@link ChildDrawingOrderCallback} to be used for drawing children.
     * <p>
     * See {@link ViewGroup#getChildDrawingOrder(int, int)} for details. Calling this method will
     * always call {@link ViewGroup#setChildrenDrawingOrderEnabled(boolean)}. The parameter will be
     * true if childDrawingOrderCallback is not null, false otherwise.
     * <p>
     * Note that child drawing order may be overridden by View's elevation.
     *
     * @param childDrawingOrderCallback The ChildDrawingOrderCallback to be used by the drawing
     *                                  system.
     */
    public void setChildDrawingOrderCallback(
            @Nullable ChildDrawingOrderCallback childDrawingOrderCallback) {
        if (childDrawingOrderCallback == mChildDrawingOrderCallback) {
            return;
        }
        mChildDrawingOrderCallback = childDrawingOrderCallback;
        setChildrenDrawingOrderEnabled(mChildDrawingOrderCallback != null);
    }

    /**
     * Set a listener that will be notified of any changes in scroll state or position.
     *
     * @param listener Listener to set or null to clear
     * @deprecated Use {@link #addOnScrollListener(OnScrollListener)} and
     * {@link #removeOnScrollListener(OnScrollListener)}
     */
    @Deprecated
    public void setOnScrollListener(@Nullable OnScrollListener listener) {
        mScrollListener = listener;
    }

    /**
     * Add a listener that will be notified of any changes in scroll state or position.
     *
     * <p>Components that add a listener should take care to remove it when finished.
     * Other components that take ownership of a view may call {@link #clearOnScrollListeners()}
     * to remove all attached listeners.</p>
     *
     * @param listener listener to set
     */
    public void addOnScrollListener(@NonNull OnScrollListener listener) {
        if (mScrollListeners == null) {
            mScrollListeners = new ArrayList<>();
        }
        mScrollListeners.add(listener);
    }

    /**
     * Remove a listener that was notified of any changes in scroll state or position.
     *
     * @param listener listener to set or null to clear
     */
    public void removeOnScrollListener(@NonNull OnScrollListener listener) {
        if (mScrollListeners != null) {
            mScrollListeners.remove(listener);
        }
    }

    /**
     * Remove all secondary listener that were notified of any changes in scroll state or position.
     */
    public void clearOnScrollListeners() {
        if (mScrollListeners != null) {
            mScrollListeners.clear();
        }
    }

    /**
     * Convenience method to scroll to a certain position.
     * <p>
     * RecyclerView does not implement scrolling logic, rather forwards the call to
     * {@link RecyclerView.LayoutManager#scrollToPosition(int)}
     *
     * @param position Scroll to this adapter position
     * @see RecyclerView.LayoutManager#scrollToPosition(int)
     */
    public void scrollToPosition(int position) {
        if (mLayoutFrozen) {
            return;
        }
        stopScroll();
        if (mLayout == null) {
            Log.e(TAG, "Cannot scroll to position a LayoutManager set. "
                    + "Call setLayoutManager with a non-null argument.");
            return;
        }
        mLayout.scrollToPosition(position);
        awakenScrollBars();
    }

    void jumpToPositionForSmoothScroller(int position) {
        if (mLayout == null) {
            return;
        }
        mLayout.scrollToPosition(position);
        awakenScrollBars();
    }

    /**
     * Starts a smooth scroll to an adapter position.
     * <p>
     * To support smooth scrolling, you must override
     * {@link LayoutManager#smoothScrollToPosition(RecyclerView, State, int)} and create a
     * {@link SmoothScroller}.
     * <p>
     * {@link LayoutManager} is responsible for creating the actual scroll action. If you want to
     * provide a custom smooth scroll logic, override
     * {@link LayoutManager#smoothScrollToPosition(RecyclerView, State, int)} in your
     * LayoutManager.
     *
     * @param position The adapter position to scroll to
     * @see LayoutManager#smoothScrollToPosition(RecyclerView, State, int)
     */
    public void smoothScrollToPosition(int position) {
        if (mLayoutFrozen) {
            return;
        }
        if (mLayout == null) {
            Log.e(TAG, "Cannot smooth scroll without a LayoutManager set. "
                    + "Call setLayoutManager with a non-null argument.");
            return;
        }
        mLayout.smoothScrollToPosition(this, mState, position);
    }

    @Override
    public void scrollTo(int x, int y) {
        Log.w(TAG, "RecyclerView does not support scrolling to an absolute position. "
                + "Use scrollToPosition instead");
    }

    @Override
    public void scrollBy(int x, int y) {
        if (mLayout == null) {
            Log.e(TAG, "Cannot scroll without a LayoutManager set. "
                    + "Call setLayoutManager with a non-null argument.");
            return;
        }
        if (mLayoutFrozen) {
            return;
        }
        final boolean canScrollHorizontal = mLayout.canScrollHorizontally();
        final boolean canScrollVertical = mLayout.canScrollVertically();
        if (canScrollHorizontal || canScrollVertical) {
            scrollByInternal(canScrollHorizontal ? x : 0, canScrollVertical ? y : 0, null);
        }
    }

    /**
     * Scrolls the RV by 'dx' and 'dy' via calls to
     * {@link LayoutManager#scrollHorizontallyBy(int, Recycler, State)} and
     * {@link LayoutManager#scrollVerticallyBy(int, Recycler, State)}.
     * <p>
     * Also sets how much of the scroll was actually consumed in 'consumed' parameter (indexes 0 and
     * 1 for the x axis and y axis, respectively).
     * <p>
     * This method should only be called in the context of an existing scroll operation such that
     * any other necessary operations (such as a call to {@link #consumePendingUpdateOperations()})
     * is already handled.
     */
    void scrollStep(int dx, int dy, @Nullable int[] consumed) {
        startInterceptRequestLayout();
        onEnterLayoutOrScroll();

        TraceCompat.beginSection(TRACE_SCROLL_TAG);
        fillRemainingScrollValues(mState);

        int consumedX = 0;
        int consumedY = 0;
        if (dx != 0) {
            consumedX = mLayout.scrollHorizontallyBy(dx, mRecycler, mState);
        }
        if (dy != 0) {
            consumedY = mLayout.scrollVerticallyBy(dy, mRecycler, mState);
        }

        TraceCompat.endSection();
        repositionShadowingViews();

        onExitLayoutOrScroll();
        stopInterceptRequestLayout(false);

        if (consumed != null) {
            consumed[0] = consumedX;
            consumed[1] = consumedY;
        }
    }

    /**
     * Helper method reflect data changes to the state.
     * <p>
     * Adapter changes during a scroll may trigger a crash because scroll assumes no data change
     * but data actually changed.
     * <p>
     * This method consumes all deferred changes to avoid that case.
     */
    void consumePendingUpdateOperations() {
        if (!mFirstLayoutComplete || mDataSetHasChangedAfterLayout) {
            TraceCompat.beginSection(TRACE_ON_DATA_SET_CHANGE_LAYOUT_TAG);
            dispatchLayout();
            TraceCompat.endSection();
            return;
        }
        if (!mAdapterHelper.hasPendingUpdates()) {
            return;
        }

        // if it is only an item change (no add-remove-notifyDataSetChanged) we can check if any
        // of the visible items is affected and if not, just ignore the change.
        if (mAdapterHelper.hasAnyUpdateTypes(AdapterHelper.UpdateOp.UPDATE) && !mAdapterHelper
                .hasAnyUpdateTypes(AdapterHelper.UpdateOp.ADD | AdapterHelper.UpdateOp.REMOVE
                        | AdapterHelper.UpdateOp.MOVE)) {
            TraceCompat.beginSection(TRACE_HANDLE_ADAPTER_UPDATES_TAG);
            startInterceptRequestLayout();
            onEnterLayoutOrScroll();
            mAdapterHelper.preProcess();
            if (!mLayoutWasDefered) {
                if (hasUpdatedView()) {
                    dispatchLayout();
                } else {
                    // no need to layout, clean state
                    mAdapterHelper.consumePostponedUpdates();
                }
            }
            stopInterceptRequestLayout(true);
            onExitLayoutOrScroll();
            TraceCompat.endSection();
        } else if (mAdapterHelper.hasPendingUpdates()) {
            TraceCompat.beginSection(TRACE_ON_DATA_SET_CHANGE_LAYOUT_TAG);
            dispatchLayout();
            TraceCompat.endSection();
        }
    }

    /**
     * @return True if an existing view holder needs to be updated
     */
    private boolean hasUpdatedView() {
        final int childCount = mChildHelper.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final ViewHolder holder = getChildViewHolderInt(mChildHelper.getChildAt(i));
            if (holder == null || holder.shouldIgnore()) {
                continue;
            }
            if (holder.isUpdated()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Does not perform bounds checking. Used by internal methods that have already validated input.
     * <p>
     * It also reports any unused scroll request to the related EdgeEffect.
     *
     * @param x  The amount of horizontal scroll request
     * @param y  The amount of vertical scroll request
     * @param ev The originating MotionEvent, or null if not from a touch event.
     * @return Whether any scroll was consumed in either direction.
     */
    boolean scrollByInternal(int x, int y, MotionEvent ev) {
        int unconsumedX = 0, unconsumedY = 0;
        int consumedX = 0, consumedY = 0;

        consumePendingUpdateOperations();
        if (mAdapter != null) {
            scrollStep(x, y, mScrollStepConsumed);
            consumedX = mScrollStepConsumed[0];
            consumedY = mScrollStepConsumed[1];
            unconsumedX = x - consumedX;
            unconsumedY = y - consumedY;
        }
        if (!mItemDecorations.isEmpty()) {
            invalidate();
        }

        if (dispatchNestedScroll(consumedX, consumedY, unconsumedX, unconsumedY, mScrollOffset,
                TYPE_TOUCH)) {
            // Update the last touch co-ords, taking any scroll offset into account
            mLastTouchX -= mScrollOffset[0];
            mLastTouchY -= mScrollOffset[1];
            if (ev != null) {
                ev.offsetLocation(mScrollOffset[0], mScrollOffset[1]);
            }
            mNestedOffsets[0] += mScrollOffset[0];
            mNestedOffsets[1] += mScrollOffset[1];
        } else if (getOverScrollMode() != View.OVER_SCROLL_NEVER) {
            if (ev != null && !MotionEventCompat.isFromSource(ev, InputDevice.SOURCE_MOUSE)) {
                pullGlows(ev.getX(), unconsumedX, ev.getY(), unconsumedY);
            }
            considerReleasingGlowsOnScroll(x, y);
        }
        if (consumedX != 0 || consumedY != 0) {
            dispatchOnScrolled(consumedX, consumedY);
        }
        if (!awakenScrollBars()) {
            invalidate();
        }
        return consumedX != 0 || consumedY != 0;
    }

    /**
     * <p>Compute the horizontal offset of the horizontal scrollbar's thumb within the horizontal
     * range. This value is used to compute the length of the thumb within the scrollbar's track.
     * </p>
     *
     * <p>The range is expressed in arbitrary units that must be the same as the units used by
     * {@link #computeHorizontalScrollRange()} and {@link #computeHorizontalScrollExtent()}.</p>
     *
     * <p>Default implementation returns 0.</p>
     *
     * <p>If you want to support scroll bars, override
     * {@link RecyclerView.LayoutManager#computeHorizontalScrollOffset(RecyclerView.State)} in your
     * LayoutManager. </p>
     *
     * @return The horizontal offset of the scrollbar's thumb
     * @see RecyclerView.LayoutManager#computeHorizontalScrollOffset
     * (RecyclerView.State)
     */
    @Override
    public int computeHorizontalScrollOffset() {
        if (mLayout == null) {
            return 0;
        }
        return mLayout.canScrollHorizontally() ? mLayout.computeHorizontalScrollOffset(mState) : 0;
    }

    /**
     * <p>Compute the horizontal extent of the horizontal scrollbar's thumb within the
     * horizontal range. This value is used to compute the length of the thumb within the
     * scrollbar's track.</p>
     *
     * <p>The range is expressed in arbitrary units that must be the same as the units used by
     * {@link #computeHorizontalScrollRange()} and {@link #computeHorizontalScrollOffset()}.</p>
     *
     * <p>Default implementation returns 0.</p>
     *
     * <p>If you want to support scroll bars, override
     * {@link RecyclerView.LayoutManager#computeHorizontalScrollExtent(RecyclerView.State)} in your
     * LayoutManager.</p>
     *
     * @return The horizontal extent of the scrollbar's thumb
     * @see RecyclerView.LayoutManager#computeHorizontalScrollExtent(RecyclerView.State)
     */
    @Override
    public int computeHorizontalScrollExtent() {
        if (mLayout == null) {
            return 0;
        }
        return mLayout.canScrollHorizontally() ? mLayout.computeHorizontalScrollExtent(mState) : 0;
    }

    /**
     * <p>Compute the horizontal range that the horizontal scrollbar represents.</p>
     *
     * <p>The range is expressed in arbitrary units that must be the same as the units used by
     * {@link #computeHorizontalScrollExtent()} and {@link #computeHorizontalScrollOffset()}.</p>
     *
     * <p>Default implementation returns 0.</p>
     *
     * <p>If you want to support scroll bars, override
     * {@link RecyclerView.LayoutManager#computeHorizontalScrollRange(RecyclerView.State)} in your
     * LayoutManager.</p>
     *
     * @return The total horizontal range represented by the vertical scrollbar
     * @see RecyclerView.LayoutManager#computeHorizontalScrollRange(RecyclerView.State)
     */
    @Override
    public int computeHorizontalScrollRange() {
        if (mLayout == null) {
            return 0;
        }
        return mLayout.canScrollHorizontally() ? mLayout.computeHorizontalScrollRange(mState) : 0;
    }

    /**
     * <p>Compute the vertical offset of the vertical scrollbar's thumb within the vertical range.
     * This value is used to compute the length of the thumb within the scrollbar's track. </p>
     *
     * <p>The range is expressed in arbitrary units that must be the same as the units used by
     * {@link #computeVerticalScrollRange()} and {@link #computeVerticalScrollExtent()}.</p>
     *
     * <p>Default implementation returns 0.</p>
     *
     * <p>If you want to support scroll bars, override
     * {@link RecyclerView.LayoutManager#computeVerticalScrollOffset(RecyclerView.State)} in your
     * LayoutManager.</p>
     *
     * @return The vertical offset of the scrollbar's thumb
     * @see RecyclerView.LayoutManager#computeVerticalScrollOffset
     * (RecyclerView.State)
     */
    @Override
    public int computeVerticalScrollOffset() {
        if (mLayout == null) {
            return 0;
        }
        return mLayout.canScrollVertically() ? mLayout.computeVerticalScrollOffset(mState) : 0;
    }

    /**
     * <p>Compute the vertical extent of the vertical scrollbar's thumb within the vertical range.
     * This value is used to compute the length of the thumb within the scrollbar's track.</p>
     *
     * <p>The range is expressed in arbitrary units that must be the same as the units used by
     * {@link #computeVerticalScrollRange()} and {@link #computeVerticalScrollOffset()}.</p>
     *
     * <p>Default implementation returns 0.</p>
     *
     * <p>If you want to support scroll bars, override
     * {@link RecyclerView.LayoutManager#computeVerticalScrollExtent(RecyclerView.State)} in your
     * LayoutManager.</p>
     *
     * @return The vertical extent of the scrollbar's thumb
     * @see RecyclerView.LayoutManager#computeVerticalScrollExtent(RecyclerView.State)
     */
    @Override
    public int computeVerticalScrollExtent() {
        if (mLayout == null) {
            return 0;
        }
        return mLayout.canScrollVertically() ? mLayout.computeVerticalScrollExtent(mState) : 0;
    }

    /**
     * <p>Compute the vertical range that the vertical scrollbar represents.</p>
     *
     * <p>The range is expressed in arbitrary units that must be the same as the units used by
     * {@link #computeVerticalScrollExtent()} and {@link #computeVerticalScrollOffset()}.</p>
     *
     * <p>Default implementation returns 0.</p>
     *
     * <p>If you want to support scroll bars, override
     * {@link RecyclerView.LayoutManager#computeVerticalScrollRange(RecyclerView.State)} in your
     * LayoutManager.</p>
     *
     * @return The total vertical range represented by the vertical scrollbar
     * @see RecyclerView.LayoutManager#computeVerticalScrollRange(RecyclerView.State)
     */
    @Override
    public int computeVerticalScrollRange() {
        if (mLayout == null) {
            return 0;
        }
        return mLayout.canScrollVertically() ? mLayout.computeVerticalScrollRange(mState) : 0;
    }

    /**
     * This method should be called before any code that may trigger a child view to cause a call to
     * {@link RecyclerView#requestLayout()}.  Doing so enables {@link RecyclerView} to avoid
     * reacting to additional redundant calls to {@link #requestLayout()}.
     * <p>
     * A call to this method must always be accompanied by a call to
     * {@link #stopInterceptRequestLayout(boolean)} that follows the code that may trigger a
     * child View to cause a call to {@link RecyclerView#requestLayout()}.
     *
     * @see #stopInterceptRequestLayout(boolean)
     */
    void startInterceptRequestLayout() {
        mInterceptRequestLayoutDepth++;
        if (mInterceptRequestLayoutDepth == 1 && !mLayoutFrozen) {
            mLayoutWasDefered = false;
        }
    }

    /**
     * This method should be called after any code that may trigger a child view to cause a call to
     * {@link RecyclerView#requestLayout()}.
     * <p>
     * A call to this method must always be accompanied by a call to
     * {@link #startInterceptRequestLayout()} that precedes the code that may trigger a child
     * View to cause a call to {@link RecyclerView#requestLayout()}.
     *
     * @see #startInterceptRequestLayout()
     */
    void stopInterceptRequestLayout(boolean performLayoutChildren) {
        if (mInterceptRequestLayoutDepth < 1) {
            //noinspection PointlessBooleanExpression
            if (DEBUG) {
                throw new IllegalStateException("stopInterceptRequestLayout was called more "
                        + "times than startInterceptRequestLayout."
                        + exceptionLabel());
            }
            mInterceptRequestLayoutDepth = 1;
        }
        if (!performLayoutChildren && !mLayoutFrozen) {
            // Reset the layout request eaten counter.
            // This is necessary since eatRequest calls can be nested in which case the other
            // call will override the inner one.
            // for instance:
            // eat layout for process adapter updates
            //   eat layout for dispatchLayout
            //     a bunch of req layout calls arrive

            mLayoutWasDefered = false;
        }
        if (mInterceptRequestLayoutDepth == 1) {
            // when layout is frozen we should delay dispatchLayout()
            if (performLayoutChildren && mLayoutWasDefered && !mLayoutFrozen
                    && mLayout != null && mAdapter != null) {
                dispatchLayout();
            }
            if (!mLayoutFrozen) {
                mLayoutWasDefered = false;
            }
        }
        mInterceptRequestLayoutDepth--;
    }

    /**
     * Enable or disable layout and scroll.  After <code>setLayoutFrozen(true)</code> is called,
     * Layout requests will be postponed until <code>setLayoutFrozen(false)</code> is called;
     * child views are not updated when RecyclerView is frozen, {@link #smoothScrollBy(int, int)},
     * {@link #scrollBy(int, int)}, {@link #scrollToPosition(int)} and
     * {@link #smoothScrollToPosition(int)} are dropped; TouchEvents and GenericMotionEvents are
     * dropped; {@link LayoutManager#onFocusSearchFailed(View, int, Recycler, State)} will not be
     * called.
     *
     * <p>
     * <code>setLayoutFrozen(true)</code> does not prevent app from directly calling {@link
     * LayoutManager#scrollToPosition(int)}, {@link LayoutManager#smoothScrollToPosition(
     *RecyclerView, State, int)}.
     * <p>
     * {@link #setAdapter(Adapter)} and {@link #swapAdapter(Adapter, boolean)} will automatically
     * stop frozen.
     * <p>
     * Note: Running ItemAnimator is not stopped automatically,  it's caller's
     * responsibility to call ItemAnimator.end().
     *
     * @param frozen true to freeze layout and scroll, false to re-enable.
     */
    public void setLayoutFrozen(boolean frozen) {
        if (frozen != mLayoutFrozen) {
            assertNotInLayoutOrScroll("Do not setLayoutFrozen in layout or scroll");
            if (!frozen) {
                mLayoutFrozen = false;
                if (mLayoutWasDefered && mLayout != null && mAdapter != null) {
                    requestLayout();
                }
                mLayoutWasDefered = false;
            } else {
                final long now = SystemClock.uptimeMillis();
                MotionEvent cancelEvent = MotionEvent.obtain(now, now,
                        MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);
                onTouchEvent(cancelEvent);
                mLayoutFrozen = true;
                mIgnoreMotionEventTillDown = true;
                stopScroll();
            }
        }
    }

    /**
     * Returns true if layout and scroll are frozen.
     *
     * @return true if layout and scroll are frozen
     * @see #setLayoutFrozen(boolean)
     */
    public boolean isLayoutFrozen() {
        return mLayoutFrozen;
    }

    /**
     * Animate a scroll by the given amount of pixels along either axis.
     *
     * @param dx Pixels to scroll horizontally
     * @param dy Pixels to scroll vertically
     */
    public void smoothScrollBy(@Px int dx, @Px int dy) {
        smoothScrollBy(dx, dy, null);
    }

    /**
     * Animate a scroll by the given amount of pixels along either axis.
     *
     * @param dx           Pixels to scroll horizontally
     * @param dy           Pixels to scroll vertically
     * @param interpolator {@link Interpolator} to be used for scrolling. If it is
     *                     {@code null}, RecyclerView is going to use the default interpolator.
     */
    public void smoothScrollBy(@Px int dx, @Px int dy, @Nullable Interpolator interpolator) {
        if (mLayout == null) {
            Log.e(TAG, "Cannot smooth scroll without a LayoutManager set. "
                    + "Call setLayoutManager with a non-null argument.");
            return;
        }
        if (mLayoutFrozen) {
            return;
        }
        if (!mLayout.canScrollHorizontally()) {
            dx = 0;
        }
        if (!mLayout.canScrollVertically()) {
            dy = 0;
        }
        if (dx != 0 || dy != 0) {
            mViewFlinger.smoothScrollBy(dx, dy, interpolator);
        }
    }

    /**
     * Begin a standard fling with an initial velocity along each axis in pixels per second.
     * If the velocity given is below the system-defined minimum this method will return false
     * and no fling will occur.
     *
     * @param velocityX Initial horizontal velocity in pixels per second
     * @param velocityY Initial vertical velocity in pixels per second
     * @return true if the fling was started, false if the velocity was too low to fling or
     * LayoutManager does not support scrolling in the axis fling is issued.
     * @see LayoutManager#canScrollVertically()
     * @see LayoutManager#canScrollHorizontally()
     */
    public boolean fling(int velocityX, int velocityY) {
        if (mLayout == null) {
            Log.e(TAG, "Cannot fling without a LayoutManager set. "
                    + "Call setLayoutManager with a non-null argument.");
            return false;
        }
        if (mLayoutFrozen) {
            return false;
        }

        final boolean canScrollHorizontal = mLayout.canScrollHorizontally();
        final boolean canScrollVertical = mLayout.canScrollVertically();

        if (!canScrollHorizontal || Math.abs(velocityX) < mMinFlingVelocity) {
            velocityX = 0;
        }
        if (!canScrollVertical || Math.abs(velocityY) < mMinFlingVelocity) {
            velocityY = 0;
        }
        if (velocityX == 0 && velocityY == 0) {
            // If we don't have any velocity, return false
            return false;
        }

        if (!dispatchNestedPreFling(velocityX, velocityY)) {
            final boolean canScroll = canScrollHorizontal || canScrollVertical;
            dispatchNestedFling(velocityX, velocityY, canScroll);

            if (mOnFlingListener != null && mOnFlingListener.onFling(velocityX, velocityY)) {
                return true;
            }

            if (canScroll) {
                int nestedScrollAxis = ViewCompat.SCROLL_AXIS_NONE;
                if (canScrollHorizontal) {
                    nestedScrollAxis |= ViewCompat.SCROLL_AXIS_HORIZONTAL;
                }
                if (canScrollVertical) {
                    nestedScrollAxis |= ViewCompat.SCROLL_AXIS_VERTICAL;
                }
                startNestedScroll(nestedScrollAxis, TYPE_NON_TOUCH);

                velocityX = Math.max(-mMaxFlingVelocity, Math.min(velocityX, mMaxFlingVelocity));
                velocityY = Math.max(-mMaxFlingVelocity, Math.min(velocityY, mMaxFlingVelocity));
                mViewFlinger.fling(velocityX, velocityY);
                return true;
            }
        }
        return false;
    }

    /**
     * Stop any current scroll in progress, such as one started by
     * {@link #smoothScrollBy(int, int)}, {@link #fling(int, int)} or a touch-initiated fling.
     */
    public void stopScroll() {
        setScrollState(SCROLL_STATE_IDLE);
        stopScrollersInternal();
    }

    /**
     * Similar to {@link #stopScroll()} but does not set the state.
     */
    private void stopScrollersInternal() {
        mViewFlinger.stop();
        if (mLayout != null) {
            mLayout.stopSmoothScroller();
        }
    }

    /**
     * Returns the minimum velocity to start a fling.
     *
     * @return The minimum velocity to start a fling
     */
    public int getMinFlingVelocity() {
        return mMinFlingVelocity;
    }


    /**
     * Returns the maximum fling velocity used by this RecyclerView.
     *
     * @return The maximum fling velocity used by this RecyclerView.
     */
    public int getMaxFlingVelocity() {
        return mMaxFlingVelocity;
    }

    /**
     * Apply a pull to relevant overscroll glow effects
     */
    private void pullGlows(float x, float overscrollX, float y, float overscrollY) {
        boolean invalidate = false;
        if (overscrollX < 0) {
            ensureLeftGlow();
            EdgeEffectCompat.onPull(mLeftGlow, -overscrollX / getWidth(), 1f - y / getHeight());
            invalidate = true;
        } else if (overscrollX > 0) {
            ensureRightGlow();
            EdgeEffectCompat.onPull(mRightGlow, overscrollX / getWidth(), y / getHeight());
            invalidate = true;
        }

        if (overscrollY < 0) {
            ensureTopGlow();
            EdgeEffectCompat.onPull(mTopGlow, -overscrollY / getHeight(), x / getWidth());
            invalidate = true;
        } else if (overscrollY > 0) {
            ensureBottomGlow();
            EdgeEffectCompat.onPull(mBottomGlow, overscrollY / getHeight(), 1f - x / getWidth());
            invalidate = true;
        }

        if (invalidate || overscrollX != 0 || overscrollY != 0) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    private void releaseGlows() {
        boolean needsInvalidate = false;
        if (mLeftGlow != null) {
            mLeftGlow.onRelease();
            needsInvalidate = mLeftGlow.isFinished();
        }
        if (mTopGlow != null) {
            mTopGlow.onRelease();
            needsInvalidate |= mTopGlow.isFinished();
        }
        if (mRightGlow != null) {
            mRightGlow.onRelease();
            needsInvalidate |= mRightGlow.isFinished();
        }
        if (mBottomGlow != null) {
            mBottomGlow.onRelease();
            needsInvalidate |= mBottomGlow.isFinished();
        }
        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    void considerReleasingGlowsOnScroll(int dx, int dy) {
        boolean needsInvalidate = false;
        if (mLeftGlow != null && !mLeftGlow.isFinished() && dx > 0) {
            mLeftGlow.onRelease();
            needsInvalidate = mLeftGlow.isFinished();
        }
        if (mRightGlow != null && !mRightGlow.isFinished() && dx < 0) {
            mRightGlow.onRelease();
            needsInvalidate |= mRightGlow.isFinished();
        }
        if (mTopGlow != null && !mTopGlow.isFinished() && dy > 0) {
            mTopGlow.onRelease();
            needsInvalidate |= mTopGlow.isFinished();
        }
        if (mBottomGlow != null && !mBottomGlow.isFinished() && dy < 0) {
            mBottomGlow.onRelease();
            needsInvalidate |= mBottomGlow.isFinished();
        }
        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    void absorbGlows(int velocityX, int velocityY) {
        if (velocityX < 0) {
            ensureLeftGlow();
            mLeftGlow.onAbsorb(-velocityX);
        } else if (velocityX > 0) {
            ensureRightGlow();
            mRightGlow.onAbsorb(velocityX);
        }

        if (velocityY < 0) {
            ensureTopGlow();
            mTopGlow.onAbsorb(-velocityY);
        } else if (velocityY > 0) {
            ensureBottomGlow();
            mBottomGlow.onAbsorb(velocityY);
        }

        if (velocityX != 0 || velocityY != 0) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    void ensureLeftGlow() {
        if (mLeftGlow != null) {
            return;
        }
        mLeftGlow = mEdgeEffectFactory.createEdgeEffect(this, EdgeEffectFactory.DIRECTION_LEFT);
        if (mClipToPadding) {
            mLeftGlow.setSize(getMeasuredHeight() - getPaddingTop() - getPaddingBottom(),
                    getMeasuredWidth() - getPaddingLeft() - getPaddingRight());
        } else {
            mLeftGlow.setSize(getMeasuredHeight(), getMeasuredWidth());
        }
    }

    void ensureRightGlow() {
        if (mRightGlow != null) {
            return;
        }
        mRightGlow = mEdgeEffectFactory.createEdgeEffect(this, EdgeEffectFactory.DIRECTION_RIGHT);
        if (mClipToPadding) {
            mRightGlow.setSize(getMeasuredHeight() - getPaddingTop() - getPaddingBottom(),
                    getMeasuredWidth() - getPaddingLeft() - getPaddingRight());
        } else {
            mRightGlow.setSize(getMeasuredHeight(), getMeasuredWidth());
        }
    }

    void ensureTopGlow() {
        if (mTopGlow != null) {
            return;
        }
        mTopGlow = mEdgeEffectFactory.createEdgeEffect(this, EdgeEffectFactory.DIRECTION_TOP);
        if (mClipToPadding) {
            mTopGlow.setSize(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                    getMeasuredHeight() - getPaddingTop() - getPaddingBottom());
        } else {
            mTopGlow.setSize(getMeasuredWidth(), getMeasuredHeight());
        }

    }

    void ensureBottomGlow() {
        if (mBottomGlow != null) {
            return;
        }
        mBottomGlow = mEdgeEffectFactory.createEdgeEffect(this, EdgeEffectFactory.DIRECTION_BOTTOM);
        if (mClipToPadding) {
            mBottomGlow.setSize(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                    getMeasuredHeight() - getPaddingTop() - getPaddingBottom());
        } else {
            mBottomGlow.setSize(getMeasuredWidth(), getMeasuredHeight());
        }
    }

    void invalidateGlows() {
        mLeftGlow = mRightGlow = mTopGlow = mBottomGlow = null;
    }

    /**
     * Set a {@link EdgeEffectFactory} for this {@link RecyclerView}.
     * <p>
     * When a new {@link EdgeEffectFactory} is set, any existing over-scroll effects are cleared
     * and new effects are created as needed using
     * {@link EdgeEffectFactory#createEdgeEffect(RecyclerView, int)}
     *
     * @param edgeEffectFactory The {@link EdgeEffectFactory} instance.
     */
    public void setEdgeEffectFactory(@NonNull EdgeEffectFactory edgeEffectFactory) {
        Preconditions.checkNotNull(edgeEffectFactory);
        mEdgeEffectFactory = edgeEffectFactory;
        invalidateGlows();
    }

    /**
     * Retrieves the previously set {@link EdgeEffectFactory} or the default factory if nothing
     * was set.
     *
     * @return The previously set {@link EdgeEffectFactory}
     * @see #setEdgeEffectFactory(EdgeEffectFactory)
     */
    @NonNull
    public EdgeEffectFactory getEdgeEffectFactory() {
        return mEdgeEffectFactory;
    }

    /**
     * Since RecyclerView is a collection ViewGroup that includes virtual children (items that are
     * in the Adapter but not visible in the UI), it employs a more involved focus search strategy
     * that differs from other ViewGroups.
     * <p>
     * It first does a focus search within the RecyclerView. If this search finds a View that is in
     * the focus direction with respect to the currently focused View, RecyclerView returns that
     * child as the next focus target. When it cannot find such child, it calls
     * {@link LayoutManager#onFocusSearchFailed(View, int, Recycler, State)} to layout more Views
     * in the focus search direction. If LayoutManager adds a View that matches the
     * focus search criteria, it will be returned as the focus search result. Otherwise,
     * RecyclerView will call parent to handle the focus search like a regular ViewGroup.
     * <p>
     * When the direction is {@link View#FOCUS_FORWARD} or {@link View#FOCUS_BACKWARD}, a View that
     * is not in the focus direction is still valid focus target which may not be the desired
     * behavior if the Adapter has more children in the focus direction. To handle this case,
     * RecyclerView converts the focus direction to an absolute direction and makes a preliminary
     * focus search in that direction. If there are no Views to gain focus, it will call
     * {@link LayoutManager#onFocusSearchFailed(View, int, Recycler, State)} before running a
     * focus search with the original (relative) direction. This allows RecyclerView to provide
     * better candidates to the focus search while still allowing the view system to take focus from
     * the RecyclerView and give it to a more suitable child if such child exists.
     *
     * @param focused   The view that currently has focus
     * @param direction One of {@link View#FOCUS_UP}, {@link View#FOCUS_DOWN},
     *                  {@link View#FOCUS_LEFT}, {@link View#FOCUS_RIGHT}, {@link View#FOCUS_FORWARD},
     *                  {@link View#FOCUS_BACKWARD} or 0 for not applicable.
     * @return A new View that can be the next focus after the focused View
     */
    @Override
    public View focusSearch(View focused, int direction) {
        View result = mLayout.onInterceptFocusSearch(focused, direction);
        if (result != null) {
            return result;
        }
        final boolean canRunFocusFailure = mAdapter != null && mLayout != null
                && !isComputingLayout() && !mLayoutFrozen;

        final FocusFinder ff = FocusFinder.getInstance();
        if (canRunFocusFailure
                && (direction == View.FOCUS_FORWARD || direction == View.FOCUS_BACKWARD)) {
            // convert direction to absolute direction and see if we have a view there and if not
            // tell LayoutManager to add if it can.
            boolean needsFocusFailureLayout = false;
            if (mLayout.canScrollVertically()) {
                final int absDir =
                        direction == View.FOCUS_FORWARD ? View.FOCUS_DOWN : View.FOCUS_UP;
                final View found = ff.findNextFocus(this, focused, absDir);
                needsFocusFailureLayout = found == null;
                if (FORCE_ABS_FOCUS_SEARCH_DIRECTION) {
                    // Workaround for broken FOCUS_BACKWARD in API 15 and older devices.
                    direction = absDir;
                }
            }
            if (!needsFocusFailureLayout && mLayout.canScrollHorizontally()) {
                boolean rtl = mLayout.getLayoutDirection() == ViewCompat.LAYOUT_DIRECTION_RTL;
                final int absDir = (direction == View.FOCUS_FORWARD) ^ rtl
                        ? View.FOCUS_RIGHT : View.FOCUS_LEFT;
                final View found = ff.findNextFocus(this, focused, absDir);
                needsFocusFailureLayout = found == null;
                if (FORCE_ABS_FOCUS_SEARCH_DIRECTION) {
                    // Workaround for broken FOCUS_BACKWARD in API 15 and older devices.
                    direction = absDir;
                }
            }
            if (needsFocusFailureLayout) {
                consumePendingUpdateOperations();
                final View focusedItemView = findContainingItemView(focused);
                if (focusedItemView == null) {
                    // panic, focused view is not a child anymore, cannot call super.
                    return null;
                }
                startInterceptRequestLayout();
                mLayout.onFocusSearchFailed(focused, direction, mRecycler, mState);
                stopInterceptRequestLayout(false);
            }
            result = ff.findNextFocus(this, focused, direction);
        } else {
            result = ff.findNextFocus(this, focused, direction);
            if (result == null && canRunFocusFailure) {
                consumePendingUpdateOperations();
                final View focusedItemView = findContainingItemView(focused);
                if (focusedItemView == null) {
                    // panic, focused view is not a child anymore, cannot call super.
                    return null;
                }
                startInterceptRequestLayout();
                result = mLayout.onFocusSearchFailed(focused, direction, mRecycler, mState);
                stopInterceptRequestLayout(false);
            }
        }
        if (result != null && !result.hasFocusable()) {
            if (getFocusedChild() == null) {
                // Scrolling to this unfocusable view is not meaningful since there is no currently
                // focused view which RV needs to keep visible.
                return super.focusSearch(focused, direction);
            }
            // If the next view returned by onFocusSearchFailed in layout manager has no focusable
            // views, we still scroll to that view in order to make it visible on the screen.
            // If it's focusable, framework already calls RV's requestChildFocus which handles
            // bringing this newly focused item onto the screen.
            requestChildOnScreen(result, null);
            return focused;
        }
        return isPreferredNextFocus(focused, result, direction)
                ? result : super.focusSearch(focused, direction);
    }

    /**
     * Checks if the new focus candidate is a good enough candidate such that RecyclerView will
     * assign it as the next focus View instead of letting view hierarchy decide.
     * A good candidate means a View that is aligned in the focus direction wrt the focused View
     * and is not the RecyclerView itself.
     * When this method returns false, RecyclerView will let the parent make the decision so the
     * same View may still get the focus as a result of that search.
     */
    private boolean isPreferredNextFocus(View focused, View next, int direction) {
        if (next == null || next == this) {
            return false;
        }
        // panic, result view is not a child anymore, maybe workaround b/37864393
        if (findContainingItemView(next) == null) {
            return false;
        }
        if (focused == null) {
            return true;
        }
        // panic, focused view is not a child anymore, maybe workaround b/37864393
        if (findContainingItemView(focused) == null) {
            return true;
        }

        mTempRect.set(0, 0, focused.getWidth(), focused.getHeight());
        mTempRect2.set(0, 0, next.getWidth(), next.getHeight());
        offsetDescendantRectToMyCoords(focused, mTempRect);
        offsetDescendantRectToMyCoords(next, mTempRect2);
        final int rtl = mLayout.getLayoutDirection() == ViewCompat.LAYOUT_DIRECTION_RTL ? -1 : 1;
        int rightness = 0;
        if ((mTempRect.left < mTempRect2.left
                || mTempRect.right <= mTempRect2.left)
                && mTempRect.right < mTempRect2.right) {
            rightness = 1;
        } else if ((mTempRect.right > mTempRect2.right
                || mTempRect.left >= mTempRect2.right)
                && mTempRect.left > mTempRect2.left) {
            rightness = -1;
        }
        int downness = 0;
        if ((mTempRect.top < mTempRect2.top
                || mTempRect.bottom <= mTempRect2.top)
                && mTempRect.bottom < mTempRect2.bottom) {
            downness = 1;
        } else if ((mTempRect.bottom > mTempRect2.bottom
                || mTempRect.top >= mTempRect2.bottom)
                && mTempRect.top > mTempRect2.top) {
            downness = -1;
        }
        switch (direction) {
            case View.FOCUS_LEFT:
                return rightness < 0;
            case View.FOCUS_RIGHT:
                return rightness > 0;
            case View.FOCUS_UP:
                return downness < 0;
            case View.FOCUS_DOWN:
                return downness > 0;
            case View.FOCUS_FORWARD:
                return downness > 0 || (downness == 0 && rightness * rtl >= 0);
            case View.FOCUS_BACKWARD:
                return downness < 0 || (downness == 0 && rightness * rtl <= 0);
        }
        throw new IllegalArgumentException("Invalid direction: " + direction + exceptionLabel());
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        if (!mLayout.onRequestChildFocus(this, mState, child, focused) && focused != null) {
            requestChildOnScreen(child, focused);
        }
        super.requestChildFocus(child, focused);
    }

    /**
     * Requests that the given child of the RecyclerView be positioned onto the screen. This method
     * can be called for both unfocusable and focusable child views. For unfocusable child views,
     * the {@param focused} parameter passed is null, whereas for a focusable child, this parameter
     * indicates the actual descendant view within this child view that holds the focus.
     *
     * @param child   The child view of this RecyclerView that wants to come onto the screen.
     * @param focused The descendant view that actually has the focus if child is focusable, null
     *                otherwise.
     */
    private void requestChildOnScreen(@NonNull View child, @Nullable View focused) {
        View rectView = (focused != null) ? focused : child;
        mTempRect.set(0, 0, rectView.getWidth(), rectView.getHeight());

        // get item decor offsets w/o refreshing. If they are invalid, there will be another
        // layout pass to fix them, then it is LayoutManager's responsibility to keep focused
        // View in viewport.
        final ViewGroup.LayoutParams focusedLayoutParams = rectView.getLayoutParams();
        if (focusedLayoutParams instanceof LayoutParams) {
            // if focused child has item decors, use them. Otherwise, ignore.
            final LayoutParams lp = (LayoutParams) focusedLayoutParams;
            if (!lp.mInsetsDirty) {
                final Rect insets = lp.mDecorInsets;
                mTempRect.left -= insets.left;
                mTempRect.right += insets.right;
                mTempRect.top -= insets.top;
                mTempRect.bottom += insets.bottom;
            }
        }

        if (focused != null) {
            offsetDescendantRectToMyCoords(focused, mTempRect);
            offsetRectIntoDescendantCoords(child, mTempRect);
        }
        mLayout.requestChildRectangleOnScreen(this, child, mTempRect, !mFirstLayoutComplete,
                (focused == null));
    }

    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rect, boolean immediate) {
        return mLayout.requestChildRectangleOnScreen(this, child, rect, immediate);
    }

    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (mLayout == null || !mLayout.onAddFocusables(this, views, direction, focusableMode)) {
            super.addFocusables(views, direction, focusableMode);
        }
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        if (isComputingLayout()) {
            // if we are in the middle of a layout calculation, don't let any child take focus.
            // RV will handle it after layout calculation is finished.
            return false;
        }
        return super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mLayoutOrScrollCounter = 0;
        mIsAttached = true;
        mFirstLayoutComplete = mFirstLayoutComplete && !isLayoutRequested();
        if (mLayout != null) {
            mLayout.dispatchAttachedToWindow(this);
        }
        mPostedAnimatorRunner = false;

        if (ALLOW_THREAD_GAP_WORK) {
            // Register with gap worker
            mGapWorker = GapWorker.sGapWorker.get();
            if (mGapWorker == null) {
                mGapWorker = new GapWorker();

                // break 60 fps assumption if data from display appears valid
                // NOTE: we only do this query once, statically, because it's very expensive (> 1ms)
                Display display = ViewCompat.getDisplay(this);
                float refreshRate = 60.0f;
                if (!isInEditMode() && display != null) {
                    float displayRefreshRate = display.getRefreshRate();
                    if (displayRefreshRate >= 30.0f) {
                        refreshRate = displayRefreshRate;
                    }
                }
                mGapWorker.mFrameIntervalNs = (long) (1000000000 / refreshRate);
                GapWorker.sGapWorker.set(mGapWorker);
            }
            mGapWorker.add(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mItemAnimator != null) {
            mItemAnimator.endAnimations();
        }
        stopScroll();
        mIsAttached = false;
        if (mLayout != null) {
            mLayout.dispatchDetachedFromWindow(this, mRecycler);
        }
        mPendingAccessibilityImportanceChange.clear();
        removeCallbacks(mItemAnimatorRunner);
        mViewInfoStore.onDetach();

        if (ALLOW_THREAD_GAP_WORK && mGapWorker != null) {
            // Unregister with gap worker
            mGapWorker.remove(this);
            mGapWorker = null;
        }
    }

    /**
     * Returns true if RecyclerView is attached to window.
     */
    @Override
    public boolean isAttachedToWindow() {
        return mIsAttached;
    }

    /**
     * Checks if RecyclerView is in the middle of a layout or scroll and throws an
     * {@link IllegalStateException} if it <b>is not</b>.
     *
     * @param message The message for the exception. Can be null.
     * @see #assertNotInLayoutOrScroll(String)
     */
    void assertInLayoutOrScroll(String message) {
        if (!isComputingLayout()) {
            if (message == null) {
                throw new IllegalStateException("Cannot call this method unless RecyclerView is "
                        + "computing a layout or scrolling" + exceptionLabel());
            }
            throw new IllegalStateException(message + exceptionLabel());

        }
    }

    /**
     * Checks if RecyclerView is in the middle of a layout or scroll and throws an
     * {@link IllegalStateException} if it <b>is</b>.
     *
     * @param message The message for the exception. Can be null.
     * @see #assertInLayoutOrScroll(String)
     */
    void assertNotInLayoutOrScroll(String message) {
        if (isComputingLayout()) {
            if (message == null) {
                throw new IllegalStateException("Cannot call this method while RecyclerView is "
                        + "computing a layout or scrolling" + exceptionLabel());
            }
            throw new IllegalStateException(message);
        }
        if (mDispatchScrollCounter > 0) {
            Log.w(TAG, "Cannot call this method in a scroll callback. Scroll callbacks might"
                            + "be run during a measure & layout pass where you cannot change the"
                            + "RecyclerView data. Any method call that might change the structure"
                            + "of the RecyclerView or the adapter contents should be postponed to"
                            + "the next frame.",
                    new IllegalStateException("" + exceptionLabel()));
        }
    }

    /**
     * Add an {@link OnItemTouchListener} to intercept touch events before they are dispatched
     * to child views or this view's standard scrolling behavior.
     *
     * <p>Client code may use listeners to implement item manipulation behavior. Once a listener
     * returns true from
     * {@link OnItemTouchListener#onInterceptTouchEvent(RecyclerView, MotionEvent)} its
     * {@link OnItemTouchListener#onTouchEvent(RecyclerView, MotionEvent)} method will be called
     * for each incoming MotionEvent until the end of the gesture.</p>
     *
     * @param listener Listener to add
     * @see SimpleOnItemTouchListener
     */
    public void addOnItemTouchListener(@NonNull OnItemTouchListener listener) {
        mOnItemTouchListeners.add(listener);
    }

    /**
     * Remove an {@link OnItemTouchListener}. It will no longer be able to intercept touch events.
     *
     * @param listener Listener to remove
     */
    public void removeOnItemTouchListener(@NonNull OnItemTouchListener listener) {
        mOnItemTouchListeners.remove(listener);
        if (mActiveOnItemTouchListener == listener) {
            mActiveOnItemTouchListener = null;
        }
    }

    private boolean dispatchOnItemTouchIntercept(MotionEvent e) {
        final int action = e.getAction();
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_DOWN) {
            mActiveOnItemTouchListener = null;
        }

        final int listenerCount = mOnItemTouchListeners.size();
        for (int i = 0; i < listenerCount; i++) {
            final OnItemTouchListener listener = mOnItemTouchListeners.get(i);
            if (listener.onInterceptTouchEvent(this, e) && action != MotionEvent.ACTION_CANCEL) {
                mActiveOnItemTouchListener = listener;
                return true;
            }
        }
        return false;
    }

    private boolean dispatchOnItemTouch(MotionEvent e) {
        final int action = e.getAction();
        if (mActiveOnItemTouchListener != null) {
            if (action == MotionEvent.ACTION_DOWN) {
                // Stale state from a previous gesture, we're starting a new one. Clear it.
                mActiveOnItemTouchListener = null;
            } else {
                mActiveOnItemTouchListener.onTouchEvent(this, e);
                if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                    // Clean up for the next gesture.
                    mActiveOnItemTouchListener = null;
                }
                return true;
            }
        }

        // Listeners will have already received the ACTION_DOWN via dispatchOnItemTouchIntercept
        // as called from onInterceptTouchEvent; skip it.
        if (action != MotionEvent.ACTION_DOWN) {
            final int listenerCount = mOnItemTouchListeners.size();
            for (int i = 0; i < listenerCount; i++) {
                final OnItemTouchListener listener = mOnItemTouchListeners.get(i);
                if (listener.onInterceptTouchEvent(this, e)) {
                    mActiveOnItemTouchListener = listener;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        if (mLayoutFrozen) {
            // When layout is frozen,  RV does not intercept the motion event.
            // A child view e.g. a button may still get the click.
            return false;
        }
        if (dispatchOnItemTouchIntercept(e)) {
            cancelTouch();
            return true;
        }

        if (mLayout == null) {
            return false;
        }

        final boolean canScrollHorizontally = mLayout.canScrollHorizontally();
        final boolean canScrollVertically = mLayout.canScrollVertically();

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(e);

        final int action = e.getActionMasked();
        final int actionIndex = e.getActionIndex();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (mIgnoreMotionEventTillDown) {
                    mIgnoreMotionEventTillDown = false;
                }
                mScrollPointerId = e.getPointerId(0);
                mInitialTouchX = mLastTouchX = (int) (e.getX() + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (e.getY() + 0.5f);

                if (mScrollState == SCROLL_STATE_SETTLING) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                    setScrollState(SCROLL_STATE_DRAGGING);
                }

                // Clear the nested offsets
                mNestedOffsets[0] = mNestedOffsets[1] = 0;

                int nestedScrollAxis = ViewCompat.SCROLL_AXIS_NONE;
                if (canScrollHorizontally) {
                    nestedScrollAxis |= ViewCompat.SCROLL_AXIS_HORIZONTAL;
                }
                if (canScrollVertically) {
                    nestedScrollAxis |= ViewCompat.SCROLL_AXIS_VERTICAL;
                }
                startNestedScroll(nestedScrollAxis, TYPE_TOUCH);
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                mScrollPointerId = e.getPointerId(actionIndex);
                mInitialTouchX = mLastTouchX = (int) (e.getX(actionIndex) + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (e.getY(actionIndex) + 0.5f);
                break;

            case MotionEvent.ACTION_MOVE: {
                final int index = e.findPointerIndex(mScrollPointerId);
                if (index < 0) {
                    Log.e(TAG, "Error processing scroll; pointer index for id "
                            + mScrollPointerId + " not found. Did any MotionEvents get skipped?");
                    return false;
                }

                final int x = (int) (e.getX(index) + 0.5f);
                final int y = (int) (e.getY(index) + 0.5f);
                if (mScrollState != SCROLL_STATE_DRAGGING) {
                    final int dx = x - mInitialTouchX;
                    final int dy = y - mInitialTouchY;
                    boolean startScroll = false;
                    if (canScrollHorizontally && Math.abs(dx) > mTouchSlop) {
                        mLastTouchX = x;
                        startScroll = true;
                    }
                    if (canScrollVertically && Math.abs(dy) > mTouchSlop) {
                        mLastTouchY = y;
                        startScroll = true;
                    }
                    if (startScroll) {
                        setScrollState(SCROLL_STATE_DRAGGING);
                    }
                }
            }
            break;

            case MotionEvent.ACTION_POINTER_UP: {
                onPointerUp(e);
            }
            break;

            case MotionEvent.ACTION_UP: {
                mVelocityTracker.clear();
                stopNestedScroll(TYPE_TOUCH);
            }
            break;

            case MotionEvent.ACTION_CANCEL: {
                cancelTouch();
            }
        }
        return mScrollState == SCROLL_STATE_DRAGGING;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        final int listenerCount = mOnItemTouchListeners.size();
        for (int i = 0; i < listenerCount; i++) {
            final OnItemTouchListener listener = mOnItemTouchListeners.get(i);
            listener.onRequestDisallowInterceptTouchEvent(disallowIntercept);
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (mLayoutFrozen || mIgnoreMotionEventTillDown) {
            return false;
        }
        if (dispatchOnItemTouch(e)) {
            cancelTouch();
            return true;
        }

        if (mLayout == null) {
            return false;
        }

        final boolean canScrollHorizontally = mLayout.canScrollHorizontally();
        final boolean canScrollVertically = mLayout.canScrollVertically();

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        boolean eventAddedToVelocityTracker = false;

        final MotionEvent vtev = MotionEvent.obtain(e);
        final int action = e.getActionMasked();
        final int actionIndex = e.getActionIndex();

        if (action == MotionEvent.ACTION_DOWN) {
            mNestedOffsets[0] = mNestedOffsets[1] = 0;
        }
        vtev.offsetLocation(mNestedOffsets[0], mNestedOffsets[1]);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mScrollPointerId = e.getPointerId(0);
                mInitialTouchX = mLastTouchX = (int) (e.getX() + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (e.getY() + 0.5f);

                int nestedScrollAxis = ViewCompat.SCROLL_AXIS_NONE;
                if (canScrollHorizontally) {
                    nestedScrollAxis |= ViewCompat.SCROLL_AXIS_HORIZONTAL;
                }
                if (canScrollVertically) {
                    nestedScrollAxis |= ViewCompat.SCROLL_AXIS_VERTICAL;
                }
                startNestedScroll(nestedScrollAxis, TYPE_TOUCH);
            }
            break;

            case MotionEvent.ACTION_POINTER_DOWN: {
                mScrollPointerId = e.getPointerId(actionIndex);
                mInitialTouchX = mLastTouchX = (int) (e.getX(actionIndex) + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (e.getY(actionIndex) + 0.5f);
            }
            break;

            case MotionEvent.ACTION_MOVE: {
                final int index = e.findPointerIndex(mScrollPointerId);
                if (index < 0) {
                    Log.e(TAG, "Error processing scroll; pointer index for id "
                            + mScrollPointerId + " not found. Did any MotionEvents get skipped?");
                    return false;
                }

                final int x = (int) (e.getX(index) + 0.5f);
                final int y = (int) (e.getY(index) + 0.5f);
                int dx = mLastTouchX - x;
                int dy = mLastTouchY - y;

                if (dispatchNestedPreScroll(dx, dy, mScrollConsumed, mScrollOffset, TYPE_TOUCH)) {
                    dx -= mScrollConsumed[0];
                    dy -= mScrollConsumed[1];
                    vtev.offsetLocation(mScrollOffset[0], mScrollOffset[1]);
                    // Updated the nested offsets
                    mNestedOffsets[0] += mScrollOffset[0];
                    mNestedOffsets[1] += mScrollOffset[1];
                }

                if (mScrollState != SCROLL_STATE_DRAGGING) {
                    boolean startScroll = false;
                    if (canScrollHorizontally && Math.abs(dx) > mTouchSlop) {
                        if (dx > 0) {
                            dx -= mTouchSlop;
                        } else {
                            dx += mTouchSlop;
                        }
                        startScroll = true;
                    }
                    if (canScrollVertically && Math.abs(dy) > mTouchSlop) {
                        if (dy > 0) {
                            dy -= mTouchSlop;
                        } else {
                            dy += mTouchSlop;
                        }
                        startScroll = true;
                    }
                    if (startScroll) {
                        setScrollState(SCROLL_STATE_DRAGGING);
                    }
                }

                if (mScrollState == SCROLL_STATE_DRAGGING) {
                    mLastTouchX = x - mScrollOffset[0];
                    mLastTouchY = y - mScrollOffset[1];

                    if (scrollByInternal(
                            canScrollHorizontally ? dx : 0,
                            canScrollVertically ? dy : 0,
                            vtev)) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    if (mGapWorker != null && (dx != 0 || dy != 0)) {
                        mGapWorker.postFromTraversal(this, dx, dy);
                    }
                }
            }
            break;

            case MotionEvent.ACTION_POINTER_UP: {
                onPointerUp(e);
            }
            break;

            case MotionEvent.ACTION_UP: {
                mVelocityTracker.addMovement(vtev);
                eventAddedToVelocityTracker = true;
                mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
                final float xvel = canScrollHorizontally
                        ? -mVelocityTracker.getXVelocity(mScrollPointerId) : 0;
                final float yvel = canScrollVertically
                        ? -mVelocityTracker.getYVelocity(mScrollPointerId) : 0;
                if (!((xvel != 0 || yvel != 0) && fling((int) xvel, (int) yvel))) {
                    setScrollState(SCROLL_STATE_IDLE);
                }
                resetTouch();
            }
            break;

            case MotionEvent.ACTION_CANCEL: {
                cancelTouch();
            }
            break;
        }

        if (!eventAddedToVelocityTracker) {
            mVelocityTracker.addMovement(vtev);
        }
        vtev.recycle();

        return true;
    }

    private void resetTouch() {
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
        }
        stopNestedScroll(TYPE_TOUCH);
        releaseGlows();
    }

    private void cancelTouch() {
        resetTouch();
        setScrollState(SCROLL_STATE_IDLE);
    }

    private void onPointerUp(MotionEvent e) {
        final int actionIndex = e.getActionIndex();
        if (e.getPointerId(actionIndex) == mScrollPointerId) {
            // Pick a new pointer to pick up the slack.
            final int newIndex = actionIndex == 0 ? 1 : 0;
            mScrollPointerId = e.getPointerId(newIndex);
            mInitialTouchX = mLastTouchX = (int) (e.getX(newIndex) + 0.5f);
            mInitialTouchY = mLastTouchY = (int) (e.getY(newIndex) + 0.5f);
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (mLayout == null) {
            return false;
        }
        if (mLayoutFrozen) {
            return false;
        }
        if (event.getAction() == MotionEventCompat.ACTION_SCROLL) {
            final float vScroll, hScroll;
            if ((event.getSource() & InputDeviceCompat.SOURCE_CLASS_POINTER) != 0) {
                if (mLayout.canScrollVertically()) {
                    // Inverse the sign of the vertical scroll to align the scroll orientation
                    // with AbsListView.
                    vScroll = -event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                } else {
                    vScroll = 0f;
                }
                if (mLayout.canScrollHorizontally()) {
                    hScroll = event.getAxisValue(MotionEvent.AXIS_HSCROLL);
                } else {
                    hScroll = 0f;
                }
            } else if ((event.getSource() & InputDeviceCompat.SOURCE_ROTARY_ENCODER) != 0) {
                final float axisScroll = event.getAxisValue(MotionEventCompat.AXIS_SCROLL);
                if (mLayout.canScrollVertically()) {
                    // Invert the sign of the vertical scroll to align the scroll orientation
                    // with AbsListView.
                    vScroll = -axisScroll;
                    hScroll = 0f;
                } else if (mLayout.canScrollHorizontally()) {
                    vScroll = 0f;
                    hScroll = axisScroll;
                } else {
                    vScroll = 0f;
                    hScroll = 0f;
                }
            } else {
                vScroll = 0f;
                hScroll = 0f;
            }

            if (vScroll != 0 || hScroll != 0) {
                scrollByInternal((int) (hScroll * mScaledHorizontalScrollFactor),
                        (int) (vScroll * mScaledVerticalScrollFactor), event);
            }
        }
        return false;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        if (mLayout == null) {
            defaultOnMeasure(widthSpec, heightSpec);
            return;
        }
        if (mLayout.isAutoMeasureEnabled()) {
            final int widthMode = MeasureSpec.getMode(widthSpec);
            final int heightMode = MeasureSpec.getMode(heightSpec);

            /**
             * This specific call should be considered deprecated and replaced with
             * {@link #defaultOnMeasure(int, int)}. It can't actually be replaced as it could
             * break existing third party code but all documentation directs developers to not
             * override {@link LayoutManager#onMeasure(int, int)} when
             * {@link LayoutManager#isAutoMeasureEnabled()} returns true.
             */
            mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec);

            final boolean measureSpecModeIsExactly =
                    widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY;
            if (measureSpecModeIsExactly || mAdapter == null) {
                return;
            }

            if (mState.mLayoutStep == State.STEP_START) {
                dispatchLayoutStep1();
            }
            // set dimensions in 2nd step. Pre-layout should happen with old dimensions for
            // consistency
            mLayout.setMeasureSpecs(widthSpec, heightSpec);
            mState.mIsMeasuring = true;
            dispatchLayoutStep2();

            // now we can get the width and height from the children.
            mLayout.setMeasuredDimensionFromChildren(widthSpec, heightSpec);

            // if RecyclerView has non-exact width and height and if there is at least one child
            // which also has non-exact width & height, we have to re-measure.
            if (mLayout.shouldMeasureTwice()) {
                mLayout.setMeasureSpecs(
                        MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
                mState.mIsMeasuring = true;
                dispatchLayoutStep2();
                // now we can get the width and height from the children.
                mLayout.setMeasuredDimensionFromChildren(widthSpec, heightSpec);
            }
        } else {
            if (mHasFixedSize) {
                mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec);
                return;
            }
            // custom onMeasure
            if (mAdapterUpdateDuringMeasure) {
                startInterceptRequestLayout();
                onEnterLayoutOrScroll();
                processAdapterUpdatesAndSetAnimationFlags();
                onExitLayoutOrScroll();

                if (mState.mRunPredictiveAnimations) {
                    mState.mInPreLayout = true;
                } else {
                    // consume remaining updates to provide a consistent state with the layout pass.
                    mAdapterHelper.consumeUpdatesInOnePass();
                    mState.mInPreLayout = false;
                }
                mAdapterUpdateDuringMeasure = false;
                stopInterceptRequestLayout(false);
            } else if (mState.mRunPredictiveAnimations) {
                // If mAdapterUpdateDuringMeasure is false and mRunPredictiveAnimations is true:
                // this means there is already an onMeasure() call performed to handle the pending
                // adapter change, two onMeasure() calls can happen if RV is a child of LinearLayout
                // with layout_width=MATCH_PARENT. RV cannot call LM.onMeasure() second time
                // because getViewForPosition() will crash when LM uses a child to measure.
                setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight());
                return;
            }

            if (mAdapter != null) {
                mState.mItemCount = mAdapter.getItemCount();
            } else {
                mState.mItemCount = 0;
            }
            startInterceptRequestLayout();
            mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec);
            stopInterceptRequestLayout(false);
            mState.mInPreLayout = false; // clear
        }
    }

    /**
     * An implementation of {@link View#onMeasure(int, int)} to fall back to in various scenarios
     * where this RecyclerView is otherwise lacking better information.
     */
    void defaultOnMeasure(int widthSpec, int heightSpec) {
        // calling LayoutManager here is not pretty but that API is already public and it is better
        // than creating another method since this is internal.
        final int width = LayoutManager.chooseSize(widthSpec,
                getPaddingLeft() + getPaddingRight(),
                ViewCompat.getMinimumWidth(this));
        final int height = LayoutManager.chooseSize(heightSpec,
                getPaddingTop() + getPaddingBottom(),
                ViewCompat.getMinimumHeight(this));

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw || h != oldh) {
            invalidateGlows();
            // layout's w/h are updated during measure/layout steps.
        }
    }

    /**
     * Sets the {@link ItemAnimator} that will handle animations involving changes
     * to the items in this RecyclerView. By default, RecyclerView instantiates and
     * uses an instance of {@link DefaultItemAnimator}. Whether item animations are
     * enabled for the RecyclerView depends on the ItemAnimator and whether
     * the LayoutManager {@link LayoutManager#supportsPredictiveItemAnimations()
     * supports item animations}.
     *
     * @param animator The ItemAnimator being set. If null, no animations will occur
     *                 when changes occur to the items in this RecyclerView.
     */
    public void setItemAnimator(@Nullable ItemAnimator animator) {
        if (mItemAnimator != null) {
            mItemAnimator.endAnimations();
            mItemAnimator.setListener(null);
        }
        mItemAnimator = animator;
        if (mItemAnimator != null) {
            mItemAnimator.setListener(mItemAnimatorListener);
        }
    }

    void onEnterLayoutOrScroll() {
        mLayoutOrScrollCounter++;
    }

    void onExitLayoutOrScroll() {
        onExitLayoutOrScroll(true);
    }

    void onExitLayoutOrScroll(boolean enableChangeEvents) {
        mLayoutOrScrollCounter--;
        if (mLayoutOrScrollCounter < 1) {
            if (DEBUG && mLayoutOrScrollCounter < 0) {
                throw new IllegalStateException("layout or scroll counter cannot go below zero."
                        + "Some calls are not matching" + exceptionLabel());
            }
            mLayoutOrScrollCounter = 0;
            if (enableChangeEvents) {
                dispatchContentChangedIfNecessary();
                dispatchPendingImportantForAccessibilityChanges();
            }
        }
    }

    boolean isAccessibilityEnabled() {
        return mAccessibilityManager != null && mAccessibilityManager.isEnabled();
    }

    private void dispatchContentChangedIfNecessary() {
        final int flags = mEatenAccessibilityChangeFlags;
        mEatenAccessibilityChangeFlags = 0;
        if (flags != 0 && isAccessibilityEnabled()) {
            final AccessibilityEvent event = AccessibilityEvent.obtain();
            event.setEventType(AccessibilityEventCompat.TYPE_WINDOW_CONTENT_CHANGED);
            AccessibilityEventCompat.setContentChangeTypes(event, flags);
            sendAccessibilityEventUnchecked(event);
        }
    }

    /**
     * Returns whether RecyclerView is currently computing a layout.
     * <p>
     * If this method returns true, it means that RecyclerView is in a lockdown state and any
     * attempt to update adapter contents will result in an exception because adapter contents
     * cannot be changed while RecyclerView is trying to compute the layout.
     * <p>
     * It is very unlikely that your code will be running during this state as it is
     * called by the framework when a layout traversal happens or RecyclerView starts to scroll
     * in response to system events (touch, accessibility etc).
     * <p>
     * This case may happen if you have some custom logic to change adapter contents in
     * response to a View callback (e.g. focus change callback) which might be triggered during a
     * layout calculation. In these cases, you should just postpone the change using a Handler or a
     * similar mechanism.
     *
     * @return <code>true</code> if RecyclerView is currently computing a layout, <code>false</code>
     * otherwise
     */
    public boolean isComputingLayout() {
        return mLayoutOrScrollCounter > 0;
    }

    /**
     * Returns true if an accessibility event should not be dispatched now. This happens when an
     * accessibility request arrives while RecyclerView does not have a stable state which is very
     * hard to handle for a LayoutManager. Instead, this method records necessary information about
     * the event and dispatches a window change event after the critical section is finished.
     *
     * @return True if the accessibility event should be postponed.
     */
    boolean shouldDeferAccessibilityEvent(AccessibilityEvent event) {
        if (isComputingLayout()) {
            int type = 0;
            if (event != null) {
                type = AccessibilityEventCompat.getContentChangeTypes(event);
            }
            if (type == 0) {
                type = AccessibilityEventCompat.CONTENT_CHANGE_TYPE_UNDEFINED;
            }
            mEatenAccessibilityChangeFlags |= type;
            return true;
        }
        return false;
    }

    @Override
    public void sendAccessibilityEventUnchecked(AccessibilityEvent event) {
        if (shouldDeferAccessibilityEvent(event)) {
            return;
        }
        super.sendAccessibilityEventUnchecked(event);
    }

    /**
     * Gets the current ItemAnimator for this RecyclerView. A null return value
     * indicates that there is no animator and that item changes will happen without
     * any animations. By default, RecyclerView instantiates and
     * uses an instance of {@link DefaultItemAnimator}.
     *
     * @return ItemAnimator The current ItemAnimator. If null, no animations will occur
     * when changes occur to the items in this RecyclerView.
     */
    @Nullable
    public ItemAnimator getItemAnimator() {
        return mItemAnimator;
    }

    /**
     * Post a runnable to the next frame to run pending item animations. Only the first such
     * request will be posted, governed by the mPostedAnimatorRunner flag.
     */
    void postAnimationRunner() {
        if (!mPostedAnimatorRunner && mIsAttached) {
            ViewCompat.postOnAnimation(this, mItemAnimatorRunner);
            mPostedAnimatorRunner = true;
        }
    }

    private boolean predictiveItemAnimationsEnabled() {
        return (mItemAnimator != null && mLayout.supportsPredictiveItemAnimations());
    }

    /**
     * Consumes adapter updates and calculates which type of animations we want to run.
     * Called in onMeasure and dispatchLayout.
     * <p>
     * This method may process only the pre-layout state of updates or all of them.
     */
    private void processAdapterUpdatesAndSetAnimationFlags() {
        if (mDataSetHasChangedAfterLayout) {
            // Processing these items have no value since data set changed unexpectedly.
            // Instead, we just reset it.
            mAdapterHelper.reset();
            if (mDispatchItemsChangedEvent) {
                mLayout.onItemsChanged(this);
            }
        }
        // simple animations are a subset of advanced animations (which will cause a
        // pre-layout step)
        // If layout supports predictive animations, pre-process to decide if we want to run them
        if (predictiveItemAnimationsEnabled()) {
            mAdapterHelper.preProcess();
        } else {
            mAdapterHelper.consumeUpdatesInOnePass();
        }
        boolean animationTypeSupported = mItemsAddedOrRemoved || mItemsChanged;
        mState.mRunSimpleAnimations = mFirstLayoutComplete
                && mItemAnimator != null
                && (mDataSetHasChangedAfterLayout
                || animationTypeSupported
                || mLayout.mRequestedSimpleAnimations)
                && (!mDataSetHasChangedAfterLayout
                || mAdapter.hasStableIds());
        mState.mRunPredictiveAnimations = mState.mRunSimpleAnimations
                && animationTypeSupported
                && !mDataSetHasChangedAfterLayout
                && predictiveItemAnimationsEnabled();
    }

    /**
     * Wrapper around layoutChildren() that handles animating changes caused by layout.
     * Animations work on the assumption that there are five different kinds of items
     * in play:
     * PERSISTENT: items are visible before and after layout
     * REMOVED: items were visible before layout and were removed by the app
     * ADDED: items did not exist before layout and were added by the app
     * DISAPPEARING: items exist in the data set before/after, but changed from
     * visible to non-visible in the process of layout (they were moved off
     * screen as a side-effect of other changes)
     * APPEARING: items exist in the data set before/after, but changed from
     * non-visible to visible in the process of layout (they were moved on
     * screen as a side-effect of other changes)
     * The overall approach figures out what items exist before/after layout and
     * infers one of the five above states for each of the items. Then the animations
     * are set up accordingly:
     * PERSISTENT views are animated via
     * {@link ItemAnimator#animatePersistence(ViewHolder, ItemHolderInfo, ItemHolderInfo)}
     * DISAPPEARING views are animated via
     * {@link ItemAnimator#animateDisappearance(ViewHolder, ItemHolderInfo, ItemHolderInfo)}
     * APPEARING views are animated via
     * {@link ItemAnimator#animateAppearance(ViewHolder, ItemHolderInfo, ItemHolderInfo)}
     * and changed views are animated via
     * {@link ItemAnimator#animateChange(ViewHolder, ViewHolder, ItemHolderInfo, ItemHolderInfo)}.
     */
    void dispatchLayout() {
        if (mAdapter == null) {
            Log.e(TAG, "No adapter attached; skipping layout");
            // leave the state in START
            return;
        }
        if (mLayout == null) {
            Log.e(TAG, "No layout manager attached; skipping layout");
            // leave the state in START
            return;
        }
        mState.mIsMeasuring = false;
        if (mState.mLayoutStep == State.STEP_START) {
            dispatchLayoutStep1();
            mLayout.setExactMeasureSpecsFrom(this);
            dispatchLayoutStep2();
        } else if (mAdapterHelper.hasUpdates() || mLayout.getWidth() != getWidth()
                || mLayout.getHeight() != getHeight()) {
            // First 2 steps are done in onMeasure but looks like we have to run again due to
            // changed size.
            mLayout.setExactMeasureSpecsFrom(this);
            dispatchLayoutStep2();
        } else {
            // always make sure we sync them (to ensure mode is exact)
            mLayout.setExactMeasureSpecsFrom(this);
        }
        dispatchLayoutStep3();
    }

    private void saveFocusInfo() {
        View child = null;
        if (mPreserveFocusAfterLayout && hasFocus() && mAdapter != null) {
            child = getFocusedChild();
        }

        final ViewHolder focusedVh = child == null ? null : findContainingViewHolder(child);
        if (focusedVh == null) {
            resetFocusInfo();
        } else {
            mState.mFocusedItemId = mAdapter.hasStableIds() ? focusedVh.getItemId() : NO_ID;
            // mFocusedItemPosition should hold the current adapter position of the previously
            // focused item. If the item is removed, we store the previous adapter position of the
            // removed item.
            mState.mFocusedItemPosition = mDataSetHasChangedAfterLayout ? NO_POSITION
                    : (focusedVh.isRemoved() ? focusedVh.mOldPosition
                    : focusedVh.getAdapterPosition());
            mState.mFocusedSubChildId = getDeepestFocusedViewWithId(focusedVh.itemView);
        }
    }

    private void resetFocusInfo() {
        mState.mFocusedItemId = NO_ID;
        mState.mFocusedItemPosition = NO_POSITION;
        mState.mFocusedSubChildId = View.NO_ID;
    }

    /**
     * Finds the best view candidate to request focus on using mFocusedItemPosition index of the
     * previously focused item. It first traverses the adapter forward to find a focusable candidate
     * and if no such candidate is found, it reverses the focus search direction for the items
     * before the mFocusedItemPosition'th index;
     *
     * @return The best candidate to request focus on, or null if no such candidate exists. Null
     * indicates all the existing adapter items are unfocusable.
     */
    @Nullable
    private View findNextViewToFocus() {
        int startFocusSearchIndex = mState.mFocusedItemPosition != -1 ? mState.mFocusedItemPosition
                : 0;
        ViewHolder nextFocus;
        final int itemCount = mState.getItemCount();
        for (int i = startFocusSearchIndex; i < itemCount; i++) {
            nextFocus = findViewHolderForAdapterPosition(i);
            if (nextFocus == null) {
                break;
            }
            if (nextFocus.itemView.hasFocusable()) {
                return nextFocus.itemView;
            }
        }
        final int limit = Math.min(itemCount, startFocusSearchIndex);
        for (int i = limit - 1; i >= 0; i--) {
            nextFocus = findViewHolderForAdapterPosition(i);
            if (nextFocus == null) {
                return null;
            }
            if (nextFocus.itemView.hasFocusable()) {
                return nextFocus.itemView;
            }
        }
        return null;
    }

    private void recoverFocusFromState() {
        if (!mPreserveFocusAfterLayout || mAdapter == null || !hasFocus()
                || getDescendantFocusability() == FOCUS_BLOCK_DESCENDANTS
                || (getDescendantFocusability() == FOCUS_BEFORE_DESCENDANTS && isFocused())) {
            // No-op if either of these cases happens:
            // 1. RV has no focus, or 2. RV blocks focus to its children, or 3. RV takes focus
            // before its children and is focused (i.e. it already stole the focus away from its
            // descendants).
            return;
        }
        // only recover focus if RV itself has the focus or the focused view is hidden
        if (!isFocused()) {
            final View focusedChild = getFocusedChild();
            if (IGNORE_DETACHED_FOCUSED_CHILD
                    && (focusedChild.getParent() == null || !focusedChild.hasFocus())) {
                // Special handling of API 15-. A focused child can be invalid because mFocus is not
                // cleared when the child is detached (mParent = null),
                // This happens because clearFocus on API 15- does not invalidate mFocus of its
                // parent when this child is detached.
                // For API 16+, this is not an issue because requestFocus takes care of clearing the
                // prior detached focused child. For API 15- the problem happens in 2 cases because
                // clearChild does not call clearChildFocus on RV: 1. setFocusable(false) is called
                // for the current focused item which calls clearChild or 2. when the prior focused
                // child is removed, removeDetachedView called in layout step 3 which calls
                // clearChild. We should ignore this invalid focused child in all our calculations
                // for the next view to receive focus, and apply the focus recovery logic instead.
                if (mChildHelper.getChildCount() == 0) {
                    // No children left. Request focus on the RV itself since one of its children
                    // was holding focus previously.
                    requestFocus();
                    return;
                }
            } else if (!mChildHelper.isHidden(focusedChild)) {
                // If the currently focused child is hidden, apply the focus recovery logic.
                // Otherwise return, i.e. the currently (unhidden) focused child is good enough :/.
                return;
            }
        }
        ViewHolder focusTarget = null;
        // RV first attempts to locate the previously focused item to request focus on using
        // mFocusedItemId. If such an item no longer exists, it then makes a best-effort attempt to
        // find the next best candidate to request focus on based on mFocusedItemPosition.
        if (mState.mFocusedItemId != NO_ID && mAdapter.hasStableIds()) {
            focusTarget = findViewHolderForItemId(mState.mFocusedItemId);
        }
        View viewToFocus = null;
        if (focusTarget == null || mChildHelper.isHidden(focusTarget.itemView)
                || !focusTarget.itemView.hasFocusable()) {
            if (mChildHelper.getChildCount() > 0) {
                // At this point, RV has focus and either of these conditions are true:
                // 1. There's no previously focused item either because RV received focused before
                // layout, or the previously focused item was removed, or RV doesn't have stable IDs
                // 2. Previous focus child is hidden, or 3. Previous focused child is no longer
                // focusable. In either of these cases, we make sure that RV still passes down the
                // focus to one of its focusable children using a best-effort algorithm.
                viewToFocus = findNextViewToFocus();
            }
        } else {
            // looks like the focused item has been replaced with another view that represents the
            // same item in the adapter. Request focus on that.
            viewToFocus = focusTarget.itemView;
        }

        if (viewToFocus != null) {
            if (mState.mFocusedSubChildId != NO_ID) {
                View child = viewToFocus.findViewById(mState.mFocusedSubChildId);
                if (child != null && child.isFocusable()) {
                    viewToFocus = child;
                }
            }
            viewToFocus.requestFocus();
        }
    }

    private int getDeepestFocusedViewWithId(View view) {
        int lastKnownId = view.getId();
        while (!view.isFocused() && view instanceof ViewGroup && view.hasFocus()) {
            view = ((ViewGroup) view).getFocusedChild();
            final int id = view.getId();
            if (id != View.NO_ID) {
                lastKnownId = view.getId();
            }
        }
        return lastKnownId;
    }

    final void fillRemainingScrollValues(State state) {
        if (getScrollState() == SCROLL_STATE_SETTLING) {
            final OverScroller scroller = mViewFlinger.mScroller;
            state.mRemainingScrollHorizontal = scroller.getFinalX() - scroller.getCurrX();
            state.mRemainingScrollVertical = scroller.getFinalY() - scroller.getCurrY();
        } else {
            state.mRemainingScrollHorizontal = 0;
            state.mRemainingScrollVertical = 0;
        }
    }

    /**
     * The first step of a layout where we;
     * - process adapter updates
     * - decide which animation should run
     * - save information about current views
     * - If necessary, run predictive layout and save its information
     */
    private void dispatchLayoutStep1() {
        mState.assertLayoutStep(State.STEP_START);
        fillRemainingScrollValues(mState);
        mState.mIsMeasuring = false;
        startInterceptRequestLayout();
        mViewInfoStore.clear();
        onEnterLayoutOrScroll();
        processAdapterUpdatesAndSetAnimationFlags();
        saveFocusInfo();
        mState.mTrackOldChangeHolders = mState.mRunSimpleAnimations && mItemsChanged;
        mItemsAddedOrRemoved = mItemsChanged = false;
        mState.mInPreLayout = mState.mRunPredictiveAnimations;
        mState.mItemCount = mAdapter.getItemCount();
        findMinMaxChildLayoutPositions(mMinMaxLayoutPositions);

        if (mState.mRunSimpleAnimations) {
            // Step 0: Find out where all non-removed items are, pre-layout
            int count = mChildHelper.getChildCount();
            for (int i = 0; i < count; ++i) {
                final ViewHolder holder = getChildViewHolderInt(mChildHelper.getChildAt(i));
                if (holder.shouldIgnore() || (holder.isInvalid() && !mAdapter.hasStableIds())) {
                    continue;
                }
                final ItemHolderInfo animationInfo = mItemAnimator
                        .recordPreLayoutInformation(mState, holder,
                                ItemAnimator.buildAdapterChangeFlagsForAnimations(holder),
                                holder.getUnmodifiedPayloads());
                mViewInfoStore.addToPreLayout(holder, animationInfo);
                if (mState.mTrackOldChangeHolders && holder.isUpdated() && !holder.isRemoved()
                        && !holder.shouldIgnore() && !holder.isInvalid()) {
                    long key = getChangedHolderKey(holder);
                    // This is NOT the only place where a ViewHolder is added to old change holders
                    // list. There is another case where:
                    //    * A VH is currently hidden but not deleted
                    //    * The hidden item is changed in the adapter
                    //    * Layout manager decides to layout the item in the pre-Layout pass (step1)
                    // When this case is detected, RV will un-hide that view and add to the old
                    // change holders list.
                    mViewInfoStore.addToOldChangeHolders(key, holder);
                }
            }
        }
        if (mState.mRunPredictiveAnimations) {
            // Step 1: run prelayout: This will use the old positions of items. The layout manager
            // is expected to layout everything, even removed items (though not to add removed
            // items back to the container). This gives the pre-layout position of APPEARING views
            // which come into existence as part of the real layout.

            // Save old positions so that LayoutManager can run its mapping logic.
            saveOldPositions();
            final boolean didStructureChange = mState.mStructureChanged;
            mState.mStructureChanged = false;
            // temporarily disable flag because we are asking for previous layout
            mLayout.onLayoutChildren(mRecycler, mState);
            mState.mStructureChanged = didStructureChange;

            for (int i = 0; i < mChildHelper.getChildCount(); ++i) {
                final View child = mChildHelper.getChildAt(i);
                final ViewHolder viewHolder = getChildViewHolderInt(child);
                if (viewHolder.shouldIgnore()) {
                    continue;
                }
                if (!mViewInfoStore.isInPreLayout(viewHolder)) {
                    int flags = ItemAnimator.buildAdapterChangeFlagsForAnimations(viewHolder);
                    boolean wasHidden = viewHolder
                            .hasAnyOfTheFlags(ViewHolder.FLAG_BOUNCED_FROM_HIDDEN_LIST);
                    if (!wasHidden) {
                        flags |= ItemAnimator.FLAG_APPEARED_IN_PRE_LAYOUT;
                    }
                    final ItemHolderInfo animationInfo = mItemAnimator.recordPreLayoutInformation(
                            mState, viewHolder, flags, viewHolder.getUnmodifiedPayloads());
                    if (wasHidden) {
                        recordAnimationInfoIfBouncedHiddenView(viewHolder, animationInfo);
                    } else {
                        mViewInfoStore.addToAppearedInPreLayoutHolders(viewHolder, animationInfo);
                    }
                }
            }
            // we don't process disappearing list because they may re-appear in post layout pass.
            clearOldPositions();
        } else {
            clearOldPositions();
        }
        onExitLayoutOrScroll();
        stopInterceptRequestLayout(false);
        mState.mLayoutStep = State.STEP_LAYOUT;
    }

    /**
     * The second layout step where we do the actual layout of the views for the final state.
     * This step might be run multiple times if necessary (e.g. measure).
     */
    private void dispatchLayoutStep2() {
        startInterceptRequestLayout();
        onEnterLayoutOrScroll();
        mState.assertLayoutStep(State.STEP_LAYOUT | State.STEP_ANIMATIONS);
        mAdapterHelper.consumeUpdatesInOnePass();
        mState.mItemCount = mAdapter.getItemCount();
        mState.mDeletedInvisibleItemCountSincePreviousLayout = 0;

        // Step 2: Run layout
        mState.mInPreLayout = false;
        mLayout.onLayoutChildren(mRecycler, mState);

        mState.mStructureChanged = false;
        mPendingSavedState = null;

        // onLayoutChildren may have caused client code to disable item animations; re-check
        mState.mRunSimpleAnimations = mState.mRunSimpleAnimations && mItemAnimator != null;
        mState.mLayoutStep = State.STEP_ANIMATIONS;
        onExitLayoutOrScroll();
        stopInterceptRequestLayout(false);
    }

    /**
     * The final step of the layout where we save the information about views for animations,
     * trigger animations and do any necessary cleanup.
     */
    private void dispatchLayoutStep3() {
        mState.assertLayoutStep(State.STEP_ANIMATIONS);
        startInterceptRequestLayout();
        onEnterLayoutOrScroll();
        mState.mLayoutStep = State.STEP_START;
        if (mState.mRunSimpleAnimations) {
            // Step 3: Find out where things are now, and process change animations.
            // traverse list in reverse because we may call animateChange in the loop which may
            // remove the target view holder.
            for (int i = mChildHelper.getChildCount() - 1; i >= 0; i--) {
                ViewHolder holder = getChildViewHolderInt(mChildHelper.getChildAt(i));
                if (holder.shouldIgnore()) {
                    continue;
                }
                long key = getChangedHolderKey(holder);
                final ItemHolderInfo animationInfo = mItemAnimator
                        .recordPostLayoutInformation(mState, holder);
                ViewHolder oldChangeViewHolder = mViewInfoStore.getFromOldChangeHolders(key);
                if (oldChangeViewHolder != null && !oldChangeViewHolder.shouldIgnore()) {
                    // run a change animation

                    // If an Item is CHANGED but the updated version is disappearing, it creates
                    // a conflicting case.
                    // Since a view that is marked as disappearing is likely to be going out of
                    // bounds, we run a change animation. Both views will be cleaned automatically
                    // once their animations finish.
                    // On the other hand, if it is the same view holder instance, we run a
                    // disappearing animation instead because we are not going to rebind the updated
                    // VH unless it is enforced by the layout manager.
                    final boolean oldDisappearing = mViewInfoStore.isDisappearing(
                            oldChangeViewHolder);
                    final boolean newDisappearing = mViewInfoStore.isDisappearing(holder);
                    if (oldDisappearing && oldChangeViewHolder == holder) {
                        // run disappear animation instead of change
                        mViewInfoStore.addToPostLayout(holder, animationInfo);
                    } else {
                        final ItemHolderInfo preInfo = mViewInfoStore.popFromPreLayout(
                                oldChangeViewHolder);
                        // we add and remove so that any post info is merged.
                        mViewInfoStore.addToPostLayout(holder, animationInfo);
                        ItemHolderInfo postInfo = mViewInfoStore.popFromPostLayout(holder);
                        if (preInfo == null) {
                            handleMissingPreInfoForChangeError(key, holder, oldChangeViewHolder);
                        } else {
                            animateChange(oldChangeViewHolder, holder, preInfo, postInfo,
                                    oldDisappearing, newDisappearing);
                        }
                    }
                } else {
                    mViewInfoStore.addToPostLayout(holder, animationInfo);
                }
            }

            // Step 4: Process view info lists and trigger animations
            mViewInfoStore.process(mViewInfoProcessCallback);
        }

        mLayout.removeAndRecycleScrapInt(mRecycler);
        mState.mPreviousLayoutItemCount = mState.mItemCount;
        mDataSetHasChangedAfterLayout = false;
        mDispatchItemsChangedEvent = false;
        mState.mRunSimpleAnimations = false;

        mState.mRunPredictiveAnimations = false;
        mLayout.mRequestedSimpleAnimations = false;
        if (mRecycler.mChangedScrap != null) {
            mRecycler.mChangedScrap.clear();
        }
        if (mLayout.mPrefetchMaxObservedInInitialPrefetch) {
            // Initial prefetch has expanded cache, so reset until next prefetch.
            // This prevents initial prefetches from expanding the cache permanently.
            mLayout.mPrefetchMaxCountObserved = 0;
            mLayout.mPrefetchMaxObservedInInitialPrefetch = false;
            mRecycler.updateViewCacheSize();
        }

        mLayout.onLayoutCompleted(mState);
        onExitLayoutOrScroll();
        stopInterceptRequestLayout(false);
        mViewInfoStore.clear();
        if (didChildRangeChange(mMinMaxLayoutPositions[0], mMinMaxLayoutPositions[1])) {
            dispatchOnScrolled(0, 0);
        }
        recoverFocusFromState();
        resetFocusInfo();
    }

    /**
     * This handles the case where there is an unexpected VH missing in the pre-layout map.
     * <p>
     * We might be able to detect the error in the application which will help the developer to
     * resolve the issue.
     * <p>
     * If it is not an expected error, we at least print an error to notify the developer and ignore
     * the animation.
     * <p>
     * https://code.google.com/p/android/issues/detail?id=193958
     *
     * @param key                 The change key
     * @param holder              Current ViewHolder
     * @param oldChangeViewHolder Changed ViewHolder
     */
    private void handleMissingPreInfoForChangeError(long key,
                                                    ViewHolder holder, ViewHolder oldChangeViewHolder) {
        // check if two VH have the same key, if so, print that as an error
        final int childCount = mChildHelper.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = mChildHelper.getChildAt(i);
            ViewHolder other = getChildViewHolderInt(view);
            if (other == holder) {
                continue;
            }
            final long otherKey = getChangedHolderKey(other);
            if (otherKey == key) {
                if (mAdapter != null && mAdapter.hasStableIds()) {
                    throw new IllegalStateException("Two different ViewHolders have the same stable"
                            + " ID. Stable IDs in your adapter MUST BE unique and SHOULD NOT"
                            + " change.\n ViewHolder 1:" + other + " \n View Holder 2:" + holder
                            + exceptionLabel());
                } else {
                    throw new IllegalStateException("Two different ViewHolders have the same change"
                            + " ID. This might happen due to inconsistent Adapter update events or"
                            + " if the LayoutManager lays out the same View multiple times."
                            + "\n ViewHolder 1:" + other + " \n View Holder 2:" + holder
                            + exceptionLabel());
                }
            }
        }
        // Very unlikely to happen but if it does, notify the developer.
        Log.e(TAG, "Problem while matching changed view holders with the new"
                + "ones. The pre-layout information for the change holder " + oldChangeViewHolder
                + " cannot be found but it is necessary for " + holder + exceptionLabel());
    }

    /**
     * Records the animation information for a view holder that was bounced from hidden list. It
     * also clears the bounce back flag.
     */
    void recordAnimationInfoIfBouncedHiddenView(ViewHolder viewHolder,
                                                ItemHolderInfo animationInfo) {
        // looks like this view bounced back from hidden list!
        viewHolder.setFlags(0, ViewHolder.FLAG_BOUNCED_FROM_HIDDEN_LIST);
        if (mState.mTrackOldChangeHolders && viewHolder.isUpdated()
                && !viewHolder.isRemoved() && !viewHolder.shouldIgnore()) {
            long key = getChangedHolderKey(viewHolder);
            mViewInfoStore.addToOldChangeHolders(key, viewHolder);
        }
        mViewInfoStore.addToPreLayout(viewHolder, animationInfo);
    }

    private void findMinMaxChildLayoutPositions(int[] into) {
        final int count = mChildHelper.getChildCount();
        if (count == 0) {
            into[0] = NO_POSITION;
            into[1] = NO_POSITION;
            return;
        }
        int minPositionPreLayout = Integer.MAX_VALUE;
        int maxPositionPreLayout = Integer.MIN_VALUE;
        for (int i = 0; i < count; ++i) {
            final ViewHolder holder = getChildViewHolderInt(mChildHelper.getChildAt(i));
            if (holder.shouldIgnore()) {
                continue;
            }
            final int pos = holder.getLayoutPosition();
            if (pos < minPositionPreLayout) {
                minPositionPreLayout = pos;
            }
            if (pos > maxPositionPreLayout) {
                maxPositionPreLayout = pos;
            }
        }
        into[0] = minPositionPreLayout;
        into[1] = maxPositionPreLayout;
    }

    private boolean didChildRangeChange(int minPositionPreLayout, int maxPositionPreLayout) {
        findMinMaxChildLayoutPositions(mMinMaxLayoutPositions);
        return mMinMaxLayoutPositions[0] != minPositionPreLayout
                || mMinMaxLayoutPositions[1] != maxPositionPreLayout;
    }

    @Override
    protected void removeDetachedView(View child, boolean animate) {
        ViewHolder vh = getChildViewHolderInt(child);
        if (vh != null) {
            if (vh.isTmpDetached()) {
                vh.clearTmpDetachFlag();
            } else if (!vh.shouldIgnore()) {
                throw new IllegalArgumentException("Called removeDetachedView with a view which"
                        + " is not flagged as tmp detached." + vh + exceptionLabel());
            }
        }

        // Clear any android.view.animation.Animation that may prevent the item from
        // detaching when being removed. If a child is re-added before the
        // lazy detach occurs, it will receive invalid attach/detach sequencing.
        child.clearAnimation();

        dispatchChildDetached(child);
        super.removeDetachedView(child, animate);
    }

    /**
     * Returns a unique key to be used while handling change animations.
     * It might be child's position or stable id depending on the adapter type.
     */
    long getChangedHolderKey(ViewHolder holder) {
        return mAdapter.hasStableIds() ? holder.getItemId() : holder.mPosition;
    }

    void animateAppearance(@NonNull ViewHolder itemHolder,
                           @Nullable ItemHolderInfo preLayoutInfo, @NonNull ItemHolderInfo postLayoutInfo) {
        itemHolder.setIsRecyclable(false);
        if (mItemAnimator.animateAppearance(itemHolder, preLayoutInfo, postLayoutInfo)) {
            postAnimationRunner();
        }
    }

    void animateDisappearance(@NonNull ViewHolder holder,
                              @NonNull ItemHolderInfo preLayoutInfo, @Nullable ItemHolderInfo postLayoutInfo) {
        addAnimatingView(holder);
        holder.setIsRecyclable(false);
        if (mItemAnimator.animateDisappearance(holder, preLayoutInfo, postLayoutInfo)) {
            postAnimationRunner();
        }
    }

    private void animateChange(@NonNull ViewHolder oldHolder, @NonNull ViewHolder newHolder,
                               @NonNull ItemHolderInfo preInfo, @NonNull ItemHolderInfo postInfo,
                               boolean oldHolderDisappearing, boolean newHolderDisappearing) {
        oldHolder.setIsRecyclable(false);
        if (oldHolderDisappearing) {
            addAnimatingView(oldHolder);
        }
        if (oldHolder != newHolder) {
            if (newHolderDisappearing) {
                addAnimatingView(newHolder);
            }
            oldHolder.mShadowedHolder = newHolder;
            // old holder should disappear after animation ends
            addAnimatingView(oldHolder);
            mRecycler.unscrapView(oldHolder);
            newHolder.setIsRecyclable(false);
            newHolder.mShadowingHolder = oldHolder;
        }
        if (mItemAnimator.animateChange(oldHolder, newHolder, preInfo, postInfo)) {
            postAnimationRunner();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        TraceCompat.beginSection(TRACE_ON_LAYOUT_TAG);
        dispatchLayout();
        TraceCompat.endSection();
        mFirstLayoutComplete = true;
    }

    @Override
    public void requestLayout() {
        if (mInterceptRequestLayoutDepth == 0 && !mLayoutFrozen) {
            super.requestLayout();
        } else {
            mLayoutWasDefered = true;
        }
    }

    void markItemDecorInsetsDirty() {
        final int childCount = mChildHelper.getUnfilteredChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = mChildHelper.getUnfilteredChildAt(i);
            ((LayoutParams) child.getLayoutParams()).mInsetsDirty = true;
        }
        mRecycler.markItemDecorInsetsDirty();
    }

    @Override
    public void draw(Canvas c) {
        super.draw(c);

        //调用ItemDecorations的onDrawOver()
        final int count = mItemDecorations.size();
        for (int i = 0; i < count; i++) {
            mItemDecorations.get(i).onDrawOver(c, this, mState);
        }

        // TODO If padding is not 0 and clipChildrenToPadding is false, to draw glows properly, we
        // need find children closest to edges. Not sure if it is worth the effort.
        boolean needsInvalidate = false;
        if (mLeftGlow != null && !mLeftGlow.isFinished()) {
            final int restore = c.save();
            final int padding = mClipToPadding ? getPaddingBottom() : 0;
            c.rotate(270);
            c.translate(-getHeight() + padding, 0);
            needsInvalidate = mLeftGlow != null && mLeftGlow.draw(c);
            c.restoreToCount(restore);
        }
        if (mTopGlow != null && !mTopGlow.isFinished()) {
            final int restore = c.save();
            if (mClipToPadding) {
                c.translate(getPaddingLeft(), getPaddingTop());
            }
            needsInvalidate |= mTopGlow != null && mTopGlow.draw(c);
            c.restoreToCount(restore);
        }
        if (mRightGlow != null && !mRightGlow.isFinished()) {
            final int restore = c.save();
            final int width = getWidth();
            final int padding = mClipToPadding ? getPaddingTop() : 0;
            c.rotate(90);
            c.translate(-padding, -width);
            needsInvalidate |= mRightGlow != null && mRightGlow.draw(c);
            c.restoreToCount(restore);
        }
        if (mBottomGlow != null && !mBottomGlow.isFinished()) {
            final int restore = c.save();
            c.rotate(180);
            if (mClipToPadding) {
                c.translate(-getWidth() + getPaddingRight(), -getHeight() + getPaddingBottom());
            } else {
                c.translate(-getWidth(), -getHeight());
            }
            needsInvalidate |= mBottomGlow != null && mBottomGlow.draw(c);
            c.restoreToCount(restore);
        }

        // If some views are animating, ItemDecorators are likely to move/change with them.
        // Invalidate RecyclerView to re-draw decorators. This is still efficient because children's
        // display lists are not invalidated.
        if (!needsInvalidate && mItemAnimator != null && mItemDecorations.size() > 0
                && mItemAnimator.isRunning()) {
            needsInvalidate = true;
        }

        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);

        //调用ItemDecoration的onDraw()方法
        final int count = mItemDecorations.size();
        for (int i = 0; i < count; i++) {
            mItemDecorations.get(i).onDraw(c, this, mState);
        }

    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams && mLayout.checkLayoutParams((LayoutParams) p);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        if (mLayout == null) {
            throw new IllegalStateException("RecyclerView has no LayoutManager" + exceptionLabel());
        }
        return mLayout.generateDefaultLayoutParams();
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        if (mLayout == null) {
            throw new IllegalStateException("RecyclerView has no LayoutManager" + exceptionLabel());
        }
        return mLayout.generateLayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        if (mLayout == null) {
            throw new IllegalStateException("RecyclerView has no LayoutManager" + exceptionLabel());
        }
        return mLayout.generateLayoutParams(p);
    }

    /**
     * Returns true if RecyclerView is currently running some animations.
     * <p>
     * If you want to be notified when animations are finished, use
     * {@link ItemAnimator#isRunning(ItemAnimator.ItemAnimatorFinishedListener)}.
     *
     * @return True if there are some item animations currently running or waiting to be started.
     */
    public boolean isAnimating() {
        return mItemAnimator != null && mItemAnimator.isRunning();
    }

    void saveOldPositions() {
        final int childCount = mChildHelper.getUnfilteredChildCount();
        for (int i = 0; i < childCount; i++) {
            final ViewHolder holder = getChildViewHolderInt(mChildHelper.getUnfilteredChildAt(i));
            if (DEBUG && holder.mPosition == -1 && !holder.isRemoved()) {
                throw new IllegalStateException("view holder cannot have position -1 unless it"
                        + " is removed" + exceptionLabel());
            }
            if (!holder.shouldIgnore()) {
                holder.saveOldPosition();
            }
        }
    }

    void clearOldPositions() {
        final int childCount = mChildHelper.getUnfilteredChildCount();
        for (int i = 0; i < childCount; i++) {
            final ViewHolder holder = getChildViewHolderInt(mChildHelper.getUnfilteredChildAt(i));
            if (!holder.shouldIgnore()) {
                holder.clearOldPosition();
            }
        }
        mRecycler.clearOldPositions();
    }

    void offsetPositionRecordsForMove(int from, int to) {
        final int childCount = mChildHelper.getUnfilteredChildCount();
        final int start, end, inBetweenOffset;
        if (from < to) {
            start = from;
            end = to;
            inBetweenOffset = -1;
        } else {
            start = to;
            end = from;
            inBetweenOffset = 1;
        }

        for (int i = 0; i < childCount; i++) {
            final ViewHolder holder = getChildViewHolderInt(mChildHelper.getUnfilteredChildAt(i));
            if (holder == null || holder.mPosition < start || holder.mPosition > end) {
                continue;
            }
            if (DEBUG) {
                Log.d(TAG, "offsetPositionRecordsForMove attached child " + i + " holder "
                        + holder);
            }
            if (holder.mPosition == from) {
                holder.offsetPosition(to - from, false);
            } else {
                holder.offsetPosition(inBetweenOffset, false);
            }

            mState.mStructureChanged = true;
        }
        mRecycler.offsetPositionRecordsForMove(from, to);
        requestLayout();
    }

    void offsetPositionRecordsForInsert(int positionStart, int itemCount) {
        final int childCount = mChildHelper.getUnfilteredChildCount();
        for (int i = 0; i < childCount; i++) {
            final ViewHolder holder = getChildViewHolderInt(mChildHelper.getUnfilteredChildAt(i));
            if (holder != null && !holder.shouldIgnore() && holder.mPosition >= positionStart) {
                if (DEBUG) {
                    Log.d(TAG, "offsetPositionRecordsForInsert attached child " + i + " holder "
                            + holder + " now at position " + (holder.mPosition + itemCount));
                }
                holder.offsetPosition(itemCount, false);
                mState.mStructureChanged = true;
            }
        }
        mRecycler.offsetPositionRecordsForInsert(positionStart, itemCount);
        requestLayout();
    }

    void offsetPositionRecordsForRemove(int positionStart, int itemCount,
                                        boolean applyToPreLayout) {
        final int positionEnd = positionStart + itemCount;
        final int childCount = mChildHelper.getUnfilteredChildCount();
        for (int i = 0; i < childCount; i++) {
            final ViewHolder holder = getChildViewHolderInt(mChildHelper.getUnfilteredChildAt(i));
            if (holder != null && !holder.shouldIgnore()) {
                if (holder.mPosition >= positionEnd) {
                    if (DEBUG) {
                        Log.d(TAG, "offsetPositionRecordsForRemove attached child " + i
                                + " holder " + holder + " now at position "
                                + (holder.mPosition - itemCount));
                    }
                    holder.offsetPosition(-itemCount, applyToPreLayout);
                    mState.mStructureChanged = true;
                } else if (holder.mPosition >= positionStart) {
                    if (DEBUG) {
                        Log.d(TAG, "offsetPositionRecordsForRemove attached child " + i
                                + " holder " + holder + " now REMOVED");
                    }
                    holder.flagRemovedAndOffsetPosition(positionStart - 1, -itemCount,
                            applyToPreLayout);
                    mState.mStructureChanged = true;
                }
            }
        }
        mRecycler.offsetPositionRecordsForRemove(positionStart, itemCount, applyToPreLayout);
        requestLayout();
    }

    /**
     * Rebind existing views for the given range, or create as needed.
     *
     * @param positionStart Adapter position to start at
     * @param itemCount     Number of views that must explicitly be rebound
     */
    void viewRangeUpdate(int positionStart, int itemCount, Object payload) {
        final int childCount = mChildHelper.getUnfilteredChildCount();
        final int positionEnd = positionStart + itemCount;

        for (int i = 0; i < childCount; i++) {
            final View child = mChildHelper.getUnfilteredChildAt(i);
            final ViewHolder holder = getChildViewHolderInt(child);
            if (holder == null || holder.shouldIgnore()) {
                continue;
            }
            if (holder.mPosition >= positionStart && holder.mPosition < positionEnd) {
                // We re-bind these view holders after pre-processing is complete so that
                // ViewHolders have their final positions assigned.
                holder.addFlags(ViewHolder.FLAG_UPDATE);
                holder.addChangePayload(payload);
                // lp cannot be null since we get ViewHolder from it.
                ((LayoutParams) child.getLayoutParams()).mInsetsDirty = true;
            }
        }
        mRecycler.viewRangeUpdate(positionStart, itemCount);
    }

    boolean canReuseUpdatedViewHolder(ViewHolder viewHolder) {
        return mItemAnimator == null || mItemAnimator.canReuseUpdatedViewHolder(viewHolder,
                viewHolder.getUnmodifiedPayloads());
    }

    /**
     * Processes the fact that, as far as we can tell, the data set has completely changed.
     *
     * <ul>
     * <li>Once layout occurs, all attached items should be discarded or animated.
     * <li>Attached items are labeled as invalid.
     * <li>Because items may still be prefetched between a "data set completely changed"
     * event and a layout event, all cached items are discarded.
     * </ul>
     *
     * @param dispatchItemsChanged Whether to call
     *                             {@link LayoutManager#onItemsChanged(RecyclerView)} during measure/layout.
     */
    void processDataSetCompletelyChanged(boolean dispatchItemsChanged) {
        mDispatchItemsChangedEvent |= dispatchItemsChanged;
        mDataSetHasChangedAfterLayout = true;
        markKnownViewsInvalid();
    }

    /**
     * Mark all known views as invalid. Used in response to a, "the whole world might have changed"
     * data change event.
     */
    void markKnownViewsInvalid() {
        final int childCount = mChildHelper.getUnfilteredChildCount();
        for (int i = 0; i < childCount; i++) {
            final ViewHolder holder = getChildViewHolderInt(mChildHelper.getUnfilteredChildAt(i));
            if (holder != null && !holder.shouldIgnore()) {
                holder.addFlags(ViewHolder.FLAG_UPDATE | ViewHolder.FLAG_INVALID);
            }
        }
        markItemDecorInsetsDirty();
        mRecycler.markKnownViewsInvalid();
    }

    /**
     * Invalidates all ItemDecorations. If RecyclerView has item decorations, calling this method
     * will trigger a {@link #requestLayout()} call.
     */
    public void invalidateItemDecorations() {
        if (mItemDecorations.size() == 0) {
            return;
        }
        if (mLayout != null) {
            mLayout.assertNotInLayoutOrScroll("Cannot invalidate item decorations during a scroll"
                    + " or layout");
        }
        markItemDecorInsetsDirty();
        requestLayout();
    }

    /**
     * Returns true if the RecyclerView should attempt to preserve currently focused Adapter Item's
     * focus even if the View representing the Item is replaced during a layout calculation.
     * <p>
     * By default, this value is {@code true}.
     *
     * @return True if the RecyclerView will try to preserve focused Item after a layout if it loses
     * focus.
     * @see #setPreserveFocusAfterLayout(boolean)
     */
    public boolean getPreserveFocusAfterLayout() {
        return mPreserveFocusAfterLayout;
    }

    /**
     * Set whether the RecyclerView should try to keep the same Item focused after a layout
     * calculation or not.
     * <p>
     * Usually, LayoutManagers keep focused views visible before and after layout but sometimes,
     * views may lose focus during a layout calculation as their state changes or they are replaced
     * with another view due to type change or animation. In these cases, RecyclerView can request
     * focus on the new view automatically.
     *
     * @param preserveFocusAfterLayout Whether RecyclerView should preserve focused Item during a
     *                                 layout calculations. Defaults to true.
     * @see #getPreserveFocusAfterLayout()
     */
    public void setPreserveFocusAfterLayout(boolean preserveFocusAfterLayout) {
        mPreserveFocusAfterLayout = preserveFocusAfterLayout;
    }

    /**
     * Retrieve the {@link ViewHolder} for the given child view.
     *
     * @param child Child of this RecyclerView to query for its ViewHolder
     * @return The child view's ViewHolder
     */
    public ViewHolder getChildViewHolder(@NonNull View child) {
        final ViewParent parent = child.getParent();
        if (parent != null && parent != this) {
            throw new IllegalArgumentException("View " + child + " is not a direct child of "
                    + this);
        }
        return getChildViewHolderInt(child);
    }

    /**
     * Traverses the ancestors of the given view and returns the item view that contains it and
     * also a direct child of the RecyclerView. This returned view can be used to get the
     * ViewHolder by calling {@link #getChildViewHolder(View)}.
     *
     * @param view The view that is a descendant of the RecyclerView.
     * @return The direct child of the RecyclerView which contains the given view or null if the
     * provided view is not a descendant of this RecyclerView.
     * @see #getChildViewHolder(View)
     * @see #findContainingViewHolder(View)
     */
    @Nullable
    public View findContainingItemView(@NonNull View view) {
        ViewParent parent = view.getParent();
        while (parent != null && parent != this && parent instanceof View) {
            view = (View) parent;
            parent = view.getParent();
        }
        return parent == this ? view : null;
    }

    /**
     * Returns the ViewHolder that contains the given view.
     *
     * @param view The view that is a descendant of the RecyclerView.
     * @return The ViewHolder that contains the given view or null if the provided view is not a
     * descendant of this RecyclerView.
     */
    @Nullable
    public ViewHolder findContainingViewHolder(@NonNull View view) {
        View itemView = findContainingItemView(view);
        return itemView == null ? null : getChildViewHolder(itemView);
    }


    static ViewHolder getChildViewHolderInt(View child) {
        if (child == null) {
            return null;
        }
        return ((LayoutParams) child.getLayoutParams()).mViewHolder;
    }

    /**
     * @deprecated use {@link #getChildAdapterPosition(View)} or
     * {@link #getChildLayoutPosition(View)}.
     */
    @Deprecated
    public int getChildPosition(@NonNull View child) {
        return getChildAdapterPosition(child);
    }

    /**
     * Return the adapter position that the given child view corresponds to.
     *
     * @param child Child View to query
     * @return Adapter position corresponding to the given view or {@link #NO_POSITION}
     */
    public int getChildAdapterPosition(@NonNull View child) {
        final ViewHolder holder = getChildViewHolderInt(child);
        return holder != null ? holder.getAdapterPosition() : NO_POSITION;
    }

    /**
     * Return the adapter position of the given child view as of the latest completed layout pass.
     * <p>
     * This position may not be equal to Item's adapter position if there are pending changes
     * in the adapter which have not been reflected to the layout yet.
     *
     * @param child Child View to query
     * @return Adapter position of the given View as of last layout pass or {@link #NO_POSITION} if
     * the View is representing a removed item.
     */
    public int getChildLayoutPosition(@NonNull View child) {
        final ViewHolder holder = getChildViewHolderInt(child);
        return holder != null ? holder.getLayoutPosition() : NO_POSITION;
    }

    /**
     * Return the stable item id that the given child view corresponds to.
     *
     * @param child Child View to query
     * @return Item id corresponding to the given view or {@link #NO_ID}
     */
    public long getChildItemId(@NonNull View child) {
        if (mAdapter == null || !mAdapter.hasStableIds()) {
            return NO_ID;
        }
        final ViewHolder holder = getChildViewHolderInt(child);
        return holder != null ? holder.getItemId() : NO_ID;
    }

    /**
     * @deprecated use {@link #findViewHolderForLayoutPosition(int)} or
     * {@link #findViewHolderForAdapterPosition(int)}
     */
    @Deprecated
    @Nullable
    public ViewHolder findViewHolderForPosition(int position) {
        return findViewHolderForPosition(position, false);
    }

    /**
     * Return the ViewHolder for the item in the given position of the data set as of the latest
     * layout pass.
     * <p>
     * This method checks only the children of RecyclerView. If the item at the given
     * <code>position</code> is not laid out, it <em>will not</em> create a new one.
     * <p>
     * Note that when Adapter contents change, ViewHolder positions are not updated until the
     * next layout calculation. If there are pending adapter updates, the return value of this
     * method may not match your adapter contents. You can use
     * #{@link ViewHolder#getAdapterPosition()} to get the current adapter position of a ViewHolder.
     * <p>
     * When the ItemAnimator is running a change animation, there might be 2 ViewHolders
     * with the same layout position representing the same Item. In this case, the updated
     * ViewHolder will be returned.
     *
     * @param position The position of the item in the data set of the adapter
     * @return The ViewHolder at <code>position</code> or null if there is no such item
     */
    @Nullable
    public ViewHolder findViewHolderForLayoutPosition(int position) {
        return findViewHolderForPosition(position, false);
    }

    /**
     * Return the ViewHolder for the item in the given position of the data set. Unlike
     * {@link #findViewHolderForLayoutPosition(int)} this method takes into account any pending
     * adapter changes that may not be reflected to the layout yet. On the other hand, if
     * {@link Adapter#notifyDataSetChanged()} has been called but the new layout has not been
     * calculated yet, this method will return <code>null</code> since the new positions of views
     * are unknown until the layout is calculated.
     * <p>
     * This method checks only the children of RecyclerView. If the item at the given
     * <code>position</code> is not laid out, it <em>will not</em> create a new one.
     * <p>
     * When the ItemAnimator is running a change animation, there might be 2 ViewHolders
     * representing the same Item. In this case, the updated ViewHolder will be returned.
     *
     * @param position The position of the item in the data set of the adapter
     * @return The ViewHolder at <code>position</code> or null if there is no such item
     */
    @Nullable
    public ViewHolder findViewHolderForAdapterPosition(int position) {
        if (mDataSetHasChangedAfterLayout) {
            return null;
        }
        final int childCount = mChildHelper.getUnfilteredChildCount();
        // hidden VHs are not preferred but if that is the only one we find, we rather return it
        ViewHolder hidden = null;
        for (int i = 0; i < childCount; i++) {
            final ViewHolder holder = getChildViewHolderInt(mChildHelper.getUnfilteredChildAt(i));
            if (holder != null && !holder.isRemoved()
                    && getAdapterPositionFor(holder) == position) {
                if (mChildHelper.isHidden(holder.itemView)) {
                    hidden = holder;
                } else {
                    return holder;
                }
            }
        }
        return hidden;
    }

    @Nullable
    ViewHolder findViewHolderForPosition(int position, boolean checkNewPosition) {
        final int childCount = mChildHelper.getUnfilteredChildCount();
        ViewHolder hidden = null;
        for (int i = 0; i < childCount; i++) {
            final ViewHolder holder = getChildViewHolderInt(mChildHelper.getUnfilteredChildAt(i));
            if (holder != null && !holder.isRemoved()) {
                if (checkNewPosition) {
                    if (holder.mPosition != position) {
                        continue;
                    }
                } else if (holder.getLayoutPosition() != position) {
                    continue;
                }
                if (mChildHelper.isHidden(holder.itemView)) {
                    hidden = holder;
                } else {
                    return holder;
                }
            }
        }
        // This method should not query cached views. It creates a problem during adapter updates
        // when we are dealing with already laid out views. Also, for the public method, it is more
        // reasonable to return null if position is not laid out.
        return hidden;
    }

    /**
     * Return the ViewHolder for the item with the given id. The RecyclerView must
     * use an Adapter with {@link Adapter#setHasStableIds(boolean) stableIds} to
     * return a non-null value.
     * <p>
     * This method checks only the children of RecyclerView. If the item with the given
     * <code>id</code> is not laid out, it <em>will not</em> create a new one.
     * <p>
     * When the ItemAnimator is running a change animation, there might be 2 ViewHolders with the
     * same id. In this case, the updated ViewHolder will be returned.
     *
     * @param id The id for the requested item
     * @return The ViewHolder with the given <code>id</code> or null if there is no such item
     */
    public ViewHolder findViewHolderForItemId(long id) {
        if (mAdapter == null || !mAdapter.hasStableIds()) {
            return null;
        }
        final int childCount = mChildHelper.getUnfilteredChildCount();
        ViewHolder hidden = null;
        for (int i = 0; i < childCount; i++) {
            final ViewHolder holder = getChildViewHolderInt(mChildHelper.getUnfilteredChildAt(i));
            if (holder != null && !holder.isRemoved() && holder.getItemId() == id) {
                if (mChildHelper.isHidden(holder.itemView)) {
                    hidden = holder;
                } else {
                    return holder;
                }
            }
        }
        return hidden;
    }

    /**
     * Find the topmost view under the given point.
     *
     * @param x Horizontal position in pixels to search
     * @param y Vertical position in pixels to search
     * @return The child view under (x, y) or null if no matching child is found
     */
    @Nullable
    public View findChildViewUnder(float x, float y) {
        final int count = mChildHelper.getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            final View child = mChildHelper.getChildAt(i);
            final float translationX = child.getTranslationX();
            final float translationY = child.getTranslationY();
            if (x >= child.getLeft() + translationX
                    && x <= child.getRight() + translationX
                    && y >= child.getTop() + translationY
                    && y <= child.getBottom() + translationY) {
                return child;
            }
        }
        return null;
    }

    @Override
    public boolean drawChild(Canvas canvas, View child, long drawingTime) {
        return super.drawChild(canvas, child, drawingTime);
    }

    /**
     * Offset the bounds of all child views by <code>dy</code> pixels.
     * Useful for implementing simple scrolling in {@link LayoutManager LayoutManagers}.
     *
     * @param dy Vertical pixel offset to apply to the bounds of all child views
     */
    public void offsetChildrenVertical(@Px int dy) {
        final int childCount = mChildHelper.getChildCount();
        for (int i = 0; i < childCount; i++) {
            mChildHelper.getChildAt(i).offsetTopAndBottom(dy);
        }
    }

    /**
     * Called when an item view is attached to this RecyclerView.
     *
     * <p>Subclasses of RecyclerView may want to perform extra bookkeeping or modifications
     * of child views as they become attached. This will be called before a
     * {@link LayoutManager} measures or lays out the view and is a good time to perform these
     * changes.</p>
     *
     * @param child Child view that is now attached to this RecyclerView and its associated window
     */
    public void onChildAttachedToWindow(@NonNull View child) {
    }

    /**
     * Called when an item view is detached from this RecyclerView.
     *
     * <p>Subclasses of RecyclerView may want to perform extra bookkeeping or modifications
     * of child views as they become detached. This will be called as a
     * {@link LayoutManager} fully detaches the child view from the parent and its window.</p>
     *
     * @param child Child view that is now detached from this RecyclerView and its associated window
     */
    public void onChildDetachedFromWindow(@NonNull View child) {
    }

    /**
     * Offset the bounds of all child views by <code>dx</code> pixels.
     * Useful for implementing simple scrolling in {@link LayoutManager LayoutManagers}.
     *
     * @param dx Horizontal pixel offset to apply to the bounds of all child views
     */
    public void offsetChildrenHorizontal(@Px int dx) {
        final int childCount = mChildHelper.getChildCount();
        for (int i = 0; i < childCount; i++) {
            mChildHelper.getChildAt(i).offsetLeftAndRight(dx);
        }
    }

    /**
     * Returns the bounds of the view including its decoration and margins.
     *
     * @param view      The view element to check
     * @param outBounds A rect that will receive the bounds of the element including its
     *                  decoration and margins.
     */
    public void getDecoratedBoundsWithMargins(@NonNull View view, @NonNull Rect outBounds) {
        getDecoratedBoundsWithMarginsInt(view, outBounds);
    }

    static void getDecoratedBoundsWithMarginsInt(View view, Rect outBounds) {
        final LayoutParams lp = (LayoutParams) view.getLayoutParams();
        final Rect insets = lp.mDecorInsets;
        outBounds.set(view.getLeft() - insets.left - lp.leftMargin,
                view.getTop() - insets.top - lp.topMargin,
                view.getRight() + insets.right + lp.rightMargin,
                view.getBottom() + insets.bottom + lp.bottomMargin);
    }

    Rect getItemDecorInsetsForChild(View child) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (!lp.mInsetsDirty) {
            return lp.mDecorInsets;
        }

        if (mState.isPreLayout() && (lp.isItemChanged() || lp.isViewInvalid())) {
            // changed/invalid items should not be updated until they are rebound.
            return lp.mDecorInsets;
        }
        final Rect insets = lp.mDecorInsets;
        insets.set(0, 0, 0, 0);
        final int decorCount = mItemDecorations.size();
        for (int i = 0; i < decorCount; i++) {
            mTempRect.set(0, 0, 0, 0);
            mItemDecorations.get(i).getItemOffsets(mTempRect, child, this, mState);
            insets.left += mTempRect.left;
            insets.top += mTempRect.top;
            insets.right += mTempRect.right;
            insets.bottom += mTempRect.bottom;
        }
        lp.mInsetsDirty = false;
        return insets;
    }

    /**
     * Called when the scroll position of this RecyclerView changes. Subclasses should use
     * this method to respond to scrolling within the adapter's data set instead of an explicit
     * listener.
     *
     * <p>This method will always be invoked before listeners. If a subclass needs to perform
     * any additional upkeep or bookkeeping after scrolling but before listeners run,
     * this is a good place to do so.</p>
     *
     * <p>This differs from {@link View#onScrollChanged(int, int, int, int)} in that it receives
     * the distance scrolled in either direction within the adapter's data set instead of absolute
     * scroll coordinates. Since RecyclerView cannot compute the absolute scroll position from
     * any arbitrary point in the data set, <code>onScrollChanged</code> will always receive
     * the current {@link View#getScrollX()} and {@link View#getScrollY()} values which
     * do not correspond to the data set scroll position. However, some subclasses may choose
     * to use these fields as special offsets.</p>
     *
     * @param dx horizontal distance scrolled in pixels
     * @param dy vertical distance scrolled in pixels
     */
    public void onScrolled(@Px int dx, @Px int dy) {
        // Do nothing
    }

    void dispatchOnScrolled(int hresult, int vresult) {
        mDispatchScrollCounter++;
        // Pass the current scrollX/scrollY values; no actual change in these properties occurred
        // but some general-purpose code may choose to respond to changes this way.
        final int scrollX = getScrollX();
        final int scrollY = getScrollY();
        onScrollChanged(scrollX, scrollY, scrollX, scrollY);

        // Pass the real deltas to onScrolled, the RecyclerView-specific method.
        onScrolled(hresult, vresult);

        // Invoke listeners last. Subclassed view methods always handle the event first.
        // All internal state is consistent by the time listeners are invoked.
        if (mScrollListener != null) {
            mScrollListener.onScrolled(this, hresult, vresult);
        }
        if (mScrollListeners != null) {
            for (int i = mScrollListeners.size() - 1; i >= 0; i--) {
                mScrollListeners.get(i).onScrolled(this, hresult, vresult);
            }
        }
        mDispatchScrollCounter--;
    }

    /**
     * Called when the scroll state of this RecyclerView changes. Subclasses should use this
     * method to respond to state changes instead of an explicit listener.
     *
     * <p>This method will always be invoked before listeners, but after the LayoutManager
     * responds to the scroll state change.</p>
     *
     * @param state the new scroll state, one of {@link #SCROLL_STATE_IDLE},
     *              {@link #SCROLL_STATE_DRAGGING} or {@link #SCROLL_STATE_SETTLING}
     */
    public void onScrollStateChanged(int state) {
        // Do nothing
    }

    void dispatchOnScrollStateChanged(int state) {
        // Let the LayoutManager go first; this allows it to bring any properties into
        // a consistent state before the RecyclerView subclass responds.
        if (mLayout != null) {
            mLayout.onScrollStateChanged(state);
        }

        // Let the RecyclerView subclass handle this event next; any LayoutManager property
        // changes will be reflected by this time.
        onScrollStateChanged(state);

        // Listeners go last. All other internal state is consistent by this point.
        if (mScrollListener != null) {
            mScrollListener.onScrollStateChanged(this, state);
        }
        if (mScrollListeners != null) {
            for (int i = mScrollListeners.size() - 1; i >= 0; i--) {
                mScrollListeners.get(i).onScrollStateChanged(this, state);
            }
        }
    }

    /**
     * Returns whether there are pending adapter updates which are not yet applied to the layout.
     * <p>
     * If this method returns <code>true</code>, it means that what user is currently seeing may not
     * reflect them adapter contents (depending on what has changed).
     * You may use this information to defer or cancel some operations.
     * <p>
     * This method returns true if RecyclerView has not yet calculated the first layout after it is
     * attached to the Window or the Adapter has been replaced.
     *
     * @return True if there are some adapter updates which are not yet reflected to layout or false
     * if layout is up to date.
     */
    public boolean hasPendingAdapterUpdates() {
        return !mFirstLayoutComplete || mDataSetHasChangedAfterLayout
                || mAdapterHelper.hasPendingUpdates();
    }


    void repositionShadowingViews() {
        // Fix up shadow views used by change animations
        int count = mChildHelper.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = mChildHelper.getChildAt(i);
            ViewHolder holder = getChildViewHolder(view);
            if (holder != null && holder.mShadowingHolder != null) {
                View shadowingView = holder.mShadowingHolder.itemView;
                int left = view.getLeft();
                int top = view.getTop();
                if (left != shadowingView.getLeft() || top != shadowingView.getTop()) {
                    shadowingView.layout(left, top,
                            left + shadowingView.getWidth(),
                            top + shadowingView.getHeight());
                }
            }
        }
    }


    /**
     * Utility method for finding an internal RecyclerView, if present
     */
    @Nullable
    static RecyclerView findNestedRecyclerView(@NonNull View view) {
        if (!(view instanceof ViewGroup)) {
            return null;
        }
        if (view instanceof RecyclerView) {
            return (RecyclerView) view;
        }
        final ViewGroup parent = (ViewGroup) view;
        final int count = parent.getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = parent.getChildAt(i);
            final RecyclerView descendant = findNestedRecyclerView(child);
            if (descendant != null) {
                return descendant;
            }
        }
        return null;
    }

    /**
     * Utility method for clearing holder's internal RecyclerView, if present
     */
    static void clearNestedRecyclerViewIfNotNested(@NonNull ViewHolder holder) {
        if (holder.mNestedRecyclerView != null) {
            View item = holder.mNestedRecyclerView.get();
            while (item != null) {
                if (item == holder.itemView) {
                    return; // match found, don't need to clear
                }

                ViewParent parent = item.getParent();
                if (parent instanceof View) {
                    item = (View) parent;
                } else {
                    item = null;
                }
            }
            holder.mNestedRecyclerView = null; // not nested
        }
    }

    /**
     * Time base for deadline-aware work scheduling. Overridable for testing.
     * <p>
     * Will return 0 to avoid cost of System.nanoTime where deadline-aware work scheduling
     * isn't relevant.
     */
    long getNanoTime() {
        if (ALLOW_THREAD_GAP_WORK) {
            return System.nanoTime();
        } else {
            return 0;
        }
    }


    void dispatchChildDetached(View child) {
        final ViewHolder viewHolder = getChildViewHolderInt(child);
        onChildDetachedFromWindow(child);
        if (mAdapter != null && viewHolder != null) {
            mAdapter.onViewDetachedFromWindow(viewHolder);
        }
        if (mOnChildAttachStateListeners != null) {
            final int cnt = mOnChildAttachStateListeners.size();
            for (int i = cnt - 1; i >= 0; i--) {
                mOnChildAttachStateListeners.get(i).onChildViewDetachedFromWindow(child);
            }
        }
    }

    void dispatchChildAttached(View child) {
        final ViewHolder viewHolder = getChildViewHolderInt(child);
        onChildAttachedToWindow(child);
        if (mAdapter != null && viewHolder != null) {
            mAdapter.onViewAttachedToWindow(viewHolder);
        }
        if (mOnChildAttachStateListeners != null) {
            final int cnt = mOnChildAttachStateListeners.size();
            for (int i = cnt - 1; i >= 0; i--) {
                mOnChildAttachStateListeners.get(i).onChildViewAttachedToWindow(child);
            }
        }
    }


    /**
     * This method is here so that we can control the important for a11y changes and test it.
     */
    @VisibleForTesting
    boolean setChildImportantForAccessibilityInternal(ViewHolder viewHolder,
                                                      int importantForAccessibility) {
        if (isComputingLayout()) {
            viewHolder.mPendingAccessibilityState = importantForAccessibility;
            mPendingAccessibilityImportanceChange.add(viewHolder);
            return false;
        }
        ViewCompat.setImportantForAccessibility(viewHolder.itemView, importantForAccessibility);
        return true;
    }

    void dispatchPendingImportantForAccessibilityChanges() {
        for (int i = mPendingAccessibilityImportanceChange.size() - 1; i >= 0; i--) {
            ViewHolder viewHolder = mPendingAccessibilityImportanceChange.get(i);
            if (viewHolder.itemView.getParent() != this || viewHolder.shouldIgnore()) {
                continue;
            }
            int state = viewHolder.mPendingAccessibilityState;
            if (state != ViewHolder.PENDING_ACCESSIBILITY_STATE_NOT_SET) {
                //noinspection WrongConstant
                ViewCompat.setImportantForAccessibility(viewHolder.itemView, state);
                viewHolder.mPendingAccessibilityState =
                        ViewHolder.PENDING_ACCESSIBILITY_STATE_NOT_SET;
            }
        }
        mPendingAccessibilityImportanceChange.clear();
    }

    int getAdapterPositionFor(ViewHolder viewHolder) {
        if (viewHolder.hasAnyOfTheFlags(ViewHolder.FLAG_INVALID
                | ViewHolder.FLAG_REMOVED | ViewHolder.FLAG_ADAPTER_POSITION_UNKNOWN)
                || !viewHolder.isBound()) {
            return RecyclerView.NO_POSITION;
        }
        return mAdapterHelper.applyPendingUpdatesToPosition(viewHolder.mPosition);
    }

    /**
     * 初始化快速滑动
     * @param verticalThumbDrawable
     * @param verticalTrackDrawable
     * @param horizontalThumbDrawable
     * @param horizontalTrackDrawable
     */
    @VisibleForTesting
    void initFastScroller(StateListDrawable verticalThumbDrawable,
                          Drawable verticalTrackDrawable, StateListDrawable horizontalThumbDrawable,
                          Drawable horizontalTrackDrawable) {
        if (verticalThumbDrawable == null || verticalTrackDrawable == null
                || horizontalThumbDrawable == null || horizontalTrackDrawable == null) {
            throw new IllegalArgumentException("Trying to set fast scroller without both required drawables." + exceptionLabel());
        }

        Resources resources = getContext().getResources();
        new FastScroller(this, verticalThumbDrawable, verticalTrackDrawable,
                horizontalThumbDrawable,
                horizontalTrackDrawable,
                resources.getDimensionPixelSize(R.dimen.fastscroll_default_thickness),
                resources.getDimensionPixelSize(R.dimen.fastscroll_minimum_range),
                resources.getDimensionPixelOffset(R.dimen.fastscroll_margin));
    }

    // NestedScrollingChild

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        getScrollingChildHelper().setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return getScrollingChildHelper().isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return getScrollingChildHelper().startNestedScroll(axes);
    }

    @Override
    public boolean startNestedScroll(int axes, int type) {
        return getScrollingChildHelper().startNestedScroll(axes, type);
    }

    @Override
    public void stopNestedScroll() {
        getScrollingChildHelper().stopNestedScroll();
    }

    @Override
    public void stopNestedScroll(int type) {
        getScrollingChildHelper().stopNestedScroll(type);
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return getScrollingChildHelper().hasNestedScrollingParent();
    }

    @Override
    public boolean hasNestedScrollingParent(int type) {
        return getScrollingChildHelper().hasNestedScrollingParent(type);
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        return getScrollingChildHelper().dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow, int type) {
        return getScrollingChildHelper().dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow, type);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return getScrollingChildHelper().dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow,
                                           int type) {
        return getScrollingChildHelper().dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow,
                type);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return getScrollingChildHelper().dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return getScrollingChildHelper().dispatchNestedPreFling(velocityX, velocityY);
    }


    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (mChildDrawingOrderCallback == null) {
            return super.getChildDrawingOrder(childCount, i);
        } else {
            return mChildDrawingOrderCallback.onGetChildDrawingOrder(childCount, i);
        }
    }


    private NestedScrollingChildHelper getScrollingChildHelper() {
        if (mScrollingChildHelper == null) {
            mScrollingChildHelper = new NestedScrollingChildHelper(this);
        }
        return mScrollingChildHelper;
    }
}
