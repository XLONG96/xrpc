package com.xlong.xrpc.repository;

import com.xlong.xrpc.entity.Response;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;


public class JpaManagerTest {

    @Test
    public void saveResponse() {
        JpaManager jpaManager = new JpaManager();
        Response response = new Response();
        response.setRequestId("123");
        response.setResponseError("null");
        String result = "Hello World!";
        Object o = result;
        response.setResponseResult(o.toString());
        response.setResponseTime(new Date());
        jpaManager.saveResponse(response);
        jpaManager.close();
    }
}