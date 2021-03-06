package com.example.es.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注解es的index名称
 *
 * @author YanCh
 * @version v1.0
 * Create by 2020-07-16 12:09
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EsIndex {

    /**
     * es 索引名称
     *
     * @return
     */
    String name();

    /**
     * setting位置
     *
     * @return
     */
    String setting() default "";

    /**
     * mapping位置
     *
     * @return
     */
    String mapping() default "";

}
