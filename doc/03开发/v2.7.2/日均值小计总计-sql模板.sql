/* ssm:contributor|dfhaeZN3RDEEJllv1atga|doris */
with ds_cur as (
    select
        M1.dim_city_level AS dim_city_level,
        M1.price_range AS price_range,
        M1.key_page_deviceid_avg_by_d AS key_page_deviceid_avg_by_d,
        M1._grp_v as _grp_v
    FROM
        (
            select
                coalesce(M1_Com.dim_city_level, M1_Dst.dim_city_level) AS dim_city_level,
                coalesce(M1_Com.price_range, M1_Dst.price_range) AS price_range,
                case
                    when M1_Dst._grp_v = 0 then
                        M1_Dst.key_page_deviceid_avg_by_d
                    else
                        M1_Dst2.key_page_deviceid_avg_by_d
                    end AS key_page_deviceid_avg_by_d,
                coalesce(M1_Com._grp_v, M1_Dst._grp_v) as _grp_v
            from
                (
                    select
                        F1.dim_city_level AS dim_city_level,
                        F1.price_range AS price_range,
                        null AS key_page_deviceid_avg_by_d,
                        bi_grouping_id(array(F1.dim_city_level, F1.price_range)) as _grp_v
                    from
                        (
                            select
                                substring(vf.dt, 1, 10) as _com_d,
                                if(
                                        vd2.city_level is null,
                                        '(null)',
                                        concat('', vd2.city_level)
                                    ) as dim_city_level,
                                substring(vf.dt, 1, 10) as dt,
                                if(
                                        vd1.price_range is null,
                                        '(null)',
                                        concat('', vd1.price_range)
                                    ) as price_range,
                                null as key_page_deviceid_avg_by_d
                            from
                                bi_view.v_ads_olap_tfc_page_visit_dtl_di_tire vf
                                    left join bi_view.v_ads_olap_usr_vcl_vehicle_dtl_f vd1 on vf.dim_tid = vd1.dim_tid
                                    left join bi_view.v_ads_olap_reg_country_code_f_city vd2 on vf.dim_city_name = vd2.city_name
                            WHERE
                                vf.dt between '2024-05-28'
                                    and '2024-06-04'
                              AND (vd1.price_range IN ('18-28万', '28-40万', '40-60万'))
                              AND (vd2.city_level IN ('一线城市', '新一线城市', '二线城市'))
                        ) F1
                    group by
                        grouping sets (
                        (F1.price_range, F1.dim_city_level),(F1.dim_city_level),()
                        )
                ) M1_Com full
                join (
                select
                F1.dim_city_level AS dim_city_level,
                F1.price_range AS price_range,((sum(key_page_deviceid_avg_by_d)) * 1.0000 / 8) AS key_page_deviceid_avg_by_d,
                bi_grouping_id(array(F1.dim_city_level, F1.price_range)) as _grp_v
                from
                (
                select
                v2.dim_city_level as dim_city_level,
                v2.price_range as price_range,
                count(distinct v2.key_page_deviceid_avg_by_d) as key_page_deviceid_avg_by_d,
                v2._com_d
                from
                (
                select
                substring(vf.dt, 1, 10) as _com_d,
                if(
                vd2.city_level is null,
                '(null)',
                concat('', vd2.city_level)
                ) as dim_city_level,
                substring(vf.dt, 1, 10) as dt,
                if(
                vd1.price_range is null,
                '(null)',
                concat('', vd1.price_range)
                ) as price_range,
                vf.key_page_deviceid as key_page_deviceid_avg_by_d
                from
                bi_view.v_ads_olap_tfc_page_visit_dtl_di_tire vf
                left join bi_view.v_ads_olap_usr_vcl_vehicle_dtl_f vd1 on vf.dim_tid = vd1.dim_tid
                left join bi_view.v_ads_olap_reg_country_code_f_city vd2 on vf.dim_city_name = vd2.city_name
                WHERE
                vf.dt between '2024-05-28'
                and '2024-06-04'
                AND (vd1.price_range IN ('18-28万', '28-40万', '40-60万'))
                AND (vd2.city_level IN ('一线城市', '新一线城市', '二线城市'))
                ) v2
                GROUP BY
                v2.dim_city_level,
                v2.price_range,
                v2._com_d
                ) F1
                group by
                grouping sets (
                (F1.price_range, F1.dim_city_level),(F1.dim_city_level),()
                )
                ) M1_Dst on coalesce(M1_Com.dim_city_level, '-9999') = coalesce(M1_Dst.dim_city_level, '-9999')
                and coalesce(M1_Com.price_range, '-9999') = coalesce(M1_Dst.price_range, '-9999')
                and M1_Com._grp_v = M1_Dst._grp_v
                full join (
        /********************* 此处添加小计总计子查询 *******************/
                    select
                    F1.dim_city_level AS dim_city_level,
                    ((sum(key_page_deviceid_avg_by_d)) * 1.0000 / 8) AS key_page_deviceid_avg_by_d,
                    bi_grouping_id(array(F1.dim_city_level)) as _grp_v
                    from
                    (
                    select
                    v2._com_d,
                    v2.dim_city_level as dim_city_level,
                    count(distinct v2.key_page_deviceid_avg_by_d) as key_page_deviceid_avg_by_d,
                    bi_grouping_id(array(v2._com_d,v2.dim_city_level)) as _grp_v
                    from
                    (
                    select
                    substring(vf.dt, 1, 10) as _com_d,
                    if(
                    vd2.city_level is null,
                    '(null)',
                    concat('', vd2.city_level)
                    ) as dim_city_level,
                    substring(vf.dt, 1, 10) as dt,
                    if(
                    vd1.price_range is null,
                    '(null)',
                    concat('', vd1.price_range)
                    ) as price_range,
                    vf.key_page_deviceid as key_page_deviceid_avg_by_d
                    from
                    bi_view.v_ads_olap_tfc_page_visit_dtl_di_tire vf
                    left join bi_view.v_ads_olap_usr_vcl_vehicle_dtl_f vd1 on vf.dim_tid = vd1.dim_tid
                    left join bi_view.v_ads_olap_reg_country_code_f_city vd2 on vf.dim_city_name = vd2.city_name
                    WHERE
                    vf.dt between '2024-05-28'
                    and '2024-06-04'
                    AND (vd1.price_range IN ('18-28万', '28-40万', '40-60万'))
                    AND (vd2.city_level IN ('一线城市', '新一线城市', '二线城市'))
                    ) v2
                /********************* 此处添加小计总计维度+日期维度 grouping sets *******************/
                    GROUP BY grouping sets(
                    (
                    v2.dim_city_level,
                    v2._com_d
                    ),
                    (
                    v2._com_d
                    )
                    )

                ) F1
                group by
                grouping sets (
                /********************* 此处添加小计总计维度grouping sets *******************/
                (F1.dim_city_level)
                )
                ) M1_Dst2
                on coalesce(M1_Dst.dim_city_level, '-9999') = coalesce(M1_Dst2.dim_city_level, '-9999')
        ) M1
)
select
    if(ds_cur._grp_v = 3, '列总计', ds_cur.dim_city_level) as dim_city_level,
    if(ds_cur._grp_v = 1, '列小计', ds_cur.price_range) as price_range,
    ds_cur.key_page_deviceid_avg_by_d,
    case
        when ds_cur._grp_v = 3 then 'col_total'
        when ds_cur._grp_v = 1 then 'col_subtotal'
        else null
        end as _grp_v,
    ds_cur._grp_v as _grp_k
from
    ds_cur ds_cur
ORDER BY
    /*sort*/
    ds_cur.dim_city_level asc nulls last,
    ds_cur._grp_v asc nulls last,
    ds_cur.key_page_deviceid_avg_by_d desc nulls last