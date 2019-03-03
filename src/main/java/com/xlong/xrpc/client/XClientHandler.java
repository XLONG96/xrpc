package com.xlong.xrpc.client;

import com.xlong.xrpc.entity.Response;
import com.xlong.xrpc.protocol.RPCRequest;
import com.xlong.xrpc.protocol.RPCResponse;
import com.xlong.xrpc.repository.JpaManager;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class XClientHandler extends SimpleChannelInboundHandler<RPCResponse> {
    private static final Logger logger = LoggerFactory.getLogger(XClientHandler.class);
    private Channel channel;
    private JpaManager jpaManager;

    private ConcurrentHashMap<String, XClientFuture> futureMap = new ConcurrentHashMap<>();

    public XClientHandler(JpaManager jpaManager) {
        this.jpaManager = jpaManager;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.channel = ctx.channel();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RPCResponse rpcResponse) throws Exception {
        String requestId = rpcResponse.getRequestId();
        XClientFuture future = futureMap.get(requestId);
        collection(rpcResponse);

        if (future != null) {
            future.done(rpcResponse);
            futureMap.remove(requestId);
        }
    }

    private void collection(RPCResponse rpcResponse) {
        Response response = new Response();
        response.setRequestId(rpcResponse.getRequestId());
        response.setResponseResult(rpcResponse.getResult().toString());
        response.setResponseError(rpcResponse.getError());
        response.setResponseTime(new Date());
        logger.info("{}", response);
        jpaManager.saveResponse(response);
    }

    public Channel getChannel() {
        return channel;
    }

    public XClientFuture sendRequest(RPCRequest rpcRequest, XListener xListener) {
        final CountDownLatch latch = new CountDownLatch(1);
        XClientFuture future = new XClientFuture(rpcRequest);
        if (xListener != null) {
            future.addListener(xListener);
        }
        futureMap.put(rpcRequest.getRequestId(), future);
        channel.writeAndFlush(rpcRequest).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return future;
    }
}
