package com.me94me.example_retrofit.retrofit2;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import androidx.annotation.Nullable;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

import static com.me94me.example_retrofit.retrofit2.Utils.checkNotNull;
import static java.util.Collections.unmodifiableList;

/**
 * Retrofit 2.4.0
 * <p>
 * 敬
 *
 * @author Bob Lee (bob@squareup.com)
 * @author Jake Wharton (jw@squareup.com)
 */
public final class Retrofit {

    /**
     * 保存网络请求配置（请求接口中的注解进行解析后得到的对象）
     */
    private final Map<Method, ServiceMethod<?, ?>> serviceMethodCache = new ConcurrentHashMap<>();
    /**
     * 请求的Url地址
     */
    final HttpUrl baseUrl;
    /**
     * 网络请求工厂
     * 用于创建一个请求供OkHttpClient发送请求 默认为OkHttpCall
     */
    final okhttp3.Call.Factory callFactory;
    /**
     * Android下默认ExecutorCallAdapterFactory
     * 请求适配工厂集合
     * 适配请求回来的数据,见CallAdapter
     */
    final List<CallAdapter.Factory> callAdapterFactories;
    /**
     * Android下默认为BuiltInConverters
     * 数据转换器工厂集合
     */
    final List<Converter.Factory> converterFactories;
    /**
     * 回调方法执行器,  Android下默认为MainThreadExecutor
     */
    final @Nullable
    Executor callbackExecutor;
    /**
     * 当retrofit实例调用create()方法时是否立即验证所有接口的配置
     */
    final boolean validateEagerly;

    Retrofit(okhttp3.Call.Factory callFactory, HttpUrl baseUrl,
             List<Converter.Factory> converterFactories, List<CallAdapter.Factory> callAdapterFactories,
             @Nullable Executor callbackExecutor, boolean validateEagerly) {
        this.callFactory = callFactory;
        this.baseUrl = baseUrl;
        this.converterFactories = converterFactories;
        this.callAdapterFactories = callAdapterFactories;
        this.callbackExecutor = callbackExecutor;
        this.validateEagerly = validateEagerly;
    }

    /********************************  获取 ******************************************/

    public okhttp3.Call.Factory callFactory() {
        return callFactory;
    }

    public HttpUrl baseUrl() {
        return baseUrl;
    }

    public List<CallAdapter.Factory> callAdapterFactories() {
        return callAdapterFactories;
    }

    public List<Converter.Factory> converterFactories() {
        return converterFactories;
    }

    public @Nullable
    Executor callbackExecutor() {
        return callbackExecutor;
    }


    /********************************  对外的方法 ******************************************/

    /**
     * 外观模式、代理模式
     */
    @SuppressWarnings("unchecked")
    public <T> T create(final Class<T> service) {
        //接口验证
        Utils.validateServiceInterface(service);
        //是否提前验证
        if (validateEagerly) {
            // 给接口中每个方法的注解进行解析并得到一个ServiceMethod对象，以Method为键将该对象存入LinkedHashMap集合中
            // 如果不是提前验证则进行动态解析对应方法得到一个ServiceMethod对象，最后存入到LinkedHashMap集合中，类似延迟加载（默认）
            eagerlyValidateMethods(service);
        }
        //通过动态代理创建接口的实例，为了获取接口方法上的所有注解
        return (T) Proxy.newProxyInstance(
                service.getClassLoader(),//动态生成接口实现类
                new Class<?>[]{service},//动态创建实例
                //代理类的具体实现交给InvocationHandler处理
                new InvocationHandler() {
                    private final Platform platform = Platform.get();

                    //接口方法的调用也即通过调用InvocationHandler对象的invoke（）来完成指定的功能

                    /**
                     * 所有接口方法的调用都会集中到这里进行处理
                     * @param proxy 接口对象
                     * @param method 调用的接口方法
                     * @param args 接口方法参数
                     */
                    @Override
                    public Object invoke(Object proxy, Method method, @Nullable Object[] args)
                            throws Throwable {
                        //如果获取声明的类等于代理类
                        if (method.getDeclaringClass() == Object.class) {
                            return method.invoke(this, args);
                        }
                        //Android平台下不处理
                        if (platform.isDefaultMethod(method)) {
                            return platform.invokeDefaultMethod(method, service, proxy, args);
                        }
                        //一个ServiceMethod对应一个接口方法
                        ServiceMethod<Object, Object> serviceMethod = (ServiceMethod<Object, Object>) loadServiceMethod(method);
                        //创建OkHttpCall对象
                        OkHttpCall<Object> okHttpCall = new OkHttpCall<>(serviceMethod, args);
                        //调用callAdapter进行请求适配,如Call<R>到Observable<R>
                        return serviceMethod.adapt(okHttpCall);
                    }
                });
    }

    /**
     * 加载serviceMethod并以method为键ServiceMethod为值缓存到serviceMethodCache
     */
    private void eagerlyValidateMethods(Class<?> service) {
        Platform platform = Platform.get();
        for (Method method : service.getDeclaredMethods()) {
            if (!platform.isDefaultMethod(method)) {
                loadServiceMethod(method);
            }
        }
    }

