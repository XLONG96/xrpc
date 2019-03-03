package com.xlong.xrpc.server;

import com.xlong.xrpc.entity.Request;
import com.xlong.xrpc.protocol.RPCRequest;
import com.xlong.xrpc.protocol.RPCResponse;
import com.xlong.xrpc.repository.JpaManager;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;

public class XProcessor extends SimpleChannelInboundHandler<RPCRequest> {
    private static final Logger logger = LoggerFactory.getLogger(XProcessor.class);

    private final Map<String, Object> handlerMapper;
    private JpaManager jpaManager;

    public XProcessor(Map<String, Object> handlerMapper, JpaManager jpaManager) {
        this.handlerMapper = handlerMapper;
        this.jpaManager = jpaManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, final RPCRequest rpcRequest) throws Exception {
        logger.debug("Receive request " + rpcRequest.getRequestId());
        RPCResponse rpcResponse = new RPCResponse();
        rpcResponse.setRequestId(rpcRequest.getRequestId());
        String host = ctx.channel().localAddress().toString();
        //logger.info(rpcRequest.toString());
        try {
            Object result = handle(rpcRequest);
            rpcResponse.setResult(result);
        } catch (Throwable t) {
            rpcResponse.setError(t.toString());
            logger.error("RPC Server handle request error",t);
        }
        collection(host, rpcRequest);
        ctx.writeAndFlush(rpcResponse).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                logger.debug("Send response for request " + rpcRequest.getRequestId());
            }
        });
    }

    private void collection(String host, RPCRequest rpcRequest) {
        Request request = new Request();
        request.setRequestId(rpcRequest.getRequestId());
        request.setRequestClassName(rpcRequest.getClassName());
        request.setRequestMethodName(rpcRequest.getMethodName());
        request.setRequestHost(host);
        request.setRequestTime(new Date());

        Class<?>[] parameterTypes = rpcRequest.getParameterTypes();
        Object[] parameters = rpcRequest.getParameters();
        if (parameters != null) {
            int size = parameters.length;
            StringBuilder sb = new StringBuilder();
            String requestParams = "";
            for (int i = 0; i < size; i++) {
                String pt = parameterTypes[i].toString();
                String p = parameters[i].toString();
                sb.append("[");
                sb.append(pt);
                sb.append(":");
                sb.append(p);
                sb.append("]");
            }
            request.setRequestParams(sb.toString());
        }
        logger.info("{}", request);
        jpaManager.saveRequest(request);
    }

    private Object handle(RPCRequest request) throws Throwable {
        logger.info(request.toString());
        String className = request.getClassName();
        Object serviceBean = handlerMapper.get(className);

        if (serviceBean == null) {
            throw new Exception("No server provide the method '" + className + "'");
        } else {
            Class<?> serviceClass = serviceBean.getClass();
            String methodName = request.getMethodName();
            Class<?>[] parameterTypes = request.getParameterTypes();
            Object[] parameters = request.getParameters();

            // JDK reflect
            /*Method method = serviceClass.getMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method.invoke(serviceBean, parameters);*/

            // Cglib reflect
            FastClass serviceFastClass = FastClass.create(serviceClass);
            FastMethod serviceFastMethod = serviceFastClass.getMethod(methodName, parameterTypes);
            return serviceFastMethod.invoke(serviceBean, parameters);
        }
    }

}
