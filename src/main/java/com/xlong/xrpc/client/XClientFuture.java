package com.xlong.xrpc.client;

import com.xlong.xrpc.protocol.RPCRequest;
import com.xlong.xrpc.protocol.RPCResponse;
import com.xlong.xrpc.server.AbstractXServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

public class XClientFuture implements Future<Object> {
    private final Logger logger = LoggerFactory.getLogger(AbstractXServer.class);

    private List<XListener> xListenerList = new ArrayList<>();

    private RPCRequest rpcRequest;
    private RPCResponse rpcResponse;
    private Syn syn;
    private ReentrantLock lock = new ReentrantLock();

    public XClientFuture(RPCRequest rpcRequest) {
        this.rpcRequest = rpcRequest;
        this.syn = new Syn();
    }

    public void addListener(XListener xListener) {
        lock.lock();
        try {
            if (isDone()) {
                runListener(xListener);
            } else {
                xListenerList.add(xListener);
            }
        } finally {
            lock.unlock();
        }
    }

    public void addListeners(List<XListener> xListeners) {
        lock.lock();
        try {
            for (XListener listener : xListeners) {
                addListener(listener);
            }
        } finally {
            lock.unlock();
        }
    }

    public void done(RPCResponse rpcResponse) {
        this.rpcResponse = rpcResponse;
        // callback
        invokeListener();
        // 解除阻塞
        syn.release(1);
    }

    private void invokeListener() {
        lock.lock();
        try {
            for (XListener listener : xListenerList) {
                runListener(listener);
            }
        } finally {
            lock.unlock();
        }
    }

    private void runListener(XListener listener) {
        if (rpcResponse.getResult() != null) {
            listener.success(rpcResponse.getResult());
        } else {
            listener.fail(new RuntimeException("Response error", new Throwable(rpcResponse.getError())));
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDone() {
        return syn.isDone();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        // 结果未出来就进行阻塞，加入阻塞队列
        syn.acquire(-1);
        if (rpcResponse != null) {
            logger.info(rpcResponse.toString());
            return rpcResponse.getResult();
        }

        return null;
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        // 结果未出来则阻塞一定时间
        boolean success = syn.tryAcquireNanos(-1, timeout);
        if (success) {
            return rpcResponse.getResult();
        } else {
            throw new RuntimeException("Timeout exception. Request id: " + this.rpcRequest.getRequestId()
                    + ". Request class name: " + this.rpcRequest.getClassName()
                    + ". Request method: " + this.rpcRequest.getMethodName());
        }
    }

    private static class Syn extends AbstractQueuedSynchronizer {

        private final int done = 1;
        private final int pending = 0;

        @Override
        protected boolean tryAcquire(int arg) {
            return getState() == done;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (getState() == pending) {
                if (compareAndSetState(pending, done)) {
                    return true;
                } else {
                    return false;
                }
            }

            return false;
        }

        public boolean isDone() {
            return getState() == done;
        }
    }
}
