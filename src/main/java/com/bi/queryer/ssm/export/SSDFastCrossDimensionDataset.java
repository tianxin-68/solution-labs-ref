package com.bi.queryer.ssm.export;

import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author contributor
 * @Date 11:48 2023-04-04
 * @Description TODO
 **/
public class SSDFastCrossDimensionDataset {
    protected List<ResultDataSetColumn> titles = new ArrayList<>();

    protected List<Map<String,Object>> rows = new ArrayList<>();

    protected void clear() {
        if(titles != null) {
            titles.clear();
        }
        if(rows != null) {
            rows.clear();
        }
    }
}
