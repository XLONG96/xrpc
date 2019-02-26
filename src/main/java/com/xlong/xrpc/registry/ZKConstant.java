package com.xlong.xrpc.registry;

/**
 * ZooKeeper constant
 *
 * @author huangyong
 */
public interface ZKConstant {

    int ZK_SESSION_TIMEOUT = 5000;

    String ZK_PROVIDER_ROOT = "/provider";
    String ZK_CONSUMER_ROOT = "/consumer";
    String ZK_PROVIDER = ZK_PROVIDER_ROOT + "/service";
    String ZK_CONSUMER = ZK_CONSUMER_ROOT + "/node";
}
