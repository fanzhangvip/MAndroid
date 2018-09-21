package com.me94me.resource_lifecycle.lifecycle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal class to handle lifecycle conversion etc.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Lifecycling {

    private static final int REFLECTIVE_CALLBACK = 1;
    private static final int GENERATED_CALLBACK = 2;

    private static Map<Class, Integer> sCallbackCache = new HashMap<>();
    private static Map<Class, List<Constructor<? extends GeneratedAdapter>>> sClassToAdapters =
            new HashMap<>();

    @NonNull
    static GenericLifecycleObserver getCallback(Object object) {
        if (object instanceof FullLifecycleObserver) {
            return new FullLifecycleObserverAdapter((FullLifecycleObserver) object);
        }

        if (object instanceof GenericLifecycleObserver) {
            return (GenericLifecycleObserver) object;
        }

        final Class<?> klass = object.getClass();
        int type = getObserverConstructorType(klass);
        if (type == GENERATED_CALLBACK) {
            List<Constructor<? extends GeneratedAdapter>> constructors =
                    sClassToAdapters.get(klass);
            if (constructors.size() == 1) {
                GeneratedAdapter generatedAdapter = createGeneratedAdapter(
                        constructors.get(0), object);
                return new SingleGeneratedAdapterObserver(generatedAdapter);
            }
            GeneratedAdapter[] adapters = new GeneratedAdapter[constructors.size()];
            for (int i = 0; i < constructors.size(); i++) {
                adapters[i] = createGeneratedAdapter(constructors.get(i), object);
            }
            return new CompositeGeneratedAdaptersObserver(adapters);
        }
        return new ReflectiveGenericLifecycleObserver(object);
    }

    private static GeneratedAdapter createGeneratedAdapter(
            Constructor<? extends GeneratedAdapter> constructor, Object object) {
        //noinspection TryWithIdenticalCatches
        try {
            return constructor.newInstance(object);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    private static Constructor<? extends GeneratedAdapter> generatedConstructor(Class<?> klass) {
        try {
            Package aPackage = klass.getPackage();
            String name = klass.getCanonicalName();
            final String fullPackage = aPackage != null ? aPackage.getName() : "";
            final String adapterName = getAdapterName(fullPackage.isEmpty() ? name :
                    name.substring(fullPackage.length() + 1));

            @SuppressWarnings("unchecked") final Class<? extends GeneratedAdapter> aClass =
                    (Class<? extends GeneratedAdapter>) Class.forName(
                            fullPackage.isEmpty() ? adapterName : fullPackage + "." + adapterName);
            Constructor<? extends GeneratedAdapter> constructor =
                    aClass.getDeclaredConstructor(klass);
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            return constructor;
        } catch (ClassNotFoundException e) {
            return null;
        } catch (NoSuchMethodException e) {
            // this should not happen
            throw new RuntimeException(e);
        }
    }

    private static int getObserverConstructorType(Class<?> klass) {
        if (sCallbackCache.containsKey(klass)) {
            return sCallbackCache.get(klass);
        }
        int type = resolveObserverCallbackType(klass);
        sCallbackCache.put(klass, type);
        return type;
    }

    private static int resolveObserverCallbackType(Class<?> klass) {
        // anonymous class bug:35073837
        if (klass.getCanonicalName() == null) {
            return REFLECTIVE_CALLBACK;
        }

        Constructor<? extends GeneratedAdapter> constructor = generatedConstructor(klass);
        if (constructor != null) {
            sClassToAdapters.put(klass, Collections
                    .<Constructor<? extends GeneratedAdapter>>singletonList(constructor));
            return GENERATED_CALLBACK;
        }

        boolean hasLifecycleMethods = ClassesInfoCache.sInstance.hasLifecycleMethods(klass);
        if (hasLifecycleMethods) {
            return REFLECTIVE_CALLBACK;
        }

        Class<?> superclass = klass.getSuperclass();
        List<Constructor<? extends GeneratedAdapter>> adapterConstructors = null;
        if (isLifecycleParent(superclass)) {
            if (getObserverConstructorType(superclass) == REFLECTIVE_CALLBACK) {
                return REFLECTIVE_CALLBACK;
            }
            adapterConstructors = new ArrayList<>(sClassToAdapters.get(superclass));
        }

        for (Class<?> intrface : klass.getInterfaces()) {
            if (!isLifecycleParent(intrface)) {
                continue;
            }
            if (getObserverConstructorType(intrface) == REFLECTIVE_CALLBACK) {
                return REFLECTIVE_CALLBACK;
            }
            if (adapterConstructors == null) {
                adapterConstructors = new ArrayList<>();
            }
            adapterConstructors.addAll(sClassToAdapters.get(intrface));
        }
        if (adapterConstructors != null) {
            sClassToAdapters.put(klass, adapterConstructors);
            return GENERATED_CALLBACK;
        }

        return REFLECTIVE_CALLBACK;
    }

    private static boolean isLifecycleParent(Class<?> klass) {
        return klass != null && LifecycleObserver.class.isAssignableFrom(klass);
    }

    /**
     * Create a name for an adapter class.
     */
    public static String getAdapterName(String className) {
        return className.replace(".", "_") + "_LifecycleAdapter";
    }

    private Lifecycling() {
    }
}
