package com.xlong.xrpc.util;

import java.net.InetAddress;

public class NetworkUtils {

    public static String getHostname() throws Exception {
        InetAddress ia = InetAddress.getLocalHost();
        String host = ia.getHostName(); //获取计算机主机名
        String IP= ia.getHostAddress(); //获取计算机IP
        return host;
    }
}
