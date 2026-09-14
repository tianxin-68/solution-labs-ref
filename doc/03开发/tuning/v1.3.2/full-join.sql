/* ssm:contributor|5c7cb7235c4d4bc7829dca61b1cf5cc9:测试fulljoin场景||doris */
with ds_cur as (
    select
        M1.dt AS dt,
        M1.dt AS dt_vs,
        M1.dim_order_source_channel_category_lvl1_name AS dim_order_source_channel_category_lvl1_name,
        M1.tire_spec AS tire_spec,
        M1.finish_order_gmv AS finish_order_gmv,
        M1.finish_order_orderid AS finish_order_orderid,
        null as finish_order_gmv_hb_r_value,
        null as finish_order_gmv_hb_ratio,
        null as finish_order_orderid_hb_r_value,
        null as finish_order_orderid_hb_ratio,
        M1._grp_v as _grp_v
    FROM
        (
            select
                F1.dt AS dt,
                F1.dim_order_source_channel_category_lvl1_name AS dim_order_source_channel_category_lvl1_name,
                F1.tire_spec AS tire_spec,
                sum(F1.finish_order_gmv) AS finish_order_gmv,
                count(distinct F1.finish_order_orderid) AS finish_order_orderid,
                bi_grouping_id(
                        array(
                                F1.dt,
                                F1.dim_order_source_channel_category_lvl1_name,
                                F1.tire_spec
                            )
                    ) as _grp_v
            from
                (
                    select
                        substring(vf.dt, 1, 10) as _com_d,
                        if(
                                vf.dim_order_source_channel_category_lvl1_name is null,
                                '(null)',
                                concat(
                                        '',
                                        vf.dim_order_source_channel_category_lvl1_name
                                    )
                            ) as dim_order_source_channel_category_lvl1_name,
                        substring(vf.dt, 1, 10) as dt,
                        if(
                                vd1.tire_spec is null,
                                '(null)',
                                concat('', vd1.tire_spec)
                            ) as tire_spec,
                        vf.finish_order_gmv as finish_order_gmv,
                        vf.finish_order_orderid as finish_order_orderid
                    from
                        bi_view.v_ads_olap_ord_order_dtl_di_tire vf
                            left join bi_view.v_ads_olap_prd_product_dtl_f vd1 on vf.prdid = vd1.prdid
                    WHERE
                        vf.dt between '2024-05-29'
                            and '2024-05-30'
                      AND (vd1.tire_spec IN ('275/55r19', '245/40r17'))
                ) F1
            group by
                grouping sets (
                (
                F1.dt,
                F1.dim_order_source_channel_category_lvl1_name,
                F1.tire_spec
                ),(
                F1.dt,
                F1.dim_order_source_channel_category_lvl1_name
                ),(F1.dt),()
                )
        ) M1
),
ds_hb as (
         select
             M1.dt AS dt,
             date_format(
                     date_add(
                             str_to_date(M1.dt, '%Y-%m-%d'),
                             interval 1 DAY
                         ),
                     '%Y-%m-%d'
                 ) as dt_vs,
             M1.dim_order_source_channel_category_lvl1_name AS dim_order_source_channel_category_lvl1_name,
             M1.tire_spec AS tire_spec,
             M1.finish_order_gmv AS finish_order_gmv,
             M1.finish_order_orderid AS finish_order_orderid,
             null as finish_order_gmv_hb_r_value,
             null as finish_order_gmv_hb_ratio,
             null as finish_order_orderid_hb_r_value,
             null as finish_order_orderid_hb_ratio,
             M1._grp_v as _grp_v
         FROM
             (
                 select
                     F1.dt AS dt,
                     F1.dim_order_source_channel_category_lvl1_name AS dim_order_source_channel_category_lvl1_name,
                     F1.tire_spec AS tire_spec,
                     sum(F1.finish_order_gmv) AS finish_order_gmv,
                     count(distinct F1.finish_order_orderid) AS finish_order_orderid,
                     bi_grouping_id(
                             array(
                                     F1.dt,
                                     F1.dim_order_source_channel_category_lvl1_name,
                                     F1.tire_spec
                                 )
                         ) as _grp_v
                 from
                     (
                         select
                             substring(vf.dt, 1, 10) as _com_d,
                             if(
                                     vf.dim_order_source_channel_category_lvl1_name is null,
                                     '(null)',
                                     concat(
                                             '',
                                             vf.dim_order_source_channel_category_lvl1_name
                                         )
                                 ) as dim_order_source_channel_category_lvl1_name,
                             substring(vf.dt, 1, 10) as dt,
                             if(
                                     vd1.tire_spec is null,
                                     '(null)',
                                     concat('', vd1.tire_spec)
                                 ) as tire_spec,
                             vf.finish_order_gmv as finish_order_gmv,
                             vf.finish_order_orderid as finish_order_orderid
                         from
                             bi_view.v_ads_olap_ord_order_dtl_di_tire vf
                                 left join bi_view.v_ads_olap_prd_product_dtl_f vd1 on vf.prdid = vd1.prdid
                         WHERE
                             vf.dt between '2024-05-28'
                                 and '2024-05-29'
                           AND (vd1.tire_spec IN ('275/55r19', '245/40r17'))
                     ) F1
                 group by
                     grouping sets (
                     (
                     F1.dt,
                     F1.dim_order_source_channel_category_lvl1_name,
                     F1.tire_spec
                     ),(
                     F1.dt,
                     F1.dim_order_source_channel_category_lvl1_name
                     ),(F1.dt),()
                     )
             ) M1
     )
