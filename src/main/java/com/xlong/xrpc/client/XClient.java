package com.xlong.xrpc.client;

import com.xlong.xrpc.protocol.RPCRequest;
import com.xlong.xrpc.registry.ServiceDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

public class XClient extends AbstractClient {
    private static final Logger logger = LoggerFactory.getLogger(XClient.class);


    public XClient(String host, int port) {
        super(host, port);
    }

    public XClient(ServiceDiscovery serviceDiscovery) {
        super(serviceDiscovery);
    }

    @SuppressWarnings("unchecked")
    public <T> T create(Class<?> interfaceClass) {
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(),
                new Class<?>[] {interfaceClass}, new XClientProxy());
    }

    private class XClientProxy implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            RPCRequest rpcRequest = new RPCRequest();
            rpcRequest.setRequestId(UUID.randomUUID().toString());
            rpcRequest.setClassName(method.getDeclaringClass().getName());
            rpcRequest.setMethodName(method.getName());
            rpcRequest.setParameterTypes(method.getParameterTypes());
            rpcRequest.setParameters(args);

            XClientHandler xClientHandler = getHandler();

            if (xClientHandler == null) {
                throw new Exception("No available client's connection");
            }

            logger.info(rpcRequest.toString());
            XClientFuture future = xClientHandler.sendRequest(rpcRequest, null);
            return future.get();
        }
    }
}
