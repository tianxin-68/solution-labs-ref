select
    coalesce(M1.dt,M2.dt) AS dt,
    coalesce(M1.AAE,M2.AAE) AS AAE,
    coalesce(M1.O_ORD_00199, M2.O_ORD_00199) AS O_ORD_00199,
    coalesce(M1.car_year, M2.car_year) AS car_year
FROM
    (
        select
            F1.dt AS dt,
            F1.AAE AS AAE,
            sum(F1.O_ORD_00199) AS O_ORD_00199,
            null as car_year,
            if(F1.dt is null, pow(2, 1), 0) + if(F1.AAE is null, pow(2, 0), 0) as _grp_v
        from
            (
                select
                    if(
                                coalesce(concat('', vd1.province_name), '') in('', 'null'),
                                '(null)',
                                concat('', vd1.province_name)
                        ) as AAE,
                    vf.businessline as ABA,
                    substring(vf.dt, 1, 10) as _com_d,
                    substring(vf.dt, 1, 10) as dt,
                    vf.submit_gmv as O_ORD_00199
                from
                    bi_olap.ads_ord_order_detail_dtl_di vf
                        left join bi_olap.dim_reg_city_f vd1 on vf.dim_city_name = vd1.city_name
                WHERE
                        vf.businessline IN ('保养', '洗美', '轮胎')
                  AND vf.dt between '2025-03-10'
                    and '2025-03-16'
            ) F1
        group by
            grouping sets ((F1.dt, F1.AAE))
    ) M1
-- 注：需要加此类特殊指标配置为独立的逻辑表，便于同一个表的不同指标进行聚合
        full join (
        select
            F1.dt,
            F1.AAE AS AAE,
            avg(F1.car_year) as car_year,
            null as O_ORD_00199
        from (
                 select
                     -- 注：此处无维度表，只有一个视图，视图中已关联了维度表
                     vf.dt,
                     vf.province_name as AAE,
                     vf.car_year
                 from (
                          select
                              t1.keypage_prd_listing_deviceid
                               ,t1.dim_tid
                               ,cast(t1.dim_car_year as double) as car_year
                               ,vd1.province_name
                               ,t1.dt
                          from bi_olap.ads_tfc_conv_platform_undertake_sum_di t1
                                   left join bi_olap.dim_reg_city_f vd1 on t1.dim_final_city = vd1.city_name  -- 注：维度表需要在最里程关联
                          where 1=1
                            and t1.dt between '2025-03-10' and '2025-03-16' -- 注：where条件在最里程加
                          group by t1.keypage_prd_listing_deviceid
                                 ,t1.dim_tid
                                 ,dim_car_year
                                 ,vd1.province_name
                                 ,t1.dt
                      ) vf
                 where vf.dt between '2025-03-10' and '2025-03-16' -- 注：此处可以去掉维度表的过来
             ) F1
        group by grouping sets ((F1.dt, F1.AAE))
    ) M2
                  on M1.dt = M2.dt and M1.AAE = M2.AAE
ORDER BY
    /*sort*/
    dt asc nulls last