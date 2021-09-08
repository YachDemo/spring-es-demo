package com.example.es.exception;

import lombok.Getter;

/**
 * 自定义异常基类
 *
 * @author YanCh
 * @version v1.0
 * Create by 2020-09-21 14:45
 **/
@Getter
public class BaseException extends RuntimeException {

    private String message;

    private String code;

    public BaseException(String message) {
        super(message);
        this.message = message;
    }

    public BaseException(String code, String msg) {
        super(msg);
        this.message = msg;
        this.code = code;
    }

    public BaseException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }

    public BaseException(String message, String code, Throwable cause) {
        super(message, cause);
        this.message = message;
        this.code = code;
    }

    public BaseException(Throwable cause) {
        super(cause);
    }

    @Override
    public String getMessage() {
        return message;
    }

}
