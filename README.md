# Spring Boot集成Elasticsearch基础脚手架

## 使用到的es分词器

- [elasticsearch-analysis-ik](https://github.com/medcl/elasticsearch-analysis-ik) ik分词器
- [elasticsearch-analysis-pinyin](https://github.com/medcl/elasticsearch-analysis-pinyin) 拼音分词器
- [elasticsearch-analysis-stconvert](https://github.com/medcl/elasticsearch-analysis-stconvert) 繁简转换分词

## 项目结构

```text
├─java
│  └─com
│      └─example
│          └─es  源码
│              ├─annotation es基础注解
│              ├─config     es初始化配置
│              ├─entity     es model以及基础model存放
│              ├─exception  异常
│              ├─service    基础接口
│              │  └─impl    基础实现
│              └─utils      工具类
└─resources
    └─es-json  mapping配置存放
```

## 说明

项目启动会根据```EsIndex```自动创建索引，示例见```ElTest``` 

model继承```BaseElasticEntity```, service继承```BaseElasticService<T>```, impl继承```BaseElasticServiceImpl<T>```就可实现基础的增删改查
