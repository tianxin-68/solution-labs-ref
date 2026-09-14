/* ssm:contributor||trino|template */
select
    main.*,
    lod1._ctm_m_3_0 as _ctm_m_3_0,
    lod2._ctm_m_3_0_d_avg as _ctm_m_3_0_d_avg
from
    (
        select
            M1.dim_analysis_business AS dim_analysis_business,
            M1.pay_orderid AS pay_orderid,
            M1.pay_orderid_avg_by_d AS pay_orderid_avg_by_d,
            row_number() over(
                ORDER BY
                    /*sort*/
                    bi_decode(
                            M1.dim_analysis_business,
                            '轮胎,01,保养,02,洗美,03,深美容,04,改装超市,05,维修,06,蓄电池,07,钣喷及事故件,08,轮毂,09,充电,10,道路救援,11,加油,12,其他,13,,98,(null),99,(other_concat_raw),90,未知,97'
                        ) asc nulls last
                ) as _r_n
        FROM
            (
                select
                    coalesce(
                            M1_Com.dim_analysis_business,
                            M1_Dst.dim_analysis_business
                        ) AS dim_analysis_business,
                    M1_Com.pay_orderid AS pay_orderid,
                    M1_Dst.pay_orderid_avg_by_d AS pay_orderid_avg_by_d,
                    coalesce(M1_Com._grp_v, M1_Dst._grp_v) as _grp_v
                from
                    (
                        select
                            F1.dim_analysis_business AS dim_analysis_business,
                            count(distinct F1.pay_orderid) AS pay_orderid,
                            null AS pay_orderid_avg_by_d,
                            grouping(F1.dim_analysis_business) as _grp_v
                        from
                            (
                            select
                            SUBSTR(vf.order_date, 1, 10) as _com_d,
                            vf.businessline as businessline,
                            if(
                            vf.dim_analysis_business is null,
                            '(null)',
                            format('%s', vf.dim_analysis_business)
                            ) as dim_analysis_business,
                            SUBSTR(vf.order_date, 1, 10) as dt,
                            vf.pay_orderid as pay_orderid,
                            null as pay_orderid_avg_by_d
                            from
                            bi_view.v_ads_zzqs_ord_order_dtl_super_multi_d vf
                            WHERE
                            vf.order_date between '2024-08-05'
                            and '2024-08-07'
                            AND vf.businessline IN ('保养', '轮胎')
                            ) F1
                        group by
                            grouping sets ((F1.dim_analysis_business))
                    ) M1_Com full
                    join (
                    select
                    F1.dim_analysis_business AS dim_analysis_business,
                    null AS pay_orderid,
                    try((sum(pay_orderid_avg_by_d)) * 1.0000 / 3) AS pay_orderid_avg_by_d,
                    grouping(F1.dim_analysis_business) as _grp_v
                    from
                    (
                    select
                    v2.dim_analysis_business as dim_analysis_business,
                    null as pay_orderid,
                    count(distinct v2.pay_orderid_avg_by_d) as pay_orderid_avg_by_d,
                    v2._com_d
                    from
                    (
                    select
                    SUBSTR(vf.order_date, 1, 10) as _com_d,
                    vf.businessline as businessline,
                    if(
                    vf.dim_analysis_business is null,
                    '(null)',
                    format('%s', vf.dim_analysis_business)
                    ) as dim_analysis_business,
                    SUBSTR(vf.order_date, 1, 10) as dt,
                    null as pay_orderid,
                    vf.pay_orderid as pay_orderid_avg_by_d
                    from
                    bi_view.v_ads_zzqs_ord_order_dtl_super_multi_d vf
                    WHERE
                    vf.order_date between '2024-08-05'
                    and '2024-08-07'
                    AND vf.businessline IN ('保养', '轮胎')
                    ) v2
                    GROUP BY
                    v2.dim_analysis_business,
                    v2._com_d
                    ) F1
                    group by
                    grouping sets ((F1.dim_analysis_business))
                    ) M1_Dst on coalesce(M1_Com.dim_analysis_business, '-9999') = coalesce(M1_Dst.dim_analysis_business, '-9999')
            ) M1
    ) main
        left join (
        select
            M2.dim_province_name AS dim_province_name,
            try(M2.app_active_deviceid) AS _ctm_m_3_0
        FROM
            (
                select
                    null as dim_province_name,
                    avg(v_lod1.app_active_deviceid) as app_active_deviceid,
                    0 as _grp_v
                from
                    (
                        select
                            F2.dim_province_name AS dim_province_name,
                            count(distinct F2.app_active_deviceid) AS app_active_deviceid,
                            grouping(F2.dim_province_name) as _grp_v
                        from
                            (
                            select
                            SUBSTR(vf.dt, 1, 10) as _com_d,
                            vf.businessline_new as businessline,
                            if(
                            vf.dim_province_name is null,
                            '(null)',
                            format('%s', vf.dim_province_name)
                            ) as dim_province_name,
                            SUBSTR(vf.dt, 1, 10) as dt,
                            vf.app_active_deviceid as app_active_deviceid
                            from
                            bi_view.v_ads_olap_core_manage_traffic_d vf
                            WHERE
                            vf.businessline_new IN ('保养', '轮胎')
                            AND vf.dt between '2024-08-05'
                            and '2024-08-07'
                            ) F2
                        group by
                            grouping sets ((F2.dim_province_name))
                    ) v_lod1
            ) M2
    ) lod1 on 1 = 1
        left join (
        select
            M2.dim_province_name AS dim_province_name,
            try(M2.app_active_deviceid_d_avg) AS _ctm_m_3_0_d_avg
        FROM
            (
                select
                    null as dim_province_name,
                    avg(v_lod2.app_active_deviceid_d_avg) as app_active_deviceid_d_avg,
                    0 as _grp_v
                from
                    (
                        -- 日均值
                        select
                            v_lod2_d_avg.dim_province_name,
                            sum(v_lod2_d_avg.app_active_deviceid)/count(distinct v_lod2_d_avg._com_d) as app_active_deviceid_d_avg
                        from (
                                 select
                                     F2.dim_province_name AS dim_province_name,
                                     F2._com_d,
                                     count(distinct F2.app_active_deviceid) AS app_active_deviceid,
                                     grouping(F2.dim_province_name,F2._com_d) as _grp_v
                                 from
                                     (
                                     select
                                     SUBSTR(vf.dt, 1, 10) as _com_d,
                                     vf.businessline_new as businessline,
                                     if(
                                     vf.dim_province_name is null,
                                     '(null)',
                                     format('%s', vf.dim_province_name)
                                     ) as dim_province_name,
                                     SUBSTR(vf.dt, 1, 10) as dt,
                                     vf.app_active_deviceid as app_active_deviceid
                                     from
                                     bi_view.v_ads_olap_core_manage_traffic_d vf
                                     WHERE
                                     vf.businessline_new IN ('保养', '轮胎')
                                     AND vf.dt between '2024-08-05'
                                     and '2024-08-07'
                                     ) F2
                                 group by
                                     grouping sets ((F2.dim_province_name,F2._com_d))
                             ) v_lod2_d_avg
                        group by v_lod2_d_avg.dim_province_name
                    ) v_lod2
            ) M2
    ) lod2 on 1 = 1
ORDER BY
    /*sort*/
    _r_n