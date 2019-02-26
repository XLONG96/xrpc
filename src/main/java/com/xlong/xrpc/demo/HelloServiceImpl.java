package com.xlong.xrpc.demo;

public class HelloServiceImpl implements HelloService {
    @Override
    public String sayHello() {
        return "Hello World!";
    }

    @Override
    public String sayWords(String words) {
        return words + "!";
    }
}
