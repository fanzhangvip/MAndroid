package com.example.example_aidl;
/*Book.aidl和Book.java需要同包名*/
parcelable  Book;
/**
* 找不到文件可以在Manifest中添加
*    sourceSets {
          main {
              java.srcDirs = ['src/main/java', 'src/main/aidl']
          }
      }
*/