package com.xlong.xrpc.repository;

import com.xlong.xrpc.entity.Request;
import com.xlong.xrpc.entity.Response;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import java.util.Date;

public class JpaManager {
    private EntityManagerFactory entityManagerFactory;

    private EntityManager entityManager;

    private EntityTransaction transaction;

    public JpaManager() {
        init();
    }

    private void init() {
        entityManagerFactory = Persistence.createEntityManagerFactory("jpa");
        entityManager = entityManagerFactory.createEntityManager();
        transaction = entityManager.getTransaction();
    }

    public void saveRequest(Request request) {
        transaction.begin();
        entityManager.persist(request);
        transaction.commit();
    }

    public void saveResponse(Response response) {
        transaction.begin();
        entityManager.persist(response);
        transaction.commit();
    }

    public void close() {
        entityManager.close();
        entityManagerFactory.close();
    }
}
