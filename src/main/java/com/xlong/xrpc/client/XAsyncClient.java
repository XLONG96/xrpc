package com.xlong.xrpc.client;

import com.xlong.xrpc.protocol.RPCRequest;
import com.xlong.xrpc.registry.ServiceDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class XAsyncClient extends AbstractClient {
    private static final Logger logger = LoggerFactory.getLogger(XAsyncClient.class);

    private Class<?> clazz;

    public XAsyncClient(String host, int port) {
        super(host, port);
    }

    public XAsyncClient(ServiceDiscovery serviceDiscovery) {
        super(serviceDiscovery);
    }

    @SuppressWarnings("unchecked")
    public void createAsync(Class<?> interfaceClass) {
       this.clazz = interfaceClass;
    }

    public XClientFuture call(String funcName, Object[] args) {
        return call(funcName, args, null);
    }

    public XClientFuture call(String funcName, Object[] args, XListener xListener) {
        XClientHandler handler = getHandler();
        RPCRequest request = createRequest(this.clazz.getName(), funcName, args);
        XClientFuture future = handler.sendRequest(request, xListener);
        return future;
    }


    private RPCRequest createRequest(String className, String methodName, Object[] args) {
        // 要是方法调用自定义类型？
        RPCRequest request = new RPCRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName(className);
        request.setMethodName(methodName);
        request.setParameters(args);

        Class[] parameterTypes = new Class[args.length];
        // Get the right class type
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = getClassType(args[i]);
        }
        request.setParameterTypes(parameterTypes);

        logger.info(request.toString());

        return request;
    }

    private Class<?> getClassType(Object obj) {
        Class<?> classType = obj.getClass();
        String typeName = classType.getName();
        switch (typeName) {
            case "java.lang.Integer":
                return Integer.TYPE;
            case "java.lang.Long":
                return Long.TYPE;
            case "java.lang.Float":
                return Float.TYPE;
            case "java.lang.Double":
                return Double.TYPE;
            case "java.lang.Character":
                return Character.TYPE;
            case "java.lang.Boolean":
                return Boolean.TYPE;
            case "java.lang.Short":
                return Short.TYPE;
            case "java.lang.Byte":
                return Byte.TYPE;
        }

        return classType;
    }
}
