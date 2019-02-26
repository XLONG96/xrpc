package com.xlong.xrpc.registry;

import com.xlong.xrpc.client.AbstractClient;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

public class ServiceDiscovery {
    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscovery.class);

    private CountDownLatch latch = new CountDownLatch(1);

    private volatile List<String> serverAddrList = new ArrayList<>();

    private String registryAddress;
    private ZooKeeper zookeeper;
    private AbstractClient abstractXClient;

    public ServiceDiscovery(String registryAddress) {
        this.registryAddress = registryAddress;
    }

    public void init(String data) {
        zookeeper = connectServer();
        if (zookeeper != null) {
            watchNode(zookeeper);
            addRootNode();
            createNode(data);
        }
    }

    public String discover() {
        String data = null;
        int size = serverAddrList.size();
        if (size > 0) {
            if (size == 1) {
                data = serverAddrList.get(0);
                logger.debug("using only data: {}", data);
            } else {
                data = serverAddrList.get(ThreadLocalRandom.current().nextInt(size));
                logger.debug("using random data: {}", data);
            }
        }
        return data;
    }

    private ZooKeeper connectServer() {
        ZooKeeper zk = null;
        try {
            zk = new ZooKeeper(registryAddress, ZKConstant.ZK_SESSION_TIMEOUT, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if (event.getState() == Event.KeeperState.SyncConnected) {
                        latch.countDown();
                    }
                }
            });
            latch.await();
        } catch (IOException | InterruptedException e) {
            logger.error("", e);
        }
        return zk;
    }

    private void watchNode(final ZooKeeper zk) {
        try {
            List<String> nodeList = zk.getChildren(ZKConstant.ZK_PROVIDER_ROOT, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if (event.getType() == Event.EventType.NodeChildrenChanged) {
                        watchNode(zk);
                    }
                }
            });
            // 获取所有提供RPC服务的地址
            List<String> serverAddrList = new ArrayList<>();
            for (String node : nodeList) {
                byte[] bytes = zk.getData(ZKConstant.ZK_PROVIDER_ROOT + "/" + node, false, null);
                serverAddrList.add(new String(bytes));
            }
            logger.debug("node data: {}", serverAddrList);
            this.serverAddrList = serverAddrList;

            logger.info("Service discovery modified.");
            abstractXClient.updateServerConnect(serverAddrList);
        } catch (KeeperException | InterruptedException e) {
            logger.error("", e);
        }
    }

    private void addRootNode(){
        try {
            Stat s = zookeeper.exists(ZKConstant.ZK_CONSUMER_ROOT, false);
            if (s == null) {
                zookeeper.create(ZKConstant.ZK_CONSUMER_ROOT, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT);
            }
        } catch (KeeperException e) {
            logger.error(e.toString());
        } catch (InterruptedException e) {
            logger.error(e.toString());
        }
    }

    private void createNode(String data) {
        try {
            byte[] bytes = data.getBytes();
            String path = zookeeper.create(ZKConstant.ZK_CONSUMER, bytes, ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL_SEQUENTIAL);
            logger.debug("create zookeeper node ({} => {})", path, data);
        } catch (KeeperException e) {
            logger.error("", e);
        }
        catch (InterruptedException ex){
            logger.error("", ex);
        }
    }

    public void setAbstractXClient(AbstractClient abstractXClient) {
        this.abstractXClient = abstractXClient;
    }

    public void stop(){
        if(zookeeper!=null){
            try {
                zookeeper.close();
            } catch (InterruptedException e) {
                logger.error("", e);
            }
        }
    }
}
