package com.bi.queryer.sys.authority.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author contributor
 */
public class AuthWarnUser {

    private String userName;

    private List<AuthWarnInfo> list =new ArrayList<>();

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public List<AuthWarnInfo> getList() {
        return list;
    }

    public void setList(List<AuthWarnInfo> list) {
        this.list = list;
    }
}
