package com.bi.queryer.ssm.promotion.model;

import java.util.LinkedList;

public class PromoYearRsp {

    private String name;

    private LinkedList<PromoNameRsp> promoNameList = new LinkedList<>();

    public PromoYearRsp (String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LinkedList<PromoNameRsp> getPromoNameList() {
        return promoNameList;
    }

    public void setPromoNameList(LinkedList<PromoNameRsp> promoNameList) {
        this.promoNameList = promoNameList;
    }
}
