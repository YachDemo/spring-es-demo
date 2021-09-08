package com.example.es.exception;

/**
 * @author YanCh
 * @version v1.0
 * Create by 2020-10-22 9:42
 **/
public class ElasticException extends BaseException {

    public ElasticException(String message, String code, Throwable cause) {
        super(message, code, cause);
    }

    public ElasticException(String message) {
        super(message);
    }

    public ElasticException(String message, Throwable cause) {
        super(message, cause);
    }

    public ElasticException(Throwable cause) {
        super(cause);
    }
}
