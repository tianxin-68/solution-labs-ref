/* ssm:doris|template */
with ds_cur as (
    select
        coalesce(M1.AAE, M2.AAE, M3.AAE) AS AAE,
        coalesce(M1.D_SHP_01445, M2.D_SHP_01445, M3.D_SHP_01445) AS D_SHP_01445,
        coalesce(M1.O_ORD_00199, M2.O_ORD_00199, M3.O_ORD_00199) AS O_ORD_00199,
        coalesce(M1.O_TFC_00005, M2.O_TFC_00005, M3.O_TFC_00005) AS O_TFC_00005,
        coalesce(M1._grp_v, M2._grp_v, M3._grp_v) as _grp_v
    FROM
        (
            select
                F1.AAE AS AAE,
                count(distinct F1.O_SHP_00007) AS O_SHP_00007,
                null AS D_SHP_01445_hb_ratio,
                null AS O_ORD_00199_hb_ratio,
                null AS O_TFC_00005_hb_ratio,
                null AS O_TFC_00005_tb_y_ratio,
                null AS O_ORD_00199_tb_y_ratio,
                null AS D_SHP_01445_tb_y_ratio,
                (
                    COUNT(
                            DISTINCT IF(
                            F1.O_SHP_00007 IS NOT null
                                and F1.AFP = '工场店',
                            CONCAT(
                                    F1.ads_shp_rev_shop_install_order_dtl_di_stat_date,
                                    '~',
                                    F1.AIK
                            ),
                            NULL
                                     )
                    )
                    ) AS D_SHP_01445,
                null AS O_ORD_00199,
                null AS O_TFC_00005,
                if(F1.AAE is null, pow(2, 0), 0) as _grp_v
            from
                (
                    select
                        if(
                                coalesce(concat('', vd1.province_name), '') in('', 'null'),
                                '(null)',
                                concat('', vd1.province_name)
                        ) as AAE,
                        vf.dim_shop_co_class_name as AFP,
                        vf.install_shopid as AIK,
                        substring(vf.dt, 1, 10) as _com_d,
                        vf.stat_date as ads_shp_rev_shop_install_order_dtl_di_stat_date,
                        substring(vf.dt, 1, 10) as dt,
                        null as O_ORD_00199,
                        null as O_ORD_00199_hb_ratio,
                        null as O_ORD_00199_tb_y_ratio,
                        vf.install_userid as O_SHP_00007,
                        null as O_TFC_00005,
                        null as O_TFC_00005_hb_ratio,
                        null as O_TFC_00005_tb_y_ratio
                    from
                        bi_olap.ads_shp_rev_shop_install_order_dtl_di vf
                            left join bi_olap.dim_reg_city_f vd1 on vf.dim_city_name = vd1.city_name
                    WHERE
                        vf.dt between '2026-03-11'
                            and '2026-03-25'
                ) F1
            group by
                grouping sets ((F1.AAE))
        ) M1 FULL
                 JOIN (
            select
                F2.AAE AS AAE,
                null AS D_SHP_01445_hb_ratio,
                null AS O_ORD_00199_hb_ratio,
                null AS O_TFC_00005_hb_ratio,
                null AS O_TFC_00005_tb_y_ratio,
                null AS O_ORD_00199_tb_y_ratio,
                null AS D_SHP_01445_tb_y_ratio,
                null AS D_SHP_01445,
                null AS O_ORD_00199,
                count(distinct F2.O_TFC_00005) AS O_TFC_00005,
                if(F2.AAE is null, pow(2, 0), 0) as _grp_v
            from
                (
                    select
                        if(
                                coalesce(concat('', vf.dim_province_name), '') in('', 'null'),
                                '(null)',
                                concat('', vf.dim_province_name)
                        ) as AAE,
                        substring(vf.dt, 1, 10) as _com_d,
                        substring(vf.dt, 1, 10) as dt,
                        null as O_ORD_00199,
                        null as O_ORD_00199_hb_ratio,
                        null as O_ORD_00199_tb_y_ratio,
                        vf.dau_deviceid as O_TFC_00005,
                        null as O_TFC_00005_hb_ratio,
                        null as O_TFC_00005_tb_y_ratio
                    from
                        bi_olap.ads_tfc_deviceid_active_dtl_di vf
                    WHERE
                        vf.dt between '2026-03-11'
                            and '2026-03-25'
                ) F2
            group by
                grouping sets ((F2.AAE))
        ) M2 ON (
            coalesce(M1.AAE, '-9999') = coalesce(M2.AAE, '-9999')
            ) FULL
                 JOIN (
            select
                F3.AAE AS AAE,
                null AS D_SHP_01445_hb_ratio,
                null AS O_ORD_00199_hb_ratio,
                null AS O_TFC_00005_hb_ratio,
                null AS O_TFC_00005_tb_y_ratio,
                null AS O_ORD_00199_tb_y_ratio,
                null AS D_SHP_01445_tb_y_ratio,
                null AS D_SHP_01445,
                sum(F3.O_ORD_00199) AS O_ORD_00199,
                null AS O_TFC_00005,
                if(F3.AAE is null, pow(2, 0), 0) as _grp_v
            from
                (
                    select
                        if(
                                coalesce(concat('', vd1.province_name), '') in('', 'null'),
                                '(null)',
                                concat('', vd1.province_name)
                        ) as AAE,
                        vf.dim_shop_co_class_name as AFP,
                        vf.install_shopid as AIK,
                        substring(vf.dt, 1, 10) as _com_d,
                        substring(vf.dt, 1, 10) as dt,
                        vf.submit_gmv as O_ORD_00199,
                        null as O_ORD_00199_hb_ratio,
                        null as O_ORD_00199_tb_y_ratio,
                        null as O_TFC_00005,
                        null as O_TFC_00005_hb_ratio,
                        null as O_TFC_00005_tb_y_ratio
                    from
                        bi_olap.ads_ord_order_detail_dtl_di vf
                            left join bi_olap.dim_reg_city_f vd1 on vf.dim_city_name = vd1.city_name
                            and vf.dim_city_name = vd1.city_name
                    WHERE
                        vf.dt between '2026-03-11'
                            and '2026-03-25'
                ) F3
            group by
                grouping sets ((F3.AAE))
        ) M3 ON (
            coalesce(M1.AAE, M2.AAE, '-9999') = coalesce(M3.AAE, '-9999')
            )
),
     ds_tb_y as (
         select
             coalesce(M1.AAE, M2.AAE, M3.AAE) AS AAE,
             coalesce(M1.D_SHP_01445, M2.D_SHP_01445, M3.D_SHP_01445) AS D_SHP_01445,
             coalesce(M1.O_ORD_00199, M2.O_ORD_00199, M3.O_ORD_00199) AS O_ORD_00199,
             coalesce(M1.O_TFC_00005, M2.O_TFC_00005, M3.O_TFC_00005) AS O_TFC_00005,
             coalesce(M1._grp_v, M2._grp_v, M3._grp_v) as _grp_v
         FROM
             (
                 select
                     F1.AAE AS AAE,
                     count(distinct F1.O_SHP_00007) AS O_SHP_00007,
                     null AS D_SHP_01445_hb_ratio,
                     null AS O_ORD_00199_hb_ratio,
                     null AS O_TFC_00005_hb_ratio,
                     null AS O_TFC_00005_tb_y_ratio,
                     null AS O_ORD_00199_tb_y_ratio,
                     null AS D_SHP_01445_tb_y_ratio,
                     (
                         COUNT(
                                 DISTINCT IF(
                                 F1.O_SHP_00007 IS NOT null
                                     and F1.AFP = '工场店',
                                 CONCAT(
                                         F1.ads_shp_rev_shop_install_order_dtl_di_stat_date,
                                         '~',
                                         F1.AIK
                                 ),
                                 NULL
                                          )
                         )
                         ) AS D_SHP_01445,
                     null AS O_ORD_00199,
                     null AS O_TFC_00005,
                     if(F1.AAE is null, pow(2, 0), 0) as _grp_v
                 from
                     (
                         select
                             if(
                                     coalesce(concat('', vd1.province_name), '') in('', 'null'),
                                     '(null)',
                                     concat('', vd1.province_name)
                             ) as AAE,
                             vf.dim_shop_co_class_name as AFP,
                             vf.install_shopid as AIK,
                             substring(vf.dt, 1, 10) as _com_d,
                             vf.stat_date as ads_shp_rev_shop_install_order_dtl_di_stat_date,
                             substring(vf.dt, 1, 10) as dt,
                             null as O_ORD_00199,
                             null as O_ORD_00199_hb_ratio,
                             null as O_ORD_00199_tb_y_ratio,
                             vf.install_userid as O_SHP_00007,
                             null as O_TFC_00005,
                             null as O_TFC_00005_hb_ratio,
                             null as O_TFC_00005_tb_y_ratio
                         from
                             bi_olap.ads_shp_rev_shop_install_order_dtl_di vf
                                 left join bi_olap.dim_reg_city_f vd1 on vf.dim_city_name = vd1.city_name
                         WHERE
                             vf.dt between '2025-03-11'
                                 and '2025-03-25'
                     ) F1
                 group by
                     grouping sets ((F1.AAE))
             ) M1 FULL
                      JOIN (
                 select
                     F2.AAE AS AAE,
                     null AS D_SHP_01445_hb_ratio,
                     null AS O_ORD_00199_hb_ratio,
                     null AS O_TFC_00005_hb_ratio,
                     null AS O_TFC_00005_tb_y_ratio,
                     null AS O_ORD_00199_tb_y_ratio,
                     null AS D_SHP_01445_tb_y_ratio,
                     null AS D_SHP_01445,
                     null AS O_ORD_00199,
                     count(distinct F2.O_TFC_00005) AS O_TFC_00005,
                     if(F2.AAE is null, pow(2, 0), 0) as _grp_v
                 from
                     (
                         select
                             if(
                                     coalesce(concat('', vf.dim_province_name), '') in('', 'null'),
                                     '(null)',
                                     concat('', vf.dim_province_name)
                             ) as AAE,
                             substring(vf.dt, 1, 10) as _com_d,
                             substring(vf.dt, 1, 10) as dt,
                             null as O_ORD_00199,
                             null as O_ORD_00199_hb_ratio,
                             null as O_ORD_00199_tb_y_ratio,
                             vf.dau_deviceid as O_TFC_00005,
                             null as O_TFC_00005_hb_ratio,
                             null as O_TFC_00005_tb_y_ratio
                         from
                             bi_olap.ads_tfc_deviceid_active_dtl_di vf
                         WHERE
                             vf.dt between '2025-03-11'
                                 and '2025-03-25'
                     ) F2
                 group by
                     grouping sets ((F2.AAE))
             ) M2 ON (
                 coalesce(M1.AAE, '-9999') = coalesce(M2.AAE, '-9999')
                 ) FULL
                      JOIN (
                 select
                     F3.AAE AS AAE,
                     null AS D_SHP_01445_hb_ratio,
                     null AS O_ORD_00199_hb_ratio,
                     null AS O_TFC_00005_hb_ratio,
                     null AS O_TFC_00005_tb_y_ratio,
                     null AS O_ORD_00199_tb_y_ratio,
                     null AS D_SHP_01445_tb_y_ratio,
                     null AS D_SHP_01445,
                     sum(F3.O_ORD_00199) AS O_ORD_00199,
                     null AS O_TFC_00005,
                     if(F3.AAE is null, pow(2, 0), 0) as _grp_v
                 from
                     (
                         select
                             if(
                                     coalesce(concat('', vd1.province_name), '') in('', 'null'),
                                     '(null)',
                                     concat('', vd1.province_name)
                             ) as AAE,
                             vf.dim_shop_co_class_name as AFP,
                             vf.install_shopid as AIK,
                             substring(vf.dt, 1, 10) as _com_d,
                             substring(vf.dt, 1, 10) as dt,
                             vf.submit_gmv as O_ORD_00199,
                             null as O_ORD_00199_hb_ratio,
                             null as O_ORD_00199_tb_y_ratio,
                             null as O_TFC_00005,
                             null as O_TFC_00005_hb_ratio,
                             null as O_TFC_00005_tb_y_ratio
                         from
                             bi_olap.ads_ord_order_detail_dtl_di vf
                                 left join bi_olap.dim_reg_city_f vd1 on vf.dim_city_name = vd1.city_name
                                 and vf.dim_city_name = vd1.city_name
                         WHERE
                             vf.dt between '2025-03-11'
                                 and '2025-03-25'
                     ) F3
                 group by
                     grouping sets ((F3.AAE))
             ) M3 ON (
                 coalesce(M1.AAE, M2.AAE, '-9999') = coalesce(M3.AAE, '-9999')
                 )
     ),
     ds_hb as (
         select
             coalesce(M1.AAE, M2.AAE, M3.AAE) AS AAE,
             coalesce(M1.D_SHP_01445, M2.D_SHP_01445, M3.D_SHP_01445) AS D_SHP_01445,
             coalesce(M1.O_ORD_00199, M2.O_ORD_00199, M3.O_ORD_00199) AS O_ORD_00199,
             coalesce(M1.O_TFC_00005, M2.O_TFC_00005, M3.O_TFC_00005) AS O_TFC_00005,
             coalesce(M1._grp_v, M2._grp_v, M3._grp_v) as _grp_v
         FROM
             (
                 select
                     F1.AAE AS AAE,
                     count(distinct F1.O_SHP_00007) AS O_SHP_00007,
                     null AS D_SHP_01445_hb_ratio,
                     null AS O_ORD_00199_hb_ratio,
                     null AS O_TFC_00005_hb_ratio,
                     null AS O_TFC_00005_tb_y_ratio,
                     null AS O_ORD_00199_tb_y_ratio,
                     null AS D_SHP_01445_tb_y_ratio,
                     (
                         COUNT(
                                 DISTINCT IF(
                                 F1.O_SHP_00007 IS NOT null
                                     and F1.AFP = '工场店',
                                 CONCAT(
                                         F1.ads_shp_rev_shop_install_order_dtl_di_stat_date,
                                         '~',
                                         F1.AIK
                                 ),
                                 NULL
                                          )
                         )
                         ) AS D_SHP_01445,
                     null AS O_ORD_00199,
                     null AS O_TFC_00005,
                     if(F1.AAE is null, pow(2, 0), 0) as _grp_v
                 from
                     (
                         select
                             if(
                                     coalesce(concat('', vd1.province_name), '') in('', 'null'),
                                     '(null)',
                                     concat('', vd1.province_name)
                             ) as AAE,
                             vf.dim_shop_co_class_name as AFP,
                             vf.install_shopid as AIK,
                             substring(vf.dt, 1, 10) as _com_d,
                             vf.stat_date as ads_shp_rev_shop_install_order_dtl_di_stat_date,
                             substring(vf.dt, 1, 10) as dt,
                             null as O_ORD_00199,
                             null as O_ORD_00199_hb_ratio,
                             null as O_ORD_00199_tb_y_ratio,
                             vf.install_userid as O_SHP_00007,
                             null as O_TFC_00005,
                             null as O_TFC_00005_hb_ratio,
                             null as O_TFC_00005_tb_y_ratio
                         from
                             bi_olap.ads_shp_rev_shop_install_order_dtl_di vf
                                 left join bi_olap.dim_reg_city_f vd1 on vf.dim_city_name = vd1.city_name
                         WHERE
                             vf.dt between '2026-03-10'
                                 and '2026-03-24'
                     ) F1
                 group by
                     grouping sets ((F1.AAE))
             ) M1 FULL
                      JOIN (
                 select
                     F2.AAE AS AAE,
                     null AS D_SHP_01445_hb_ratio,
                     null AS O_ORD_00199_hb_ratio,
                     null AS O_TFC_00005_hb_ratio,
                     null AS O_TFC_00005_tb_y_ratio,
                     null AS O_ORD_00199_tb_y_ratio,
                     null AS D_SHP_01445_tb_y_ratio,
                     null AS D_SHP_01445,
                     null AS O_ORD_00199,
                     count(distinct F2.O_TFC_00005) AS O_TFC_00005,
                     if(F2.AAE is null, pow(2, 0), 0) as _grp_v
                 from
                     (
                         select
                             if(
                                     coalesce(concat('', vf.dim_province_name), '') in('', 'null'),
                                     '(null)',
                                     concat('', vf.dim_province_name)
                             ) as AAE,
                             substring(vf.dt, 1, 10) as _com_d,
                             substring(vf.dt, 1, 10) as dt,
                             null as O_ORD_00199,
                             null as O_ORD_00199_hb_ratio,
                             null as O_ORD_00199_tb_y_ratio,
                             vf.dau_deviceid as O_TFC_00005,
                             null as O_TFC_00005_hb_ratio,
                             null as O_TFC_00005_tb_y_ratio
                         from
                             bi_olap.ads_tfc_deviceid_active_dtl_di vf
                         WHERE
                             vf.dt between '2026-03-10'
                                 and '2026-03-24'
                     ) F2
                 group by
                     grouping sets ((F2.AAE))
             ) M2 ON (
                 coalesce(M1.AAE, '-9999') = coalesce(M2.AAE, '-9999')
                 ) FULL
                      JOIN (
                 select
                     F3.AAE AS AAE,
                     null AS D_SHP_01445_hb_ratio,
                     null AS O_ORD_00199_hb_ratio,
                     null AS O_TFC_00005_hb_ratio,
                     null AS O_TFC_00005_tb_y_ratio,
                     null AS O_ORD_00199_tb_y_ratio,
                     null AS D_SHP_01445_tb_y_ratio,
                     null AS D_SHP_01445,
                     sum(F3.O_ORD_00199) AS O_ORD_00199,
                     null AS O_TFC_00005,
                     if(F3.AAE is null, pow(2, 0), 0) as _grp_v
                 from
                     (
                         select
                             if(
                                     coalesce(concat('', vd1.province_name), '') in('', 'null'),
                                     '(null)',
                                     concat('', vd1.province_name)
                             ) as AAE,
                             vf.dim_shop_co_class_name as AFP,
                             vf.install_shopid as AIK,
                             substring(vf.dt, 1, 10) as _com_d,
                             substring(vf.dt, 1, 10) as dt,
                             vf.submit_gmv as O_ORD_00199,
                             null as O_ORD_00199_hb_ratio,
                             null as O_ORD_00199_tb_y_ratio,
                             null as O_TFC_00005,
                             null as O_TFC_00005_hb_ratio,
                             null as O_TFC_00005_tb_y_ratio
                         from
                             bi_olap.ads_ord_order_detail_dtl_di vf
                                 left join bi_olap.dim_reg_city_f vd1 on vf.dim_city_name = vd1.city_name
                                 and vf.dim_city_name = vd1.city_name
                         WHERE
                             vf.dt between '2026-03-10'
                                 and '2026-03-24'
                     ) F3
                 group by
                     grouping sets ((F3.AAE))
             ) M3 ON (
                 coalesce(M1.AAE, M2.AAE, '-9999') = coalesce(M3.AAE, '-9999')
                 )
     )
