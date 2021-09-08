package com.example.es;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * 单元测试基类
 *
 * @author YanCh
 * Create by 2020-02-11 15:56
 **/
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringEsDemoApplication.class)
@WebAppConfiguration
public class BaseApplicationTest {
    @Before
    public void init() {
        System.out.println("开始测试-----------------");
    }

    @After
    public void after() {
        System.out.println("测试结束-----------------");
    }

    public BaseApplicationTest() {
        System.setProperty("es.set.netty.runtime.available.processors", "false");
    }
}
