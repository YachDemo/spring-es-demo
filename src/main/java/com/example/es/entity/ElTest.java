package com.example.es.entity;

import com.example.es.annotation.EsIndex;
import lombok.Data;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.geometry.Geometry;
import org.elasticsearch.geometry.GeometryCollection;

import java.util.List;
import java.util.Map;

/**
 * Es 保存的骑手位置信息
 *
 * @author YanCh
 * @version v1.0
 * Create by 2020-07-17 10:54
 **/
@Data
@EsIndex(name = "el_test",
        mapping = "/es-json/el_test-mapping.json",
        setting = "/es-json/base-setting.json")
public class ElTest extends BaseElasticEntity {


    private String name;

    private GeoPoint location;

    public ElTest(String id) {
        super(id);
    }

    public ElTest() {
    }


}
