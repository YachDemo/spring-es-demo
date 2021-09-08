package com.example.es.service;

import com.example.es.entity.BaseElasticEntity;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Elastic接口基类
 *
 * @author YanCh
 * Create by 2020-03-05 10:05
 **/
public interface BaseElasticService<T extends BaseElasticEntity> {

    default Class<T> getClazz() {
        return (Class<T>) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0];
    }

    /**
     * 创建索引
     *
     * @param idxSQL  索引描述
     * @param setting setting
     */
    void createIndex(String idxSQL, String setting);

    /**
     * 断某个index是否存在
     *
     * @return boolean
     */
    boolean indexExist() throws Exception;

    /**
     * 断某个index是否存在
     *
     * @return boolean
     * @throws
     */
    boolean isExistsIndex() throws Exception;

    /**
     * 设置分片
     *
     * @param request
     * @param shards   分片数
     * @param replicas 副本数
     * @throws
     */
    void buildSetting(CreateIndexRequest request, Integer shards, Integer replicas);

    /**
     * 插入更新数据
     *
     * @param t 对象
     */
    void insertOrUpdateOne(T t);

    /**
     * 根据id更新部分数据
     *
     * @param t
     */
    void updateNotNull(T t);

    /**
     * 批量插入数据
     *
     * @param list 带插入列表
     */
    void insertBatch(List<T> list);

    /**
     * 批量删除
     *
     * @param idList 待删除列表
     */
    void deleteBatch(Collection<T> idList);

    /**
     * 查询
     *
     * @param builder 查询参数
     * @return
     */
    List<T> search(SearchSourceBuilder builder);


    SearchResponse searchResponse(SearchSourceBuilder builder);

    /**
     * set打乱顺序保证唯一
     *
     * @param builder
     * @return
     */
    Set<T> searchBySet(SearchSourceBuilder builder);

    /**
     * 根据id查询
     *
     * @param id
     * @return
     */
    T searchById(String id);

    int searchCount(QueryBuilder builder);

    /**
     * 删除index
     */
    void deleteIndex();

    /**
     * 批量删除
     *
     * @param builder
     */
    void deleteByQuery(QueryBuilder builder);

    /**
     * 根据id删除
     *
     * @param id
     */
    void deleteById(String id);
}
