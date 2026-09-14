/* ssm-contributor */
with ds1 as (
    SELECT
        M1.dt_ext_m AS dt_ext_m,
        M1.businessline AS businessline,
        M1.dim_order_source_channel_category_lvl1_name AS dim_order_source_channel_category_lvl1_name,
        M1.pay_user AS pay_user,
        M1.pay_gmv AS pay_gmv,
        null AS pay_gmv_zb_ct_ratio,
        M1._grp_v as _grp_v
    FROM
        (
            SELECT
                F1.dt_ext_m AS dt_ext_m,
                F1.businessline AS businessline,
                F1.dim_order_source_channel_category_lvl1_name AS dim_order_source_channel_category_lvl1_name,
                count(DISTINCT F1.pay_user) AS pay_user,
                sum(F1.pay_gmv) AS pay_gmv,
                grouping(
                F1.businessline,
                F1.dim_order_source_channel_category_lvl1_name,
                F1.dt_ext_m
                ) as _grp_v
            from
                (
                select
                v.businessline as businessline,
                v.dim_order_source_channel_category_lvl1_name as dim_order_source_channel_category_lvl1_name,
                SUBSTR(v.order_date, 1, 10) as dt,
                REPLACE(SUBSTR(v.order_date, 1, 7), '-', '') as dt_ext_m,
                v.pay_gmv as pay_gmv,
                v.pay_user as pay_user
                from
                bi_view.v_ads_zzqs_ord_order_dtl_super_multi_d v
                WHERE
                (
                SUBSTR(v.order_date, 1, 10) between '2023-07-31'
                and '2023-08-06'
                )
                AND (v.businessline IN ('保养', '轮胎'))
                ) F1
            GROUP BY
                GROUPING SETS (
                (
                -- 常规分组
                F1.businessline,
                F1.dim_order_source_channel_category_lvl1_name,
                F1.dt_ext_m
                ),
                (
                -- 列小计1
                F1.businessline,
                F1.dt_ext_m
                ),
                (
                -- 列小计2
                F1.dt_ext_m
                ),
                (
                -- 列总计
                )
                )
        ) M1
)
select
    if(
                ds_cur._grp_v > 0,
                '列总计',
                cast(ds_cur.dt_ext_m as varchar)
        ) as dt_ext_m,
    ds_cur.businessline,
    ds_cur.dim_order_source_channel_category_lvl1_name,
    ds_cur.pay_user,
    ds_cur.pay_gmv,
    try(
                (ds_cur.pay_gmv * 1.0000) /(
                    (
                        sum(if(ds_cur._grp_v > 0, null, ds_cur.pay_gmv)) over()
                        ) * 1.0000
                )
        ) as pay_gmv_zb_ct_ratio,
    ds_cur._grp_v as _grp_v
from
    ds1 ds_cur
ORDER BY
    /*sort*/
    ds_cur._grp_v desc,
    ds_cur.dt_ext_m asc