package com.bi.queryer.ssm.promotion.model;

import java.util.LinkedList;

public class PromoNameRsp {

    private String name;

    private LinkedList<PromoPhaseRsp> phaseList = new LinkedList<>();

    public PromoNameRsp (String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LinkedList<PromoPhaseRsp> getPhaseList() {
        return phaseList;
    }

    public void setPhaseList(LinkedList<PromoPhaseRsp> phaseList) {
        this.phaseList = phaseList;
    }
}
