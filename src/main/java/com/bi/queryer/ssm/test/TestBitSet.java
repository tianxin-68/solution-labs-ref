package com.bi.queryer.ssm.test;

import cn.hutool.core.util.RandomUtil;

import java.util.*;

/**
 * @Author contributor
 * @Date 15:07 2025/12/13
 * @Description TODO
 **/
public class TestBitSet {
    public static void main(String[] args) {
        BitSet b1 = new BitSet(4);
        b1.set(0);
        b1.set(5);
        b1.set(18);
        b1.set(9);

        BitSet b2 = new BitSet(4);
        b2.set(1);
        b2.set(5);
        b2.set(5);
        b2.set(8);
        b2.set(8);

        boolean f = b2.intersects(b1);
        System.out.println(b2);
        b2.and(b1);
        System.out.println(b2);

long t1 = System.currentTimeMillis();
        Map<String, BitSet> bitSetList = new HashMap<>(128);
        for(int i = 0 ; i< 100000; i++){
            BitSet bs = new BitSet(500);
            Integer randomCount = RandomUtil.randomInt(20,30);
            for(int j = 0 ; j < randomCount; j++){
                Integer randomIndex = RandomUtil.randomInt(0,30);
                bs.set(randomIndex);
            }
            bitSetList.put("field" + i, bs);
        }

        Set<String> queryFields = new HashSet<>();
        for(int i = 0 ; i< 50; i++){
            Integer randomIndex = RandomUtil.randomInt(0,50);
            queryFields.add("field" + randomIndex);
        }

        Iterator<String> iterator = queryFields.iterator();
        BitSet intersection = null;
        BitSet current = null;
        while(iterator.hasNext()){
            String fieldCode = iterator.next();
            current =  bitSetList.get(fieldCode);
            if(current == null){
                continue;
            }
            if(intersection == null){
                intersection = current;
                continue;
            }
            intersection = (BitSet) intersection.clone();
            intersection.and(current);
            if(intersection.isEmpty()){
                break;
            }
        }

        if(intersection != null && !intersection.isEmpty()){
            System.out.println(intersection);
        }

        long t2 = System.currentTimeMillis();
        System.out.println(t2 - t1);

    }
}
