package com.bi.queryer.sys.enums;

/**
 * @author contributor
 */
public enum SortType {

    DESC,
    ASC,
    NONE;

    public static SortType getType(String typeStr){
        for(SortType t : SortType.values()){
            if(t.toString().equalsIgnoreCase(typeStr)){
                return t;
            }
        }
        return NONE;
    }
}
