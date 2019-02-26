package com.xlong.xrpc.client;

import com.xlong.xrpc.protocol.RPCRequest;
import com.xlong.xrpc.protocol.RPCResponse;
import com.xlong.xrpc.protocol.XProtobufDecoder;
import com.xlong.xrpc.protocol.XProtobufEncoder;
import com.xlong.xrpc.registry.ServiceDiscovery;
import com.xlong.xrpc.util.NetworkUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractClient implements Client {
    private static final Logger logger = LoggerFactory.getLogger(AbstractClient.class);

    private static final int MAX_CONNECT_TIMEOUT = 6000;

    private ServiceDiscovery serviceDiscovery;

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    private CopyOnWriteArrayList<XClientHandler> connectedHandlers = new CopyOnWriteArrayList<>();
    private Map<InetSocketAddress, XClientHandler> connectedServerNodes = new ConcurrentHashMap<>();

    private ReentrantLock lock = new ReentrantLock();
    private Condition connected = lock.newCondition();
    private AtomicInteger roundRobin = new AtomicInteger();


    public AbstractClient(String host, int port) {
        connect(new InetSocketAddress(host, port));
    }

    public AbstractClient(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
        serviceDiscovery.setAbstractXClient(this);
        try {
            serviceDiscovery.init(NetworkUtils.getHostname());
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    private void checkConnectedServer() {
        int activeCount = 0;
        // 移除失活连接
        for (Iterator<XClientHandler> it = connectedHandlers.iterator(); it.hasNext(); ) {
            XClientHandler handler = it.next();
            if (!handler.getChannel().isActive()) {
                logger.info("remove one dead connect");
                it.remove();
                connectedServerNodes.remove(handler.getChannel().remoteAddress());
            } else {
                activeCount += 1;
            }
        }
        logger.info("Check Completed! That has {} active connect", activeCount);
    }

    public void updateServerConnect(List<String> serverAddrList) {
        checkConnectedServer();

        if (serverAddrList.size() != 0) {
            // 连接并添加本地不存在的服务连接
            for (String remoteAddr :serverAddrList) {
                String[] temp = remoteAddr.split(":");
                if (temp.length == 2) {
                    String host = temp[0];
                    int port = Integer.parseInt(temp[1]);
                    InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
                    if (!connectedServerNodes.keySet().contains(inetSocketAddress)) {
                        connect(inetSocketAddress);
                    }
                }
            }
        }
    }

    @Override
    public void connect(final InetSocketAddress serverAddress) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .remoteAddress(serverAddress)
                .handler(new ChannelInitializer<SocketChannel>() {
                             @Override
                             protected void initChannel(SocketChannel channel) throws Exception {
                                 channel.pipeline()
                                         .addLast(new LengthFieldBasedFrameDecoder(64 * 1024, 0, 4))
                                         .addLast(new XProtobufEncoder(RPCRequest.class))
                                         .addLast(new XProtobufDecoder(RPCResponse.class))
                                         .addLast(new XClientHandler());
                             }
                         }
                );
        logger.info("Start connect server...");
        ChannelFuture channelFuture = null;
        try {
            channelFuture = bootstrap.connect().sync();
        } catch (InterruptedException e) {
            logger.error("Client's connecting has being interrupted");
        } catch (Exception e) {
            logger.error("{}", e);
        }
        // 客户端连接成功后保存持久连接的handler，然后在该zookeeper的相应服务下新建结点
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(final ChannelFuture channelFuture) throws Exception {
                if (channelFuture.isSuccess()) {
                    logger.info("Successfully connect to remote server. remote peer = {}", serverAddress);
                    XClientHandler handler = channelFuture.channel().pipeline().get(XClientHandler.class);
                    addHandler(handler);
                }
            }
        });
    }

    public void addHandler(XClientHandler xClientHandler) {
        logger.info("Add a connected handler");
        connectedHandlers.add(xClientHandler);
        InetSocketAddress inetSocketAddress = (InetSocketAddress) xClientHandler.getChannel()
                                                    .remoteAddress();
        connectedServerNodes.put(inetSocketAddress, xClientHandler);
        signalAllWaiters();
    }

    private void signalAllWaiters() {
        lock.lock();
        try {
            connected.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private boolean waitForHandler() throws InterruptedException {
        lock.lock();
        try {
            return connected.await(MAX_CONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
        } finally {
            lock.unlock();
        }
    }

    public XClientHandler getHandler() {
        // 检查连接活性
        checkConnectedServer();

        int size = connectedHandlers.size();
        // 没有可用连接则阻塞一定时间
        if (size == 0) {
            try {
                boolean available = waitForHandler();
                if (available) {
                    size = connectedHandlers.size();
                } else {
                    return null;
                }
            } catch (InterruptedException e) {
                logger.error("Waiting for available node is interrupted! ", e);
                throw new RuntimeException("Can't connect any servers!", e);
            }
        }

        // round robin
        int index = (roundRobin.getAndAdd(1) + size) % size;
        return connectedHandlers.get(index);
    }

    @Override
    public void stop() {
        // 关闭所有连接
        for (XClientHandler handler : connectedHandlers) {
            if (handler.getChannel().isActive()) {
                handler.getChannel().close();
            }
        }
        signalAllWaiters();
        eventLoopGroup.shutdownGracefully();
        if (serviceDiscovery != null) {
            serviceDiscovery.stop();
        }
    }
}
