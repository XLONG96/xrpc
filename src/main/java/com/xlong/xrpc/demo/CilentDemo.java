package com.xlong.xrpc.demo;

import com.xlong.xrpc.client.*;
import com.xlong.xrpc.registry.ServiceDiscovery;

import java.net.InetAddress;
import java.util.concurrent.ExecutionException;

public class CilentDemo {
    public static void main(String[] args) throws Exception {
        ServiceDiscovery serviceDiscovery = new ServiceDiscovery("120.77.246.48:2181");
        XClient client = new XClient(serviceDiscovery);
        HelloService helloService = client.create(HelloService.class);
        System.out.println(helloService.sayHello());
        System.out.println(helloService.sayWords("Hello Java"));
/*
        XAsyncClient xAsyncClient = new XAsyncClient(serviceDiscovery);
        XClientFuture future = xAsyncClient.call("sayWords", new Object[]{"Hello World"}, new XListener() {
            @Override
            public void success(Object result) {
                System.out.println("I'm done!");
            }

            @Override
            public void fail(Exception e) {
                System.out.println(e);
            }
        });
        System.out.println(future.get());


        // local
        XClient client2 = new XClient("localhost",8888);
        HelloService helloService2= client2.create(HelloService.class);
        helloService2.sayHello();
        helloService2.sayWords("Hello World");


        XAsyncClient xAsyncClient2 = new XAsyncClient("localhost", 8888);
        xAsyncClient2.createAsync(HelloService.class);
        XClientFuture future2 = xAsyncClient2.call("sayWords", new Object[]{"Hello World"}, new XListener() {
            @Override
            public void success(Object result) {
                System.out.println("I'm done!");
            }

            @Override
            public void fail(Exception e) {
                System.out.println(e);
            }
        });
        System.out.println(future2.get());
*/
    }
}
