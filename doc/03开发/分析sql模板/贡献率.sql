/* ssm-contributor */
with ds1 as (
    SELECT
        M1.dt AS dt,
        M1.businessline AS businessline,
        M1.dim_order_source_channel_category_lvl1_name AS dim_order_source_channel_category_lvl1_name,
        M1.submit_gmv AS submit_gmv,
        null as submit_gmv_hb_ratio,
        null as submit_gmv_hb_value,
        M1._grp_v as _grp_v
    FROM
        (
            SELECT
                F1.dt AS dt,
                F1.businessline AS businessline,
                F1.dim_order_source_channel_category_lvl1_name AS dim_order_source_channel_category_lvl1_name,
                sum(F1.submit_gmv) AS submit_gmv,
                grouping(
                F1.dt,
                F1.businessline,
                F1.dim_order_source_channel_category_lvl1_name
                ) as _grp_v
            from
                (
                select
                coalesce(v.businessline, '(null)') as businessline,
                coalesce(
                v.dim_order_source_channel_category_lvl1_name,
                                         '(null)'
                ) as dim_order_source_channel_category_lvl1_name,
                SUBSTR(v.order_date, 1, 10) as dt,
                v.submit_gmv as submit_gmv
                from
                bi_view.v_ads_zzqs_ord_order_dtl_super_multi_d v
                WHERE
                (v.businessline IN ('保养', '轮胎'))
                AND (
                SUBSTR(v.order_date, 1, 10) between '2023-09-01'
                and '2023-09-05'
                )
                ) F1
            group by
                grouping sets (
                (
                F1.businessline,
                F1.dim_order_source_channel_category_lvl1_name,
                F1.dt
                ),(F1.dt, F1.businessline),(F1.dt)
                )
        ) M1
),
     ds2 as (
         SELECT
             M1.dt AS dt,
             M1.businessline AS businessline,
             M1.dim_order_source_channel_category_lvl1_name AS dim_order_source_channel_category_lvl1_name,
             M1.submit_gmv AS submit_gmv,
             null as submit_gmv_hb_ratio,
             null as submit_gmv_hb_value,
             M1._grp_v as _grp_v
         FROM
             (
                 SELECT
                     F1.dt AS dt,
                     F1.businessline AS businessline,
                     F1.dim_order_source_channel_category_lvl1_name AS dim_order_source_channel_category_lvl1_name,
                     sum(F1.submit_gmv) AS submit_gmv,
                     grouping(
                     F1.dt,
                     F1.businessline,
                     F1.dim_order_source_channel_category_lvl1_name
                     ) as _grp_v
                 from
                    (
                     select
                     coalesce(v.businessline, '(null)') as businessline,
                     coalesce(
                     v.dim_order_source_channel_category_lvl1_name,
                     '(null)'
                     ) as dim_order_source_channel_category_lvl1_name,
                     SUBSTR(v.order_date, 1, 10) as dt,
                     v.submit_gmv as submit_gmv
                     from
                     bi_view.v_ads_zzqs_ord_order_dtl_super_multi_d v
                     WHERE
                    (v.businessline IN ('保养', '轮胎'))
                     AND (
                    (
                     SUBSTR(v.order_date, 1, 10) between '2023-09-01'
                     and '2023-09-05'
                     )
                     or (
                     SUBSTR(v.order_date, 1, 10) between '2023-08-31'
                     and '2023-08-31'
                     )
                     )
                     ) F1
                 group by
                     grouping sets (
                     (
                     F1.businessline,
                     F1.dim_order_source_channel_category_lvl1_name,
                     F1.dt
                     ),(F1.dt, F1.businessline),(F1.dt)
                     )
             ) M1
     )
select
    if(ds_cur._grp_v = 7, '列总计', ds_cur.dt) as dt,
    if(ds_cur._grp_v = 3, '列小计', ds_cur.businessline) as businessline,
    if(
                ds_cur._grp_v = 1,
                '列小计',
                ds_cur.dim_order_source_channel_category_lvl1_name
        ) as dim_order_source_channel_category_lvl1_name,
    ds_cur.submit_gmv,
    try(
                    (ds_cur.submit_gmv * 1.0000) /(ds_hb.submit_gmv * 1.0000) - 1
        ) as submit_gmv_hb_ratio,
    try(ds_cur.submit_gmv - ds_hb.submit_gmv) as submit_gmv_hb_value,
    -- 贡献率分母
    case
        when ds_cur._grp_v = 0 then
            sum(if(ds_cur._grp_v = 0, ds_cur.submit_gmv - ds_hb.submit_gmv, null)) over(partition by ds_cur.dt, ds_cur.businessline)
        when ds_cur._grp_v = 1 then
            sum(if(ds_cur._grp_v = 0, ds_cur.submit_gmv - ds_hb.submit_gmv, null)) over(partition by ds_cur.dt)
        when ds_cur._grp_v = 3 then
            sum(if(ds_cur._grp_v = 0, ds_cur.submit_gmv - ds_hb.submit_gmv, null)) over()
        else null
        end   as submit_gmv_xj,
    -- 贡献率计算表达式
    try(
                    (ds_cur.submit_gmv - ds_hb.submit_gmv) * 1.0000/
                    (
                        case
                            when ds_cur._grp_v = 0 then
                                sum(if(ds_cur._grp_v = 0, ds_cur.submit_gmv - ds_hb.submit_gmv, null)) over(partition by ds_cur.dt, ds_cur.businessline)
                            when ds_cur._grp_v = 1 then
                                sum(if(ds_cur._grp_v = 0, ds_cur.submit_gmv - ds_hb.submit_gmv, null)) over(partition by ds_cur.dt)
                            when ds_cur._grp_v = 3 then
                                sum(if(ds_cur._grp_v = 0, ds_cur.submit_gmv - ds_hb.submit_gmv, null)) over()
                            else null
                            end
                        )
        ) as submit_gmv_hb_gxl,

    case
        when ds_cur._grp_v = 7 then 'col_total'
        when ds_cur._grp_v = 3 then 'col_subtotal'
        when ds_cur._grp_v = 1 then 'col_subtotal'
        else null
        end as _grp_v
from
    ds1 ds_cur
        left join ds2 ds_hb on ds_cur.dt = date_format(
            date_add('day', 1, date_parse(ds_hb.dt, '%Y-%m-%d')),
            '%Y-%m-%d'
        )
        and coalesce(ds_cur.businessline, '-9999') = coalesce(ds_hb.businessline, '-9999')
        and coalesce(
                    ds_cur.dim_order_source_channel_category_lvl1_name,
                    '-9999'
                ) = coalesce(
                    ds_hb.dim_order_source_channel_category_lvl1_name,
                    '-9999'
                )
ORDER BY
    /*sort*/
    ds_cur.dt asc,
    ds_cur.businessline asc,
    ds_cur._grp_v asc,
    ds_cur.submit_gmv desc