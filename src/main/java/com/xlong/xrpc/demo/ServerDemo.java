package com.xlong.xrpc.demo;

import com.xlong.xrpc.registry.ServiceRegistry;
import com.xlong.xrpc.server.XNioServer;
import com.xlong.xrpc.server.XServer;

public class ServerDemo {
    public static void main(String[] args) throws InterruptedException {

        ServiceRegistry serviceRegistry = new ServiceRegistry("120.77.246.48:2181");
        XServer server = new XNioServer("localhost", 8888, serviceRegistry);
        server.addService(HelloService.class.getName(), new HelloServiceImpl());
        server.start();

        /*
        // local
        XServer server2 = new XNioServer(8888);
        server2.addService(HelloService.class.getName(), new HelloServiceImpl());
        server2.start();
        */
    }
}
