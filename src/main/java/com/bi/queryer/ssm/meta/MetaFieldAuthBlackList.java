package com.bi.queryer.ssm.meta;

/**
 * @Author contributor
 * @Date 13:56 2025/11/26
 * @Description TODO
 **/
public class MetaFieldAuthBlackList extends MetaFieldAuthWhiteList{
    public MetaFieldAuthBlackList(){

    }

    public MetaFieldAuthBlackList(String userName, String fieldGroup, String fieldCode, String fieldTitle){
        super(userName, fieldGroup, fieldCode, fieldTitle);
    }
}