    /**
     * 获取ServiceMethod
     */
    ServiceMethod<?, ?> loadServiceMethod(Method method) {
        //从缓存中取,若存在直接返回
        ServiceMethod<?, ?> result = serviceMethodCache.get(method);
        if (result != null) return result;
        //线程同步
        synchronized (serviceMethodCache) {
            result = serviceMethodCache.get(method);
            if (result == null) {
                //构造模式新建ServiceMethod并加入缓存
                result = new ServiceMethod.Builder<>(this, method).build();
                serviceMethodCache.put(method, result);
            }
        }
        return result;
    }


    public CallAdapter<?, ?> callAdapter(Type returnType, Annotation[] annotations) {
        return nextCallAdapter(null, returnType, annotations);
    }

    public CallAdapter<?, ?> nextCallAdapter(@Nullable CallAdapter.Factory skipPast, Type returnType, Annotation[] annotations) {
        checkNotNull(returnType, "returnType == null");
        checkNotNull(annotations, "annotations == null");
        int start = callAdapterFactories.indexOf(skipPast) + 1;
        //寻找适合的CallAdapter,未找到则抛出异常
        for (int i = start, count = callAdapterFactories.size(); i < count; i++) {
            CallAdapter<?, ?> adapter = callAdapterFactories.get(i).get(returnType, annotations, this);
            if (adapter != null) {
                return adapter;
            }
        }
        StringBuilder builder = new StringBuilder("Could not locate call adapter for ")
                .append(returnType)
                .append(".\n");
        if (skipPast != null) {
            builder.append("  Skipped:");
            for (int i = 0; i < start; i++) {
                builder.append("\n   * ").append(callAdapterFactories.get(i).getClass().getName());
            }
            builder.append('\n');
        }
        builder.append("  Tried:");
        for (int i = start, count = callAdapterFactories.size(); i < count; i++) {
            builder.append("\n   * ").append(callAdapterFactories.get(i).getClass().getName());
        }
        throw new IllegalArgumentException(builder.toString());
    }

    public <T> Converter<T, RequestBody> requestBodyConverter(Type type,
                                                              Annotation[] parameterAnnotations, Annotation[] methodAnnotations) {
        return nextRequestBodyConverter(null, type, parameterAnnotations, methodAnnotations);
    }

