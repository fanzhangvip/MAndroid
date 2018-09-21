package com.example.example_aidl;

import com.example.example_aidl.Book;

// Declare any non-default types here with import statements

interface BookService {
    /** 需要添加tag(in/out/inout,不然会报错)*/
    void addBook(in Book book);
    List<Book> getBooks();
}
