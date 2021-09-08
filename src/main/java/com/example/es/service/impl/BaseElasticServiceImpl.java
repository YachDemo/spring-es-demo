package com.example.es.service.impl;

import com.example.es.annotation.EsIndex;
import com.example.es.config.ElasticsearchConfig;
import com.example.es.entity.BaseElasticEntity;
import com.example.es.exception.ElasticException;
import com.example.es.service.BaseElasticService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;

/**
 * Elastic接口基类
 *
 * @author YanCh
 * Create by 2020-03-05 10:05
 **/
@Slf4j
@Component
public class BaseElasticServiceImpl<T extends BaseElasticEntity> implements BaseElasticService<T> {
    @Autowired
    RestHighLevelClient restHighLevelClient;

    private String getIndexName() {
        return ElasticsearchConfig.indexMap.get(getClazz());
    }

    public void createIndex(String idxSQL, String setting) {
        String indexName = getIndexName();
        try {
            if (this.isExistsIndex()) {
                log.error(" idxName={} 已经存在,idxSql={}", indexName, idxSQL);
                return;
            }
            CreateIndexRequest request = new CreateIndexRequest(indexName);
            request.mapping(idxSQL, XContentType.JSON);
            request.settings(setting, XContentType.JSON); // 手工指定Setting
            CreateIndexResponse res = restHighLevelClient.indices().create(request, RequestOptions.DEFAULT);
            if (!res.isAcknowledged()) {
                throw new ElasticException("初始化失败");
            }
        } catch (Exception e) {
            throw new ElasticException(e);
        }
    }

    @Override
    public boolean indexExist() throws IOException {
        GetIndexRequest request = new GetIndexRequest(getIndexName());
        request.local(false);
        request.humanReadable(true);
        request.includeDefaults(false);
        request.indicesOptions(IndicesOptions.lenientExpandOpen());
        return restHighLevelClient.indices().exists(request, RequestOptions.DEFAULT);
    }


    @Override
    public boolean isExistsIndex() throws Exception {
        return restHighLevelClient.indices().exists(new GetIndexRequest(getIndexName()), RequestOptions.DEFAULT);
    }

    @Override
    public void buildSetting(CreateIndexRequest request, Integer shards, Integer replicas) {
        request.settings(Settings.builder().put("index.number_of_shards", shards)
                .put("index.number_of_replicas", replicas));
    }

