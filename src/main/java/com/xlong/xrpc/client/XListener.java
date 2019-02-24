package com.xlong.xrpc.client;

public interface XListener {
    void success(Object result);

    void fail(Exception e);
}
