package com.xlong.xrpc.server;

import com.xlong.xrpc.registry.ServiceRegistry;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class XNioServer extends AbstractXServer {

    public XNioServer(int port) {
        super(port, new NioEventLoopGroup(), new NioEventLoopGroup(), NioServerSocketChannel.class);
    }

    public XNioServer(String host, int port, ServiceRegistry serviceRegistry) {
        super(host, port, serviceRegistry, new NioEventLoopGroup(),
                new NioEventLoopGroup(), NioServerSocketChannel.class);
    }

}