select
    coalesce(ds_cur.AAE, ds_tb_y.AAE, ds_hb.AAE) as AAE,
    ds_cur.D_SHP_01445,(
        (ds_cur.D_SHP_01445 - ds_tb_y.D_SHP_01445) * 1.000000 / abs(ds_tb_y.D_SHP_01445)
        ) as D_SHP_01445_tb_y_ratio,(
        (ds_cur.D_SHP_01445 - ds_hb.D_SHP_01445) * 1.000000 / abs(ds_hb.D_SHP_01445)
        ) as D_SHP_01445_hb_ratio,
    ds_cur.O_ORD_00199,(
        (ds_cur.O_ORD_00199 - ds_tb_y.O_ORD_00199) * 1.000000 / abs(ds_tb_y.O_ORD_00199)
        ) as O_ORD_00199_tb_y_ratio,(
        (ds_cur.O_ORD_00199 - ds_hb.O_ORD_00199) * 1.000000 / abs(ds_hb.O_ORD_00199)
        ) as O_ORD_00199_hb_ratio,
    ds_cur.O_TFC_00005,(
        (ds_cur.O_TFC_00005 - ds_tb_y.O_TFC_00005) * 1.000000 / abs(ds_tb_y.O_TFC_00005)
        ) as O_TFC_00005_tb_y_ratio,(
        (ds_cur.O_TFC_00005 - ds_hb.O_TFC_00005) * 1.000000 / abs(ds_hb.O_TFC_00005)
        ) as O_TFC_00005_hb_ratio,
    case
        coalesce(ds_cur._grp_v, ds_tb_y._grp_v, ds_hb._grp_v)
        when 1 then 'col_total'
        else null
        end as _grp_v,
    coalesce(ds_cur._grp_v, ds_tb_y._grp_v, ds_hb._grp_v) as _grp_k
from
    ds_cur ds_cur full
                      join ds_tb_y ds_tb_y on coalesce(ds_cur.AAE, '-9999') = coalesce(ds_tb_y.AAE, '-9999') full
                      join ds_hb ds_hb on coalesce(ds_cur.AAE, ds_tb_y.AAE, '-9999') = coalesce(ds_hb.AAE, '-9999')
ORDER BY
    /*sort*/
    (
        case
            AAE
            when '未知' then '97'
            when '' then '98'
            when '(null)' then '99'
            else concat('90', AAE)
            end
        ) asc nulls first