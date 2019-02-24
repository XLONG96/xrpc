package com.xlong.xrpc.server;

import com.xlong.xrpc.protocol.RPCRequest;
import com.xlong.xrpc.protocol.RPCResponse;
import com.xlong.xrpc.protocol.XProtobufDecoder;
import com.xlong.xrpc.protocol.XProtobufEncoder;
import com.xlong.xrpc.registry.ServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractXServer implements XServer {
    private final Logger logger = LoggerFactory.getLogger(AbstractXServer.class);

    private String host;
    private int port;
    private ServiceRegistry serviceRegistry;
    private InetSocketAddress inetSocketAddress;

    private Map<String, Object> handlerMapper = new HashMap<String, Object>();

    private EventLoopGroup bossGroup = null;
    private EventLoopGroup workerGroup = null;
    private Class<? extends ServerChannel> channel;

    public AbstractXServer(int port) {
        this(port, new NioEventLoopGroup(), new NioEventLoopGroup(),
                NioServerSocketChannel.class);
    }

    public AbstractXServer(int port, EventLoopGroup bossGroup, EventLoopGroup workerGroup,
                           Class<? extends ServerChannel> channel) {
        this.port = port;
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        this.channel = channel;
        initAddress();
    }

    public AbstractXServer(String host, int port, ServiceRegistry serviceRegistry) {
        this(host, port, serviceRegistry, new NioEventLoopGroup(), new NioEventLoopGroup(),
                NioServerSocketChannel.class);
    }

    public AbstractXServer(String host, int port, ServiceRegistry serviceRegistry,
                           EventLoopGroup bossGroup, EventLoopGroup workerGroup,
                           Class<? extends ServerChannel> channel) {
        this.host = host;
        this.port = port;
        this.serviceRegistry = serviceRegistry;
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        this.channel = channel;
        initAddress();
    }

    private void initAddress() {
        if (host == null) { // only port
            inetSocketAddress = new InetSocketAddress("localhost", port);
        } else {
            inetSocketAddress = new InetSocketAddress(host, port);
        }
    }

    public void start() throws InterruptedException {
        logger.info("Server startup...");
        if (bossGroup != null && workerGroup != null) {
            logger.info("bootstrap setup...");
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(channel)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketchannel) throws Exception {
                            socketchannel.pipeline()
                                    .addLast(new LengthFieldBasedFrameDecoder(64 * 1024, 0, 4))
                                    .addLast(new XProtobufDecoder(RPCRequest.class))
                                    .addLast(new XProtobufEncoder(RPCResponse.class))
                                    .addLast(new XProcessor(handlerMapper));

                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind().sync();
            f.channel().closeFuture().sync();

            if (serviceRegistry != null) {
                logger.info("Server is listening in {}:{}", host, port);
                serviceRegistry.register(host + ":" + port);
            } else {
                logger.info("Server is listening in {}:{}", inetSocketAddress.getAddress(), inetSocketAddress.getPort());
            }
        }
    }

    public void stop() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }

        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }

    public XServer addService(String interfaceName, Object serviceBean) {
        if (!handlerMapper.containsKey(interfaceName)) {
            logger.info("Loading service: {}", interfaceName);
            this.handlerMapper.put(interfaceName, serviceBean);
        }
        return this;
    }
}