    @Override
    public void insertOrUpdateOne(T t) {
        IndexRequest request = new IndexRequest(getIndexName());
        Gson gson = new Gson();
        String json = gson.toJson(t);
        log.info("Data : id={},entity={}", t.getId(), json);
        request.id(t.getId());
        request.source(json, XContentType.JSON);
//        request.source(JSON.toJSONString(entity.getData()), XContentType.JSON);
        try {
            restHighLevelClient.index(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new ElasticException(e);
        }
    }

    @Override
    public void updateNotNull(T t) {
        if (StringUtils.isEmpty(t.getId())) {
            throw new ElasticException("es id not find");
        }
        UpdateRequest request = new UpdateRequest(getIndexName(), t.getId());
        String json = new Gson().toJson(t);
        request.doc(json, XContentType.JSON);
        try {
            restHighLevelClient.update(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new ElasticException("修改失败", e);
        }
    }

    @Override
    public void insertBatch(List<T> list) {
        BulkRequest request = new BulkRequest();
        String idxName = getIndexName();
        Gson gson = new GsonBuilder().serializeNulls().create();
        list.forEach(item -> request.add(new IndexRequest(idxName).id(item.getId()).source(gson.toJson(item), XContentType.JSON)));
        try {
            restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
        } catch (Exception e) {
            throw new ElasticException("批量插入失败", e);
        }
    }

    @Override
    public void deleteBatch(Collection<T> idList) {
        BulkRequest request = new BulkRequest();
        String indexName = getIndexName();
        idList.forEach(item -> request.add(new DeleteRequest(indexName, item.toString())));
        try {
            restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
        } catch (Exception e) {
            throw new ElasticException("批量删除失败", e);
        }
    }

    @Override
    public List<T> search(SearchSourceBuilder builder) {
        String idxName = getIndexName();
        SearchRequest request = new SearchRequest(idxName);
        request.source(builder);
        try {
            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
            SearchHit[] hits = response.getHits().getHits();
            List<T> res = new ArrayList<>(hits.length);
            for (SearchHit hit : hits) {
                // 距离 如果定义距离
//                BigDecimal geoDis = BigDecimal.valueOf((double) hit.getSortValues()[0]);
//                System.out.println(">>>>>>>>>>>>>>>>>>>>" + geoDis);
                res.add(new GsonBuilder().create().fromJson(hit.getSourceAsString(), getClazz()));
            }
            return res;
        } catch (Exception e) {
            throw new ElasticException(e);
        }
    }

    @Override
    public SearchResponse searchResponse(SearchSourceBuilder builder) {
        String idxName = getIndexName();
        SearchRequest request = new SearchRequest(idxName);
        request.source(builder);
        try {
            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
            SearchHit[] hits = response.getHits().getHits();
            return response;
        } catch (Exception e) {
            throw new ElasticException(e);
        }
    }

    @Override
    public Set<T> searchBySet(SearchSourceBuilder builder) {
        String idxName = getIndexName();
        SearchRequest request = new SearchRequest(idxName);
        request.source(builder);
        try {
            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
            SearchHit[] hits = response.getHits().getHits();
            Set<T> res = new HashSet<>(hits.length);
            for (SearchHit hit : hits) {
                // 距离 如果定义距离
//                BigDecimal geoDis = BigDecimal.valueOf((double) hit.getSortValues()[0]);
//                System.out.println(">>>>>>>>>>>>>>>>>>>>" + geoDis);
                res.add(new GsonBuilder().create().fromJson(hit.getSourceAsString(), getClazz()));
            }
            return res;
        } catch (Exception e) {
            throw new ElasticException("搜索服务异常", e);
        }
    }

    @Override
    public T searchById(String id) {
        GetRequest getRequest = new GetRequest(getIndexName(), id);
        GetResponse getResponse;
        try {
            getResponse = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new ElasticException(e);
        }
        String json = getResponse.getSourceAsString();
        return new GsonBuilder().serializeNulls().create().fromJson(json, getClazz());
    }

    @Override
    public int searchCount(QueryBuilder builder) {
        CountRequest countRequest = new CountRequest(getIndexName());
        countRequest.query(builder);
        try {
            CountResponse response = restHighLevelClient.count(countRequest, RequestOptions.DEFAULT);
            return (int) response.getCount();
        } catch (Exception e) {
            throw new ElasticException(e);
        }
    }


    public void deleteIndex() {
        String idxName = getIndexName();
        try {
            if (!this.indexExist()) {
                log.error(" idxName={} 已经存在", idxName);
                return;
            }
            restHighLevelClient.indices().delete(new DeleteIndexRequest(idxName), RequestOptions.DEFAULT);
        } catch (Exception e) {
            throw new ElasticException(e);
        }
    }


    public void deleteByQuery(QueryBuilder builder) {
        DeleteByQueryRequest request = new DeleteByQueryRequest(getIndexName());
        request.setQuery(builder);
        //设置批量操作数量,最大为10000
        request.setBatchSize(10000);
        request.setConflicts("proceed");
        try {
            restHighLevelClient.deleteByQuery(request, RequestOptions.DEFAULT);
        } catch (Exception e) {
            throw new ElasticException(e);
        }
    }

    @Override
    public void deleteById(String id) {
        String indexName = getIndexName();
        DeleteRequest deleteRequest = new DeleteRequest();
        deleteRequest.id(id);
        deleteRequest.index(indexName);
        // 设置超时时间
        deleteRequest.timeout(TimeValue.timeValueMinutes(2));
        // 设置刷新策略"wait_for"
        // 保持此请求打开，直到刷新使此请求的内容可以搜索为止。此刷新策略与高索引和搜索吞吐量兼容，但它会导致请求等待响应，直到发生刷新
        deleteRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
        // 删除
        try {
            restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new ElasticException(e);
        }
    }
}
