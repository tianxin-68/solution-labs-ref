/* ssm:doris|template */
select
    M1.dt AS dt,
    M1.AAA AS AAA,
    M1.D_ORD_01324 AS D_ORD_01324,
    M1.D_ORD_01592 AS D_ORD_01592
FROM
    (
        select
            F1.dt AS dt,
            F1.AAA AS AAA,
            sum(F1.O_ORD_00014) AS O_ORD_00014,
            sum(F1.O_ORD_00062) AS O_ORD_00062,
            (
                sum(
                        case
                            when F1.ABA = '轮胎' then F1.O_ORD_00014
                            end
                )
                ) AS D_ORD_01324,
            (
                sum(
                        case
                            when F1.ABA = '轮胎' then F1.O_ORD_00062
                            end
                )
                ) AS D_ORD_01592,
            if(F1.dt is null, pow(2, 1), 0) + if(F1.AAA is null, pow(2, 0), 0) as _grp_v
        from
            (
                select
                    if(
                            vf.dim_city_name is null,
                            '(null)',
                            concat('', vf.dim_city_name)
                    ) as AAA,
                    vf.businessline as ABA,
                    substring(vf.dt, 1, 10) as _com_d,
                    --replace(substring(vf.dt, 1, 7), '-', '') as dt,
                    concat(substring(vf.dt, 1, 4),'-Q',quarter(vf.dt)) as dt,
                    vf.pay_gmv as O_ORD_00014,
                    vf.amt_b0_service as O_ORD_00062
                from
                    bi_olap.ads_ord_order_detail_dtl_di vf
                WHERE
                    vf.businessline IN ('保养', '轮胎')
                  AND vf.dt between '2024-07-01'
                    and '2024-12-31'
            ) F1
        group by
            grouping sets ((F1.dt, F1.AAA))
    ) M1
ORDER BY
    /*sort*/
    dt asc nulls last
    limit 1000