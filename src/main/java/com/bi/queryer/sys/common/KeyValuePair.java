package com.bi.queryer.sys.common;

/**
 * User: contributor
 * Date: 2020/2/7
 * Time: 11:31
 * Description:
 */
public class KeyValuePair implements Comparable{
    public String key;
    public Object value;

    public KeyValuePair(){

    }

    public KeyValuePair(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public int compareTo(Object o) {
        if (o == null) {
            return -1;
        }
        return this.key.compareTo( ((KeyValuePair)o).key);
    }
}
