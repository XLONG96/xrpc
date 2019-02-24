package com.xlong.xrpc.demo;

public class HelloServiceImpl implements HelloService {
    @Override
    public void sayHello() {
        System.out.println("Hello World!");
    }

    @Override
    public void sayWords(String words) {
        System.out.println(words);
    }
}