    public <T> Converter<T, RequestBody> nextRequestBodyConverter(
            @Nullable Converter.Factory skipPast, Type type, Annotation[] parameterAnnotations,
            Annotation[] methodAnnotations) {
        checkNotNull(type, "type == null");
        checkNotNull(parameterAnnotations, "parameterAnnotations == null");
        checkNotNull(methodAnnotations, "methodAnnotations == null");

        int start = converterFactories.indexOf(skipPast) + 1;
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            Converter.Factory factory = converterFactories.get(i);
            Converter<?, RequestBody> converter =
                    factory.requestBodyConverter(type, parameterAnnotations, methodAnnotations, this);
            if (converter != null) {
                //noinspection unchecked
                return (Converter<T, RequestBody>) converter;
            }
        }
        StringBuilder builder = new StringBuilder("Could not locate RequestBody converter for ")
                .append(type)
                .append(".\n");
        if (skipPast != null) {
            builder.append("  Skipped:");
            for (int i = 0; i < start; i++) {
                builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
            }
            builder.append('\n');
        }
        builder.append("  Tried:");
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
        }
        throw new IllegalArgumentException(builder.toString());
    }

    /**
     * 获取Response的数据转换器
     */
    public <T> Converter<ResponseBody, T> responseBodyConverter(Type type, Annotation[] annotations) {
        return nextResponseBodyConverter(null, type, annotations);
    }

    public <T> Converter<ResponseBody, T> nextResponseBodyConverter(
            @Nullable Converter.Factory skipPast, Type type, Annotation[] annotations) {
        checkNotNull(type, "type == null");
        checkNotNull(annotations, "annotations == null");

        int start = converterFactories.indexOf(skipPast) + 1;
        //获取合适的Response数据转化器
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            Converter<ResponseBody, ?> converter =
                    converterFactories.get(i).responseBodyConverter(type, annotations, this);
            if (converter != null) {
                //noinspection unchecked
                return (Converter<ResponseBody, T>) converter;
            }
        }
        StringBuilder builder = new StringBuilder("Could not locate ResponseBody converter for ")
                .append(type)
                .append(".\n");
        if (skipPast != null) {
            builder.append("  Skipped:");
            for (int i = 0; i < start; i++) {
                builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
            }
            builder.append('\n');
        }
        builder.append("  Tried:");
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
        }
        throw new IllegalArgumentException(builder.toString());
    }

    public <T> Converter<T, String> stringConverter(Type type, Annotation[] annotations) {
        checkNotNull(type, "type == null");
        checkNotNull(annotations, "annotations == null");

        for (int i = 0, count = converterFactories.size(); i < count; i++) {
            Converter<?, String> converter =
                    converterFactories.get(i).stringConverter(type, annotations, this);
            if (converter != null) {
                //noinspection unchecked
                return (Converter<T, String>) converter;
            }
        }
        // Nothing matched. Resort to default converter which just calls toString().
        //noinspection unchecked
        return (Converter<T, String>) BuiltInConverters.ToStringConverter.INSTANCE;
    }


    /***********************  Builder ************************/

    public Builder newBuilder() {
        return new Builder(this);
    }

    public static final class Builder {
        private final Platform platform;
        private @Nullable
        okhttp3.Call.Factory callFactory;
        private HttpUrl baseUrl;
        private final List<Converter.Factory> converterFactories = new ArrayList<>();
        private final List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>();
        private @Nullable
        Executor callbackExecutor;
        private boolean validateEagerly;

        //看代码是不能用的Platform不能访问的
        Builder(Platform platform) {
            this.platform = platform;
        }

        public Builder() {
            this(Platform.get());
        }

        Builder(Retrofit retrofit) {
            platform = Platform.get();
            callFactory = retrofit.callFactory;
            baseUrl = retrofit.baseUrl;
            //移除第一个默认的那个BuiltInConverters(),因为后面构造构造Retrofit实例会再加一个
            converterFactories.addAll(retrofit.converterFactories);
            converterFactories.remove(0);
            //移除最后一个,默认的那个，因为最后构造Retrofit实例会再加一个
            callAdapterFactories.addAll(retrofit.callAdapterFactories);
            callAdapterFactories.remove(callAdapterFactories.size() - 1);
            callbackExecutor = retrofit.callbackExecutor;
            validateEagerly = retrofit.validateEagerly;
        }
        /***********************  构建实例参数  ************************/

        /**
         * OkHttpClient实现了okHttp3.call.factory
         */
        public Builder client(OkHttpClient client) {
            return callFactory(checkNotNull(client, "client == null"));
        }

        public Builder callFactory(okhttp3.Call.Factory factory) {
            this.callFactory = checkNotNull(factory, "factory == null");
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            checkNotNull(baseUrl, "baseUrl == null");
            HttpUrl httpUrl = HttpUrl.parse(baseUrl);
            if (httpUrl == null) {
                throw new IllegalArgumentException("Illegal URL: " + baseUrl);
            }
            return baseUrl(httpUrl);
        }

        public Builder baseUrl(HttpUrl baseUrl) {
            checkNotNull(baseUrl, "baseUrl == null");
            List<String> pathSegments = baseUrl.pathSegments();
            //BaseUrl必须以/结尾
            if (!"".equals(pathSegments.get(pathSegments.size() - 1))) {
                throw new IllegalArgumentException("baseUrl must end in /: " + baseUrl);
            }
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder addConverterFactory(Converter.Factory factory) {
            converterFactories.add(checkNotNull(factory, "factory == null"));
            return this;
        }

        public Builder addCallAdapterFactory(CallAdapter.Factory factory) {
            callAdapterFactories.add(checkNotNull(factory, "factory == null"));
            return this;
        }

        public Builder callbackExecutor(Executor executor) {
            this.callbackExecutor = checkNotNull(executor, "executor == null");
            return this;
        }

        public List<CallAdapter.Factory> callAdapterFactories() {
            return this.callAdapterFactories;
        }

        public List<Converter.Factory> converterFactories() {
            return this.converterFactories;
        }

        public Builder validateEagerly(boolean validateEagerly) {
            this.validateEagerly = validateEagerly;
            return this;
        }

        /***********************  构建Retrofit实例  ************************/
        public Retrofit build() {
            //检验baseUrl
            if (baseUrl == null) {
                throw new IllegalStateException("Base URL required.");
            }
            okhttp3.Call.Factory callFactory = this.callFactory;
            //若为null新建一个，默认采用OkHttpClient
            if (callFactory == null) {
                //默认使用OkHttp
                callFactory = new OkHttpClient();
            }
            //请求回调若为null默认一个，Android平台下默认为ExecutorCallAdapterFactory
            Executor callbackExecutor = this.callbackExecutor;
            if (callbackExecutor == null) {
                //Android平台下为MainThreadExecutor()
                callbackExecutor = platform.defaultCallbackExecutor();
            }
            //new ArrayList()防止传入list不能增删改查
            //callAdapterFactory集合，添加默认的callAdapterFactory（@{DefaultCallAdapterFactory}）并传入回调执行器
            List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>(this.callAdapterFactories);
            //Android下默认为ExecutorCallAdapterFactory
            callAdapterFactories.add(platform.defaultCallAdapterFactory(callbackExecutor));

            //数据转换器,GsonConverterFactory
            List<Converter.Factory> converterFactories = new ArrayList<>(1 + this.converterFactories.size());
            //首页添加内置的转换器确保正确的行为
            converterFactories.add(new BuiltInConverters());
            converterFactories.addAll(this.converterFactories);

            //unmodifiableList不能修改的list
            return new Retrofit(callFactory, baseUrl, unmodifiableList(converterFactories),
                    unmodifiableList(callAdapterFactories), callbackExecutor, validateEagerly);
        }
    }
}
