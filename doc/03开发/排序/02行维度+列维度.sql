-- 行维度 日期 + 城市
-- 列维度 成本业务线
-- 指标 提单GMV

--按 日期降序 + 提单GMV 【加油】 降序

/* ssm:doris|template */
select
    dt as dt,
    AAA as AAA,
    CGW as CGW,
    _pivot.O_ORD_00199 as O_ORD_00199
from
    (
        select
            M1.dt AS dt,
            M1.AAA AS AAA,
            M1.CGW AS CGW,
            M1.O_ORD_00199 AS O_ORD_00199
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
                    grouping sets ((F1.dt, F1.AAA, F1.CGW))
            ) M1
    ) _pivot
where
    (
        _pivot.CGW in ('保养', '轮胎', '加油')
            or _pivot.CGW is null
        )
ORDER BY
    /*sort*/
    (case when CGW = '加油' then O_ORD_00199  else null end ) desc nulls last