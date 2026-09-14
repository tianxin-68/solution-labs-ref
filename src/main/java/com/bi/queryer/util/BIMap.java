package com.bi.queryer.util;

import com.alibaba.fastjson.JSONObject;

import java.io.Serializable;
import java.util.HashMap;

/**
 * User: contributor
 * Date: 2020/2/7
 * Time: 12:14
 * Description:
 */
public class BIMap extends HashMap implements JSONSerializable, Serializable, Cloneable{
    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.putAll(this);
        return json;
    }
}
