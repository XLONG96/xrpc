package com.xlong.xrpc.client;

import java.net.InetSocketAddress;

public interface Client {
    void connect(InetSocketAddress inetSocketAddress);

    void stop();
}
