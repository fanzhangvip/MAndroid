```Java
// Add Dagger dependencies
dependencies {
  compile 'com.google.dagger:dagger:2.x'
  annotationProcessor 'com.google.dagger:dagger-compiler:2.x'
}
```

目前最新版为2.17

```Java
compile 'com.google.dagger:dagger-android:2.x'
compile 'com.google.dagger:dagger-android-support:2.x' // if you use the support libraries
annotationProcessor 'com.google.dagger:dagger-android-processor:2.x'
```

```Java
gradle.projectsEvaluated {
  tasks.withType(JavaCompile) {
    options.compilerArgs << "-Xmaxerrs" << "500" // or whatever number you want
  }
}
```

如果使用[Android Databinding library](https://developer.android.com/topic/libraries/data-binding/index.html)，可能需要增加`javac`打印的错误个数。Dagger打印错误信息时，编译绑定（binding compilation）会停止，有时候会打印超过100个错误，这是`javac`的默认设置。