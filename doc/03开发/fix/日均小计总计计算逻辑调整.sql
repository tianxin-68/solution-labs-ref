/* ssm:contributor|9ce3a6328a3f4ff2808972f8ee08f0ab:日均-小计+总计|jqfensS8JOEUchBu8InMI|trino|template */
with ds_cur as (
    select
        M1.dt AS dt,
        M1.dim_analysis_business AS dim_analysis_business,
        M1.dim_city_level AS dim_city_level,
        M1.submit_order AS submit_order,
        M1.submit_order_avg_by_d AS submit_order_avg_by_d,
        M1._grp_v as _grp_v
    FROM
        (
            select
                coalesce(M1_Com.dt, M1_Dst.dt) AS dt,
                coalesce(M1_Com.dim_city_level, M1_Dst.dim_city_level) AS dim_city_level,
                coalesce(
                        M1_Com.dim_analysis_business,
                        M1_Dst.dim_analysis_business
                    ) AS dim_analysis_business,
                M1_Com.submit_order AS submit_order,
                M1_Dst.submit_order_avg_by_d AS submit_order_avg_by_d,
                coalesce(M1_Com._grp_v, M1_Dst._grp_v) as _grp_v
            from
                (
                    select
                        F1.dt AS dt,
                        F1.dim_city_level AS dim_city_level,
                        F1.dim_analysis_business AS dim_analysis_business,
                        count(distinct F1.submit_order) AS submit_order,
                        null AS submit_order_avg_by_d,
                        grouping(F1.dt, F1.dim_analysis_business, F1.dim_city_level) as _grp_v
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
                                if(
                                        vf.dim_city_level is null,
                                        '(null)',
                                        format('%s', vf.dim_city_level)
                                    ) as dim_city_level,
                                bi_get_week_id(vf.order_date) as dt,
                                vf.submit_order as submit_order,
                                null as submit_order_avg_by_d
                            from
                                bi_view.v_ads_zzqs_ord_order_dtl_super_multi_d vf
                            WHERE
                                    vf.dim_city_level IN ('一线城市', '新一线城市')
                              AND vf.order_date between '2024-08-12'
                                and '2024-08-25'
                              AND vf.businessline IN ('保养', '轮胎')
                        ) F1
                    group by
                        grouping sets (
                        (F1.dim_city_level, F1.dt, F1.dim_analysis_business),(F1.dt, F1.dim_analysis_business),(F1.dt)
                        )
                ) M1_Com full join (

                    select
                        F1.dt AS dt,
                        F1.dim_analysis_business AS dim_analysis_business,
                        F1.dim_city_level AS dim_city_level,
                        null AS submit_order,
                        try(
                                        (sum(submit_order_avg_by_d)) * 1.000000 / if(dt is null, 14, 7)
                            ) AS submit_order_avg_by_d,
                        grouping(F1.dt, F1.dim_analysis_business, F1.dim_city_level) as _grp_v
                    from
                        (
                            select
                                v2.dt as dt,
                                v2._com_d,
                                v2.dim_analysis_business as dim_analysis_business,
                                v2.dim_city_level as dim_city_level,
                                null as submit_order,
                                count(distinct v2.submit_order_avg_by_d) as submit_order_avg_by_d,
                                grouping(v2.dt, v2._com_d, v2.dim_analysis_business, v2.dim_city_level) as _grp_v
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
                                        if(
                                                vf.dim_city_level is null,
                                                '(null)',
                                                format('%s', vf.dim_city_level)
                                            ) as dim_city_level,
                                        bi_get_week_id(vf.order_date) as dt,
                                        null as submit_order,
                                        vf.submit_order as submit_order_avg_by_d
                                    from
                                        bi_view.v_ads_zzqs_ord_order_dtl_super_multi_d vf
                                    WHERE
                                            vf.dim_city_level IN ('一线城市', '新一线城市')
                                      AND vf.order_date between '2024-08-12'
                                        and '2024-08-25'
                                      AND vf.businessline IN ('保养', '轮胎')
                                ) v2
                            group by v2.dt,
                                     v2._com_d,
                                     v2.dim_analysis_business,
                                     v2.dim_city_level
                        ) F1
                    group by
                        -- 此处不做小计和总计groping sets
                        grouping sets (
                        (F1.dim_city_level, F1.dt, F1.dim_analysis_business)
                        )
                    -- ----------添加总计、小计------------------
                    union all
                    -- ----------------------------
                    select
                        F1.dt AS dt,
                        F1.dim_analysis_business AS dim_analysis_business,
                        F1.dim_city_level AS dim_city_level,
                        null AS submit_order,
                        try(
                                        (sum(submit_order_avg_by_d)) * 1.000000 / if(dt is null, 14, 7)
                            ) AS submit_order_avg_by_d,
                        F1._grp_v as _grp_v
                    from
                        (
                            select
                                v2.dt as dt,
                                v2._com_d,
                                v2.dim_analysis_business as dim_analysis_business,
                                v2.dim_city_level as dim_city_level,
                                null as submit_order,
                                count(distinct v2.submit_order_avg_by_d) as submit_order_avg_by_d,
                                -- 此处添加小计总计标识
                                grouping(v2.dt, v2._com_d, v2.dim_analysis_business, v2.dim_city_level) as _grp_v
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
                                        if(
                                                vf.dim_city_level is null,
                                                '(null)',
                                                format('%s', vf.dim_city_level)
                                            ) as dim_city_level,
                                        bi_get_week_id(vf.order_date) as dt,
                                        null as submit_order,
                                        vf.submit_order as submit_order_avg_by_d
                                    from
                                        bi_view.v_ads_zzqs_ord_order_dtl_super_multi_d vf
                                    WHERE
                                            vf.dim_city_level IN ('一线城市', '新一线城市')
                                      AND vf.order_date between '2024-08-12'
                                        and '2024-08-25'
                                      AND vf.businessline IN ('保养', '轮胎')
                                ) v2
                            -- 此处添加小计总计grouping sets
                            group by grouping sets(
                                (
                                 v2.dt,
                                 v2._com_d,
                                 v2.dim_analysis_business,
                                 v2.dim_city_level
                                    ),
                                (
                                 v2.dt,
                                 v2._com_d,
                                 v2.dim_analysis_business
                                    ),
                                (
                                 v2.dt,
                                 v2._com_d
                                    )
                                )
                        ) F1
                    -- 添加where条件，去掉明细数据
                    where 1=1
                      and F1._grp_v > 0
                    -- 此处无需再次小计总计
                    group by F1.dim_city_level, F1.dt, F1.dim_analysis_business, F1._grp_v

                ) M1_Dst on coalesce(M1_Com.dt, '-9999') = coalesce(M1_Dst.dt, '-9999')
                    and coalesce(M1_Com.dim_city_level, '-9999') = coalesce(M1_Dst.dim_city_level, '-9999')
                    and coalesce(M1_Com.dim_analysis_business, '-9999') = coalesce(M1_Dst.dim_analysis_business, '-9999')
                    and M1_Com._grp_v = M1_Dst._grp_v
        ) M1
)
select
    ds_cur.dt as dt,
    ds_cur.dim_analysis_business as dim_analysis_business,
    ds_cur.dim_city_level as dim_city_level,
    ds_cur.submit_order,
    ds_cur.submit_order_avg_by_d,
    case
        ds_cur._grp_v
        when 7 then 'col_total'
        when 3 then 'col_subtotal'
        when 1 then 'col_subtotal'
        else null
        end as _grp_v,
    ds_cur._grp_v as _grp_k
from
    ds_cur ds_cur
ORDER BY
    /*sort*/
    dt asc nulls last,
    bi_decode(
            dim_analysis_business,
            '(other_concat_raw),90,未知,97,,98,(null),99'
        ) asc nulls last,
    bi_decode(
            dim_city_level,
            '一线城市,1,新一线城市,2,二线城市,3,三线城市,4,四线城市,5,五线城市,6,未知,97,,98,(other_concat_raw),90,(null),99'
        ) asc nulls last,
    _grp_v asc nulls last