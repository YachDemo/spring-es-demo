package com.example.es.config;

import com.example.es.annotation.EsIndex;
import com.example.es.exception.ElasticException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.reflections.Reflections;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Elasticsearch 配置
 * 由spring管理
 *
 * @author YanCh
 * Create by 2020-03-05 9:09
 **/
@Configuration
@Slf4j
public class ElasticsearchConfig implements InitializingBean, DisposableBean, FactoryBean<RestHighLevelClient> {

    /**
     * es连接地址
     */
    @Value("${es.hosts}")
    private String[] hosts;
    /**
     * es账号
     */
    @Value("${es.username:#{null}}")
    private String username;
    /**
     * es密码
     */
    @Value("${es.password:#{null}}")
    private String password;
    /**
     * es entity类 包位置
     */
    @Value("${es.locations}")
    private String locations;
    /**
     * 超时时间,单位毫秒
     */
    @Value("${es.connectTimeout:3000}")
    private int connectTimeout;
    /**
     * 请求数据超时时间,单位毫秒
     */
    @Value("${es.socketTimeout:3000}")
    private int socketTimeout;
    /**
     * 设置从connect Manager(连接池)获取Connection 超时时间，单位毫秒
     */
    @Value("${es.connectionRequestTimeout:3000}")
    private int connectionRequestTimeout;


    private RestHighLevelClient restHighLevelClient;


    /**
     * 索引map
     * class与indexName映射
     */
    public static final Map<Class, String> indexMap = new HashMap<>();

    @Override
    public RestHighLevelClient getObject() throws Exception {
        return this.restHighLevelClient;
    }

    @Override
    public Class<?> getObjectType() {
        return RestHighLevelClient.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void destroy() throws Exception {
        if (restHighLevelClient != null) {
            restHighLevelClient.close();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        restHighLevelClient = buildClient();
        createIndex(restHighLevelClient);
    }

    private RestHighLevelClient buildClient() {
        HttpHost[] httpHosts = new HttpHost[hosts.length];
        for (int i = 0; i < hosts.length; i++) {
            httpHosts[i] = HttpHost.create(hosts[i]);
        }
        RestClientBuilder builder = RestClient.builder(httpHosts);
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        if (StringUtils.isEmpty(username)) {
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        }
        // 设置连接
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
                    .setConnectTimeout(connectTimeout)
                    .setSocketTimeout(socketTimeout)
                    .setConnectionRequestTimeout(connectionRequestTimeout);
            httpClientBuilder.setDefaultRequestConfig(requestConfigBuilder.build());
            httpClientBuilder.disableAuthCaching();
            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            return httpClientBuilder;
        });
        restHighLevelClient = new RestHighLevelClient(builder);
        return restHighLevelClient;
    }

    /**
     * 创建es索引
     *
     * @param restHighLevelClient
     */
    private void createIndex(RestHighLevelClient restHighLevelClient) {

        Reflections reflections = new Reflections(locations);
        Set<Class<?>> classSet = reflections.getTypesAnnotatedWith(EsIndex.class);
        for (Class<?> clazz : classSet) {
            Annotation[] annotations = clazz.getAnnotations();
            for (Annotation annotation : annotations) {
                EsIndex esIndex = (EsIndex) annotation;
                indexMap.put(clazz, esIndex.name());
                try {
                    if (StringUtils.isEmpty(esIndex.mapping()) || StringUtils.isEmpty(esIndex.setting())) {
                        continue;
                    }
                    if (this.indexExist(esIndex.name(), restHighLevelClient)) {
                        log.warn("idxName={} 已经存在", esIndex.name());
                        continue;
                    }
                    String idxSQL = readJson(esIndex.mapping());
                    String setting = readJson(esIndex.setting());
                    if (StringUtils.isEmpty(idxSQL)) {
                        throw new ElasticException("es mapping empty");
                    }
                    if (StringUtils.isEmpty(setting)) {
                        throw new ElasticException("es setting empty");
                    }
                    CreateIndexRequest request = new CreateIndexRequest(esIndex.name());
                    request.mapping(idxSQL, XContentType.JSON);
                    request.settings(setting, XContentType.JSON); // 手工指定Setting
                    CreateIndexResponse res;
                    res = restHighLevelClient.indices().create(request, RequestOptions.DEFAULT);
                    if (!res.isAcknowledged()) {
                        throw new ElasticException("初始化失败");
                    } else {
                        log.info("创建index={}，创建成功", esIndex.name());
                    }
                } catch (IOException e) {
                    throw new ElasticException("读取资源失败", e);
                }
            }
        }
    }

    /**
     * 断某个index是否存在
     *
     * @param idxName index名
     * @return boolean
     */
    public boolean indexExist(String idxName, RestHighLevelClient restHighLevelClient) throws IOException {
        return restHighLevelClient.indices().exists(new GetIndexRequest(idxName), RequestOptions.DEFAULT);
    }


    private String readJson(String path) throws IOException {
        //此处如果用File file = Resource.getFile(filePath)会报异常：找不到文件
        Resource resource = new ClassPathResource(path);
        InputStream is = resource.getInputStream();
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8.name());
    }

}
