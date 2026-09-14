/* ssm:contributor|trino|template */
select
    M1.dt AS dt,
    M1.businessline AS businessline,
    M1.pay_orderid AS pay_orderid,
    M1.pay_gmv AS pay_gmv
FROM
    (
        select
            F1.dt AS dt,
            F1.businessline AS businessline,
            count(distinct F1.pay_orderid) AS pay_orderid,
            sum(F1.pay_gmv) AS pay_gmv,
            grouping(F1.dt, F1.businessline) as _grp_v
        from
            (
                select
                    SUBSTR(vf.order_date, 1, 10) as _com_d,
                    if(
                            vf.businessline is null,
                            '(null)',
                            format('%s', vf.businessline)
                    ) as businessline,
                    --REPLACE(SUBSTR(vf.order_date, 1, 7), '-', '') as dt,
                    cast(quarter(CAST(vf.order_date as date)) as varchar),
                    concat(substring(vf.order_date, 1, 4),'-Q',cast(quarter(date_parse(REPLACE(SUBSTR(vf.order_date, 1, 7), '-', ''), '%Y%m')) as varchar)) as dt,
                    vf.pay_gmv as pay_gmv,
                    vf.pay_orderid as pay_orderid
                from
                    bi_view.v_ads_zzqs_ord_order_dtl_super_multi_d vf
                WHERE
                    vf.order_date between '2024-07-01'
                        and '2024-12-31'
                  AND vf.businessline IN ('轮胎', '保养')
            ) F1
        group by
            grouping sets ((F1.dt, F1.businessline))
    ) M1
ORDER BY
    /*sort*/
    dt asc nulls last
    limit 1000;