select
    COALESCE(ds_cur.dt_vs, ds_hb.dt_vs) as dt,
    COALESCE(ds_cur.dim_order_source_channel_category_lvl1_name, ds_hb.dim_order_source_channel_category_lvl1_name) as dim_order_source_channel_category_lvl1_name,
    COALESCE(ds_cur.tire_spec, ds_hb.tire_spec) as tire_spec,
    ds_cur.finish_order_gmv,
    ds_hb.finish_order_gmv as finish_order_gmv_hb_r_value,(
        bi_division(
                    COALESCE(ds_cur.finish_order_gmv, 0) - COALESCE(ds_hb.finish_order_gmv, 0),
                    COALESCE(ds_hb.finish_order_gmv, 0),
                    0
            )
        ) as finish_order_gmv_hb_ratio,
    ds_cur.finish_order_orderid,
    ds_hb.finish_order_orderid as finish_order_orderid_hb_r_value,(
        bi_division(
                    COALESCE(ds_cur.finish_order_orderid, 0) - COALESCE(ds_hb.finish_order_orderid, 0),
                    COALESCE(ds_hb.finish_order_orderid, 0),
                    0
            )
        ) as finish_order_orderid_hb_ratio,
    case
        COALESCE(ds_cur._grp_v, ds_hb._grp_v)
        when 7 then 'col_total'
        when 3 then 'col_subtotal'
        when 1 then 'col_subtotal'
        else null
        end as _grp_v,
    COALESCE(ds_cur._grp_v,ds_hb._grp_v) as _grp_k
from
    ds_cur ds_cur
    full join ds_hb ds_hb on coalesce(ds_cur.dt_vs, '-9999') = coalesce(ds_hb.dt_vs, '-9999')
    and coalesce(
    ds_cur.dim_order_source_channel_category_lvl1_name,
    '-9999'
    ) = coalesce(
    ds_hb.dim_order_source_channel_category_lvl1_name,
    '-9999'
    )
    and coalesce(ds_cur.tire_spec, '-9999') = coalesce(ds_hb.tire_spec, '-9999')
ORDER BY
    /*sort*/
    dt asc nulls last,(
    case
    dim_order_source_channel_category_lvl1_name
    when '线上自有平台' then '1'
    when '线下门店' then '2'
    when '汽配龙' then '3'
    when '大客户' then '4'
    when '第三方' then '5'
    when '其他' then '6'
    when '未知' then '97'
    when '' then '98'
    when '(null)' then '99'
    else concat(
         '90',
    dim_order_source_channel_category_lvl1_name
    )
    end
    ) asc nulls last,(
    case
    tire_spec
    when '未知' then '97'
    when '' then '98'
    when '(null)' then '99'
    else concat('90', tire_spec)
    end
    ) asc nulls last,
    _grp_v asc nulls last