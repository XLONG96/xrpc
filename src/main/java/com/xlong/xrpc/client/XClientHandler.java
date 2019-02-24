package com.xlong.xrpc.client;

import com.xlong.xrpc.protocol.RPCRequest;
import com.xlong.xrpc.protocol.RPCResponse;
import io.netty.channel.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class XClientHandler extends SimpleChannelInboundHandler<RPCResponse> {

    private Channel channel;

    private ConcurrentHashMap<String, XClientFuture> futureMap = new ConcurrentHashMap<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.channel = ctx.channel();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RPCResponse rpcResponse) throws Exception {
        String requestId = rpcResponse.getRequestId();
        XClientFuture future = futureMap.get(requestId);

        if (future != null) {
            future.done(rpcResponse);
            futureMap.remove(requestId);
        }
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
