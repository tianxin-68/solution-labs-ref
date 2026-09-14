with ds_cur as (
    select
        M2.dt AS dt,
        M2.dim_plat_type AS dim_plat_type,
        M2.dim_province_name AS dim_province_name,
        M2.submit_user AS submit_user,
        M2.pay_gmv AS pay_gmv,
        M2.business_page_deviceid AS business_page_deviceid,
        M2._grp_v as _grp_v
    FROM
        (
            select
                F2.dt AS dt,
                F2.dim_plat_type AS dim_plat_type,
                F2.dim_province_name AS dim_province_name,
                count(distinct F2.submit_user) AS submit_user,
                sum(F2.pay_gmv) AS pay_gmv,
                count(distinct F2.business_page_deviceid) AS business_page_deviceid,
                if(F2.dt is null, pow(2, 2), 0) + if(F2.dim_plat_type is null, pow(2, 1), 0) + if(F2.dim_province_name is null, pow(2, 0), 0) as _grp_v
            from
                (
                    select
                        substring(vf.dt, 1, 10) as _com_d,
                        vf.businessline as businessline,
                        if(
                                vf.dim_plat_type is null,
                                '(null)',
                                concat('', vf.dim_plat_type)
                            ) as dim_plat_type,
                        if(
                                vf.final_province is null,
                                '(null)',
                                concat('', vf.final_province)
                            ) as dim_province_name,
                        replace(substring(vf.dt, 1, 7), '-', '') as dt,
                        vf.business_page_deviceid as business_page_deviceid,
                        vf.pay_gmv as pay_gmv,
                        vf.submit_order_userid as submit_user
                    from
                        bi_hot.h_v_ads_zzqs_tfc_device_visit_trans_sum_d vf
                    WHERE
                        vf.dt between '2024-08-01'
                            and '2024-10-23'
                      AND vf.businessline IN ('保养', '洗美', '轮胎')
                ) F2
            group by
                grouping sets (
                (F2.dt, F2.dim_plat_type, F2.dim_province_name),(F2.dt, F2.dim_plat_type),(F2.dt),()
                )
        ) M2
),
     ds_hb as (
         select
             M2.dt AS dt,
             M2.dim_plat_type AS dim_plat_type,
             M2.dim_province_name AS dim_province_name,
             M2.submit_user AS submit_user,
             M2.pay_gmv AS pay_gmv,
             M2.business_page_deviceid AS business_page_deviceid,
             M2._grp_v as _grp_v,(bi_add_month(M2.dt, 'month', 1)) as dt_vs
         FROM
             (
                 select
                     F2.dt AS dt,
                     F2.dim_plat_type AS dim_plat_type,
                     F2.dim_province_name AS dim_province_name,
                     count(distinct F2.submit_user) AS submit_user,
                     sum(F2.pay_gmv) AS pay_gmv,
                     count(distinct F2.business_page_deviceid) AS business_page_deviceid,
                     if(F2.dt is null, pow(2, 2), 0) + if(F2.dim_plat_type is null, pow(2, 1), 0) + if(F2.dim_province_name is null, pow(2, 0), 0) as _grp_v
                 from
                     (
                         select
                             substring(vf.dt, 1, 10) as _com_d,
                             vf.businessline as businessline,
                             if(
                                     vf.dim_plat_type is null,
                                     '(null)',
                                     concat('', vf.dim_plat_type)
                                 ) as dim_plat_type,
                             if(
                                     vf.final_province is null,
                                     '(null)',
                                     concat('', vf.final_province)
                                 ) as dim_province_name,
                             replace(substring(vf.dt, 1, 7), '-', '') as dt,
                             vf.business_page_deviceid as business_page_deviceid,
                             vf.pay_gmv as pay_gmv,
                             vf.submit_order_userid as submit_user
                         from
                             bi_hot.h_v_ads_zzqs_tfc_device_visit_trans_sum_d vf
                         WHERE
                             vf.dt between '2024-07-01'
                                 and '2024-09-23'
                           AND vf.businessline IN ('保养', '洗美', '轮胎')
                     ) F2
                 group by
                     grouping sets (
                     (F2.dt, F2.dim_plat_type, F2.dim_province_name),(F2.dt, F2.dim_plat_type),(F2.dt),()
                     )
             ) M2
     )
select
    coalesce(ds_cur.dt, ds_hb.dt_vs) as dt,
    coalesce(ds_cur.dim_plat_type, ds_hb.dim_plat_type) as dim_plat_type,
    coalesce(ds_cur.dim_province_name, ds_hb.dim_province_name) as dim_province_name,
    ds_cur.submit_user,(
                (ds_cur.submit_user - ds_hb.submit_user) * 1.000000 / ds_hb.submit_user
        ) as submit_user_hb_ratio,
    ds_cur.pay_gmv,(
                (ds_cur.pay_gmv - ds_hb.pay_gmv) * 1.000000 / ds_hb.pay_gmv
        ) as pay_gmv_hb_ratio,
    ds_cur.business_page_deviceid,(
                (
                        ds_cur.business_page_deviceid - ds_hb.business_page_deviceid
                    ) * 1.000000 / ds_hb.business_page_deviceid
        ) as business_page_deviceid_hb_ratio,
    case
        coalesce(ds_cur._grp_v, ds_hb._grp_v)
        when 7 then 'col_total'
        when 3 then 'col_subtotal'
        when 1 then 'col_subtotal'
        else null
        end as _grp_v,
    coalesce(ds_cur._grp_v, ds_hb._grp_v) as _grp_k
from
    ds_cur ds_cur full
                      join ds_hb ds_hb on coalesce(ds_cur.dt, '-9999') = coalesce(ds_hb.dt_vs, '-9999')
        and coalesce(ds_cur.dim_plat_type, '-9999') = coalesce(ds_hb.dim_plat_type, '-9999')
        and coalesce(ds_cur.dim_province_name, '-9999') = coalesce(ds_hb.dim_province_name, '-9999')
ORDER BY
    /*sort*/
    if(_grp_k = 7, 1, 0) desc nulls last,
    dt asc nulls last,(
        case
            dim_plat_type
            when '未知' then '97'
            when '' then '98'
            when '(null)' then '99'
            else concat('90', dim_plat_type)
            end
        ) asc nulls last,(
        case
            dim_province_name
            when '未知' then '97'
            when '' then '98'
            when '(null)' then '99'
            else concat('90', dim_province_name)
            end
        ) asc nulls last,
    _grp_v asc nulls last
