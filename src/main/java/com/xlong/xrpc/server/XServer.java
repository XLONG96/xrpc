package com.xlong.xrpc.server;

public interface XServer {
    void start() throws InterruptedException;

    void stop();

    XServer addService(String interfaceName, Object serviceBean);
}
