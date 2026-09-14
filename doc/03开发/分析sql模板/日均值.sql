/* ssm-contributor(8430a9e146bd47cca7d390e30e730cc0:debug：v2.3.1) */
with ds1 as (
    SELECT
        M1.dt AS dt,
        M1.businessline AS businessline,
        M1.pay_gmv AS pay_gmv,
        M1.pay_user AS pay_user,
        M1.pay_order AS pay_order,
        M1._grp_v as _grp_v
    FROM
        (
            select
                -- *****将*****
                coalesce(M1_0.dt, M1_1.dt) AS dt,
                coalesce(M1_0.businessline, M1_1.businessline) AS businessline,
                coalesce(M1_0.pay_gmv, M1_1.pay_gmv) AS pay_gmv,
                coalesce(M1_0.pay_user, M1_1.pay_user) AS pay_user,
                coalesce(M1_0.pay_order, M1_1.pay_order) AS pay_order,
                coalesce(M1_0._grp_v, M1_1._grp_v) as _grp_v
            from (
                -- *****不需要去重的指标日均 + 去重指标sql*****
                SELECT
                    F1.dt AS dt,
                    F1.businessline AS businessline,
                    ((sum(f1.pay_gmv)) * 1.0000 / 7) AS pay_gmv,
                    count(DISTINCT F1.pay_user) AS pay_user,
                    null as pay_order,
                    grouping(F1.dt, F1.businessline) as _grp_v
                from
                    (
                    select
                        coalesce(v.businessline, '(null)') as businessline,
                        bi_get_week_id(v.order_date) as dt,
                        v.pay_gmv as pay_gmv,
                        v.pay_order as pay_order,
                        v.pay_user as pay_user
                    from
                        bi_view.v_ads_zzqs_ord_order_dtl_super_multi_d v
                    WHERE
                        (v.businessline IN ('轮胎', '保养'))
                        AND (
                        bi_get_week_id(v.order_date) between '202336'
                        and '202337'
                        )
                    ) F1
                group by
                    grouping sets ((F1.businessline, F1.dt),(F1.dt),())
            ) M1_0 full join (
               -- *****需要日均的指标sql*****
                SELECT
                    F1.dt AS dt,
                    F1.businessline AS businessline,
                    null AS pay_gmv,
                    null pay_user,
               -- *****直接sum*****
                    sum(F1.pay_order)*1.000/7 AS pay_order,
                    grouping(F1.dt, F1.businessline) as _grp_v
                from
               (
               -- *****先按日粒度聚合去重*****
                    select
                    v2.businessline,
                    v2.dt_d,
                    v2.dt,
               -- *****此处聚合为原始聚合类型*****
                    count(distinct v2.pay_order) as pay_order
                    from (
               -- *****前台原始sql*****
                        select
                            coalesce(v.businessline, '(null)') as businessline,
               -- *****需要把最细粒度的日期带出来*****
                            v.order_date as _com_d,
                            bi_get_week_id(v.order_date) as dt,
                            v.pay_gmv as pay_gmv,
                            v.pay_order as pay_order,
                            v.pay_user as pay_user
                        from bi_view.v_ads_zzqs_ord_order_dtl_super_multi_d v
                    WHERE
                       (v.businessline IN ('轮胎', '保养'))
                        AND (
                        bi_get_week_id(v.order_date) between '202336'
                        and '202337'
                        )
                    ) v2
               -- *****按最细粒度group by*****
                        group by businessline, _com_d, dt
                ) F1
                group by
                    grouping sets ((F1.businessline, F1.dt),(F1.dt),())
                ) M1_1
            on
                -- *****按相同维度join*****
                coalesce(M1_0.dt, '-9999') = coalesce(M1_1.dt, '-9999')
                and coalesce(M1_0.businessline, '-9999') = coalesce(M1_1.businessline, '-9999')
                and M1_0._grp_v = M1_1._grp_v

        ) M1
)
select
    if(ds_cur._grp_v = 3, '列总计', ds_cur.dt) as dt,
    if(ds_cur._grp_v = 1, '列小计', ds_cur.businessline) as businessline,
    ds_cur.pay_gmv,
    ds_cur.pay_user,
    ds_cur.pay_order,
    case
        when ds_cur._grp_v = 3 then 'col_total'
        when ds_cur._grp_v = 1 then 'col_subtotal'
        else null
        end as _grp_v
from
    ds1 ds_cur
ORDER BY
    /*sort*/
    ds_cur.dt asc,
    ds_cur._grp_v asc,
    ds_cur.pay_gmv desc