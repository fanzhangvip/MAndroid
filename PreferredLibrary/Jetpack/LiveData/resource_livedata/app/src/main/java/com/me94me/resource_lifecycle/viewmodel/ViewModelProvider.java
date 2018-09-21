
package com.me94me.resource_lifecycle.viewmodel;

import android.app.Application;

import java.lang.reflect.InvocationTargetException;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

/**
 * An utility class that provides {@code ViewModels} for a scope.
 * <p>
 * Default {@code ViewModelProvider} for an {@code Activity} or a {@code Fragment} can be obtained
 * from {@link ViewModelProviders} class.
 */
@SuppressWarnings("WeakerAccess")
public class ViewModelProvider {

    //获取ViewModel类的key前缀
    private static final String DEFAULT_KEY = "androidx.lifecycle.ViewModelProvider.DefaultKey";

    /**
     * {@code Factory}的实现类用于实例化ViewModels
     */
    public interface Factory {
        @NonNull
        <T extends ViewModel> T create(@NonNull Class<T> modelClass);
    }


    private final Factory mFactory;
    private final ViewModelStore mViewModelStore;

    /**
     * 构造方法
     */
    public ViewModelProvider(@NonNull ViewModelStoreOwner owner, @NonNull Factory factory) {
        this(owner.getViewModelStore(), factory);
    }
    public ViewModelProvider(@NonNull ViewModelStore store, @NonNull Factory factory) {
        mFactory = factory;
        this.mViewModelStore = store;
    }


    /**
     * 获取ViewModel
     */
    @NonNull
    @MainThread
    public <T extends ViewModel> T get(@NonNull Class<T> modelClass) {
        //获取标准类名
        String canonicalName = modelClass.getCanonicalName();
        if (canonicalName == null) {
            throw new IllegalArgumentException("Local and anonymous classes can not be ViewModels");
        }
        return get(DEFAULT_KEY + ":" + canonicalName, modelClass);
    }
    @NonNull
    @MainThread
    public <T extends ViewModel> T get(@NonNull String key, @NonNull Class<T> modelClass) {
        //获取ViewModel
        ViewModel viewModel = mViewModelStore.get(key);
        if (modelClass.isInstance(viewModel)) {
            return (T) viewModel;
        } else {
            if (viewModel != null) {
            }
        }
        //创建新的ViewModel并保存
        viewModel = mFactory.create(modelClass);
        mViewModelStore.put(key, viewModel);
        return (T) viewModel;
    }


    /**
     * 创建ViewModel的工具类
     */
    public static class NewInstanceFactory implements Factory {
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            try {
                return modelClass.newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException("Cannot create an instance of " + modelClass, e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot create an instance of " + modelClass, e);
            }
        }
    }


    /**
     * 创建{@link AndroidViewModel} and{@link ViewModel}的工厂类
     */
    public static class AndroidViewModelFactory extends ViewModelProvider.NewInstanceFactory {
        private static AndroidViewModelFactory sInstance;
        /**
         * 注册单例
         */
        @NonNull
        public static AndroidViewModelFactory getInstance(@NonNull Application application) {
            if (sInstance == null) {
                sInstance = new AndroidViewModelFactory(application);
            }
            return sInstance;
        }
        private Application mApplication;
        /**
         * Creates a {@code AndroidViewModelFactory}
         * @param application an application to pass in {@link AndroidViewModel}
         */
        public AndroidViewModelFactory(@NonNull Application application) {
            mApplication = application;
        }
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (AndroidViewModel.class.isAssignableFrom(modelClass)) {
                try {
                    return modelClass.getConstructor(Application.class).newInstance(mApplication);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("Cannot create an instance of " + modelClass, e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Cannot create an instance of " + modelClass, e);
                } catch (InstantiationException e) {
                    throw new RuntimeException("Cannot create an instance of " + modelClass, e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException("Cannot create an instance of " + modelClass, e);
                }
            }
            return super.create(modelClass);
        }
    }

}
