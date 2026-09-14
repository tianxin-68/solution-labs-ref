with ds1 as (
    select
        dt_ext_m AS dt_ext_m,
        dim_order_source_channel_category_lvl1_name AS dim_order_source_channel_category_lvl1_name,
        max(
                if(_pivot.businessline = '保养', _pivot.pay_user, null)
            ) as pay_user_c__d_0_0,
        max(
                if(_pivot.businessline = '保养', _pivot.pay_gmv, null)
            ) as pay_gmv_c__d_0_0,
        null as pay_gmv_zb_ct_ratio_c__d_0_0,
        max(
                if(_pivot.businessline = '轮胎', _pivot.pay_user, null)
            ) as pay_user_c__d_0_1,
        max(
                if(_pivot.businessline = '轮胎', _pivot.pay_gmv, null)
            ) as pay_gmv_c__d_0_1,

        -- 行总计
        max(
                if(_pivot._grp_v = 1 , _pivot.pay_gmv, null)
            ) as pay_gmv_c_total,
        max(
                if(_pivot._grp_v = 1 , _pivot.pay_user, null)
            ) as pay_user_total,

        -- 整表总计
        max(max(
                    if(  _pivot._grp_v = 7 , _pivot.pay_user, null)
                )) over() as pay_user_whole_total,
        max(max(
                    if( _pivot._grp_v = 7 , _pivot.pay_gmv, null)
                )) over() as pay_gmv_whole_total,
        null as pay_gmv_zb_ct_ratio_c__d_0_1,
        max(_pivot._grp_v) as _grp_v
    from
        (
            SELECT
                M1.dt_ext_m AS dt_ext_m,
                M1.dim_order_source_channel_category_lvl1_name AS dim_order_source_channel_category_lvl1_name,
                M1.businessline AS businessline,
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
                        F1.dt_ext_m,
                        F1.dim_order_source_channel_category_lvl1_name,
                        F1.businessline
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
                        --  常规分组
                        F1.dim_order_source_channel_category_lvl1_name,
                        F1.dt_ext_m,
                        F1.businessline
                        ),

                        (
                        -- 列小计
                        F1.dt_ext_m,
                        F1.businessline
                        ),
                        (
                        -- 行总计
                        F1.dim_order_source_channel_category_lvl1_name,
                        F1.dt_ext_m
                        )
                            ,
                        (
                        -- 列总计
                        F1.businessline
                        ),
                        (
                        -- 整表总计
                        )
                        )
                ) M1
        ) _pivot
    group by
        dt_ext_m,
        dim_order_source_channel_category_lvl1_name
)
select
    if(ds_cur._grp_v = 6, '列总计', cast(ds_cur.dt_ext_m as varchar) ) as dt_ext_m,
    if(ds_cur._grp_v  = 2, '列小计', cast(ds_cur.dim_order_source_channel_category_lvl1_name as varchar)) as dim_order_source_channel_category_lvl1_name,
    ds_cur.pay_user_c__d_0_0,
    ds_cur.pay_gmv_c__d_0_0,
    try(
                (ds_cur.pay_gmv_c__d_0_0 * 1.0000) /(
                    (
                        sum(
                                if(ds_cur._grp_v > 0, null, ds_cur.pay_gmv_c__d_0_0)
                            ) over()
                        ) * 1.0000
                )
        ) as pay_gmv_zb_ct_ratio_c__d_0_0,
    ds_cur.pay_user_c__d_0_1,
    ds_cur.pay_gmv_c__d_0_1,
    try(
                (ds_cur.pay_gmv_c__d_0_1 * 1.0000) /(
                    (
                        sum(
                                if(ds_cur._grp_v > 0, null, ds_cur.pay_gmv_c__d_0_1)
                            ) over()
                        ) * 1.0000
                )
        ) as pay_gmv_zb_ct_ratio_c__d_0_1,
    ds_cur._grp_v as _grp_v
from
    ds1 ds_cur
ORDER BY
    /*sort*/
    if(ds_cur._grp_v = 6, 1,0) desc,
    ds_cur.dt_ext_m asc,
    ds_cur.dim_order_source_channel_category_lvl1_name