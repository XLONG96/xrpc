package com.xlong.xrpc.server;

import com.xlong.xrpc.registry.ServiceRegistry;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.oio.OioServerSocketChannel;

public class XOioServer extends AbstractXServer {
    public XOioServer(int port) {
        super(port, new OioEventLoopGroup(), new OioEventLoopGroup(), OioServerSocketChannel.class);
    }

    public XOioServer(String host, int port, ServiceRegistry serviceRegistry) {
        super(host, port, serviceRegistry, new OioEventLoopGroup(),
                new OioEventLoopGroup(), OioServerSocketChannel.class);
    }
}
