-- 行维度 日期 + 城市
-- 列维度 成本业务线
-- 指标 提单GMV
-- 分析 列小计+列总计 + 占列小计

--按  提单GMV_本期值（列小计） 【轮胎】 降序
--   提单GMV_占列小计比列【保养】 降序

/* ssm:doris|template */
with ds_cur as (
    select
        dt as dt,
        AAA as AAA,
        CGW as CGW,
        _pivot.O_ORD_00199 as O_ORD_00199,
        null as O_ORD_00199_zb_cs_ratio,
        _pivot._grp_v as _grp_v
    from
        (
            select
                M1.dt AS dt,
                M1.AAA AS AAA,
                M1.CGW AS CGW,
                M1.O_ORD_00199 AS O_ORD_00199,
                M1._grp_v as _grp_v
            FROM
                (
                    select
                        F1.dt AS dt,
                        F1.AAA AS AAA,
                        F1.CGW AS CGW,
                        sum(F1.O_ORD_00199) AS O_ORD_00199,
                        if(F1.dt is null, pow(2, 2), 0) + if(F1.AAA is null, pow(2, 1), 0) + if(F1.CGW is null, pow(2, 0), 0) as _grp_v
                    from
                        (
                            select
                                if(
                                        coalesce(concat('', vf.dim_city_name), '') in('', 'null'),
                                        '(null)',
                                        concat('', vf.dim_city_name)
                                ) as AAA,
                                if(
                                        coalesce(concat('', vf.dim_cost_businessline), '') in('', 'null'),
                                        '(null)',
                                        concat('', vf.dim_cost_businessline)
                                ) as CGW,
                                substring(vf.dt, 1, 10) as _com_d,
                                substring(vf.dt, 1, 10) as dt,
                                vf.submit_gmv as O_ORD_00199
                            from
                                bi_olap.ads_ord_order_detail_dtl_di vf
                            WHERE
                                vf.dt between '2025-05-04'
                                    and '2025-05-05'
                              AND vf.dim_city_name IN ('上海市', '北京市')
                              AND vf.dim_cost_businessline IN ('轮胎', '保养', '加油')
                        ) F1
                    group by
                        grouping sets ((F1.dt, F1.AAA, F1.CGW),(F1.dt, F1.CGW),(F1.CGW))
                ) M1
        ) _pivot
    where
        (
            _pivot.CGW in ('保养', '轮胎', '加油')
                or _pivot.CGW is null
            )
    ORDER BY
        /*sort*/
        dt asc nulls last
)
select
    ds_cur.CGW as CGW,
    ds_cur.dt as dt,
    ds_cur.AAA as AAA,
    ds_cur.O_ORD_00199,(
        (ds_cur.O_ORD_00199 * 1.000000) /(
            case
                when ds_cur._grp_v = 2 then ds_cur.O_ORD_00199
                when ds_cur._grp_v = 0 then sum(if(ds_cur._grp_v = 2, ds_cur.O_ORD_00199, null)) over(partition by ds_cur.dt, ds_cur.CGW)
                else null
                end
            )
        ) as O_ORD_00199_zb_cs_ratio,
    case
        ds_cur._grp_v
        when 6 then 'col_total'
        when 2 then 'col_subtotal'
        when 7 then 'col_total'
        when 3 then 'col_subtotal'
        else null
        end as _grp_v,
    ds_cur._grp_v as _grp_k
from
    ds_cur ds_cur
ORDER BY
    /*sort*/
    if(_grp_k in (6, 7), 1, 0) desc nulls last,
    (case when CGW = '轮胎' and _grp_k = 2 then O_ORD_00199 else  null end ) asc nulls last,
    (case when CGW = '保养' then O_ORD_00199_zb_cs_ratio else  null end ) asc nulls last,
    _grp_v asc nulls first