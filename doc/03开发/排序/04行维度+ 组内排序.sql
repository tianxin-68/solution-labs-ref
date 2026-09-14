-- 行维度 日期 + 城市 + 成本业务线
-- 列维度 无
-- 指标 提单GMV


--按  提单GMV_本期值 组内排序

/* ssm:doris|template */
with ds_cur as (
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
                grouping sets ((F1.dt, F1.AAA, F1.CGW),(F1.dt, F1.AAA),(F1.dt),())
        ) M1
)
select
    ds_cur.dt as dt,
    ds_cur.AAA as AAA,
    ds_cur.CGW as CGW,
    ds_cur.O_ORD_00199,(
        (ds_cur.O_ORD_00199 * 1.000000) /(
            case
                when ds_cur._grp_v = 3 then ds_cur.O_ORD_00199
                when ds_cur._grp_v = 1 then sum(if(ds_cur._grp_v = 3, ds_cur.O_ORD_00199, null)) over(partition by ds_cur.dt)
                when ds_cur._grp_v = 0 then sum(if(ds_cur._grp_v = 1, ds_cur.O_ORD_00199, null)) over(partition by ds_cur.dt, ds_cur.AAA)
                else null
                end
            )
        ) as O_ORD_00199_zb_cs_ratio,
    case
        ds_cur._grp_v
        when 7 then 'col_total'
        when 3 then 'col_subtotal'
        when 1 then 'col_subtotal'
        else null
        end as _grp_v,
    ds_cur._grp_v as _grp_k,
    RANK() OVER (PARTITION BY  ds_cur.dt,ds_cur.AAA ORDER BY ds_cur.O_ORD_00199 asc) AS O_ORD_00199_rank_num
from
    ds_cur ds_cur
ORDER BY
    /*sort*/
    if(_grp_k = 7, 1, 0) desc nulls last,

    O_ORD_00199_rank_num asc ,

    dt asc nulls first,(
        case
            AAA
            when '未知' then '97'
            when '' then '98'
            when '(null)' then '99'
            else concat('90', AAA)
            end
        ) asc nulls first,(
        case
            CGW
            when '轮胎' then '01'
            when '保养' then '02'
            when '蓄电池' then '03'
            when '维修' then '04'
            when '洗美' then '05'
            when '改装超市' then '06'
            when '深美容' then '07'
            when '其他' then '08'
            when '钣喷及事故件' then '09'
            when '轮毂' then '10'
            when '动力电池' then '11'
            when '道路救援' then '12'
            when '充电' then '13'
            when '加油' then '14'
            when '未知' then '97'
            when '' then '98'
            when '(null)' then '99'
            else concat('90', CGW)
            end
        ) asc nulls first,
    _grp_v asc nulls first