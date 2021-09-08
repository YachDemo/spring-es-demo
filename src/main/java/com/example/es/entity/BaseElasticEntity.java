package com.example.es.entity;

/**
 * Elastic基类
 *
 * @author YanCh
 * Create by 2020-03-05 10:13
 **/
public class BaseElasticEntity {
    /**
     * id
     */
    protected String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BaseElasticEntity(String id) {
        this.id = id;
    }

    public BaseElasticEntity() {
    }
}
