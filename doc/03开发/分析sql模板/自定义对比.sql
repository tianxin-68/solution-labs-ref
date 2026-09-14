--日
/* ssm-contributor */
with ds1 as (
  SELECT
    M1.dt AS dt,
    M1.businessline AS businessline,
    M1.finish_gmv AS finish_gmv,
    null AS finish_gmv_hb_ratio,
    null AS finish_gmv_hb_value,
    M1._grp_v as _grp_v
  FROM
    (
      SELECT
        F1.dt AS dt,
        F1.businessline AS businessline,
        sum(F1.finish_gmv) AS finish_gmv,
        grouping(F1.dt, F1.businessline) as _grp_v
      from
        (
          select
            coalesce(v.businessline, '(null)') as businessline,
            coalesce(
              v.dim_order_source_channel_category_lvl1_name,
              '(null)'
            ) as dim_order_source_channel_category_lvl1_name,
            SUBSTR(v.order_date, 1, 10) as dt,
            v.finish_gmv as finish_gmv
          from
            bi_view.v_ads_zzqs_ord_order_dtl_super_multi_d v
          WHERE
            (
              SUBSTR(v.order_date, 1, 10) between '2023-08-21' and '2023-08-22'
            )
        ) F1
      GROUP BY
        F1.dt,
        F1.businessline
    ) M1
),
ds2 as (
  SELECT
    M1.dt AS dt,
    M1.businessline AS businessline,
    M1.finish_gmv AS finish_gmv,
    null AS finish_gmv_db_ratio,
    null AS finish_gmv_db_value,
    M1._grp_v as _grp_v
  FROM
    (
      SELECT
        F1.dt AS dt,
        F1.businessline AS businessline,
        sum(F1.finish_gmv) AS finish_gmv,
        grouping(F1.dt, F1.businessline) as _grp_v
      from
        (
          select
            coalesce(v.businessline, '(null)') as businessline,
            coalesce(
              v.dim_order_source_channel_category_lvl1_name,
              '(null)'
            ) as dim_order_source_channel_category_lvl1_name,
            SUBSTR(v.order_date, 1, 10) as dt,
            v.finish_gmv as finish_gmv
          from
            bi_view.v_ads_zzqs_ord_order_dtl_super_multi_d v
          WHERE
            (
              (
                SUBSTR(v.order_date, 1, 10) between '2023-07-20' and '2023-07-21'
              )
              or (
                SUBSTR(v.order_date, 1, 10) between '2023-08-20' and '2023-08-20'
              )
            )
        ) F1
      GROUP BY
        F1.dt,
        F1.businessline
    ) M1
)
select
  if(ds_cur._grp_v = 3, '列总计', ds_cur.dt) as dt,
  if(ds_cur._grp_v = 1, '列小计', ds_cur.businessline) as businessline,
  ds_cur.finish_gmv,
  try(
    (ds_cur.finish_gmv * 1.0000) / (ds_db.finish_gmv * 1.0000) - 1
  ) as finish_gmv_db_ratio,
  try(ds_cur.finish_gmv - ds_db.finish_gmv) as finish_gmv_db_value,
  case
    when ds_cur._grp_v = 3 then 'col_total'
    when ds_cur._grp_v = 1 then 'col_subtotal'
    else null
  end as _grp_v
from
  ds1 ds_cur
  left join ds2 ds_db on ds_cur.dt = date_format(
    date_add('day', 32, date_parse(ds_db.dt, '%Y-%m-%d')),
    '%Y-%m-%d'
  )
  and ds_cur.businessline = ds_db.businessline
ORDER BY
  /*sort*/
  ds_cur.dt asc;


--月
  /* ssm-contributor */
with ds1 as (
  SELECT
    M1.dt AS dt,
    M1.businessline AS businessline,
    M1.finish_gmv AS finish_gmv,
    null AS finish_gmv_hb_ratio,
    null AS finish_gmv_hb_value,
    M1._grp_v as _grp_v
  FROM
    (
      SELECT
        F1.dt AS dt,
        F1.businessline AS businessline,
        sum(F1.finish_gmv) AS finish_gmv,
        grouping(F1.dt, F1.businessline) as _grp_v
      from
        (
          select
            coalesce(v.businessline, '(null)') as businessline,
            coalesce(
              v.dim_order_source_channel_category_lvl1_name,
              '(null)'
            ) as dim_order_source_channel_category_lvl1_name,
            REPLACE(SUBSTR(v.order_date, 1, 7), '-', '') as dt,
            v.finish_gmv as finish_gmv
          from
            bi_view.v_ads_zzqs_ord_order_dtl_super_multi_d v
          WHERE
            (
              REPLACE(SUBSTR(v.order_date, 1, 7), '-', '') between '202307' and '202308'
            )
        ) F1
      GROUP BY
        F1.dt,
        F1.businessline
    ) M1
),
ds2 as (
  SELECT
    M1.dt AS dt,
    M1.businessline AS businessline,
    M1.finish_gmv AS finish_gmv,
    null AS finish_gmv_db_ratio,
    null AS finish_gmv_db_value,
    M1._grp_v as _grp_v
  FROM
    (
      SELECT
        F1.dt AS dt,
        F1.businessline AS businessline,
        sum(F1.finish_gmv) AS finish_gmv,
        grouping(F1.dt, F1.businessline) as _grp_v
      from
        (
          select
            coalesce(v.businessline, '(null)') as businessline,
            coalesce(
              v.dim_order_source_channel_category_lvl1_name,
              '(null)'
            ) as dim_order_source_channel_category_lvl1_name,
             REPLACE(SUBSTR(v.order_date, 1, 7), '-', '') as dt,
            v.finish_gmv as finish_gmv
          from
            bi_view.v_ads_zzqs_ord_order_dtl_super_multi_d v
          WHERE
            (
              (
                REPLACE(SUBSTR(v.order_date, 1, 7), '-', '') between '202204' and '202205'
              )

            )
        ) F1
      GROUP BY
        F1.dt,
        F1.businessline
    ) M1
)
select
  if(ds_cur._grp_v = 3, '列总计', ds_cur.dt) as dt,
  if(ds_cur._grp_v = 1, '列小计', ds_cur.businessline) as businessline,
  ds_cur.finish_gmv,
  try(
    (ds_cur.finish_gmv * 1.0000) / (ds_db.finish_gmv * 1.0000) - 1
  ) as finish_gmv_db_ratio,
  try(ds_cur.finish_gmv - ds_db.finish_gmv) as finish_gmv_db_value,
  case
    when ds_cur._grp_v = 3 then 'col_total'
    when ds_cur._grp_v = 1 then 'col_subtotal'
    else null
  end as _grp_v
from
  ds1 ds_cur
  left join ds2 ds_db on ds_cur.dt = date_format(
    date_add('month', 15, date_parse(ds_db.dt, '%Y%m')),
    '%Y%m'
  )
  and ds_cur.businessline = ds_db.businessline
ORDER BY
  /*sort*/
  ds_cur.dt asc;

--年
/* ssm-contributor */
with ds1 as (
  SELECT
    M1.dt AS dt,
    M1.businessline AS businessline,
    M1.finish_gmv AS finish_gmv,
    null AS finish_gmv_hb_ratio,
    null AS finish_gmv_hb_value,
    M1._grp_v as _grp_v
  FROM
    (
      SELECT
        F1.dt AS dt,
        F1.businessline AS businessline,
        sum(F1.finish_gmv) AS finish_gmv,
        grouping(F1.dt, F1.businessline) as _grp_v
      from
        (
          select
            coalesce(v.businessline, '(null)') as businessline,
            coalesce(
              v.dim_order_source_channel_category_lvl1_name,
              '(null)'
            ) as dim_order_source_channel_category_lvl1_name,
             SUBSTR(v.order_date, 1, 4) as dt,
            v.finish_gmv as finish_gmv
          from
            bi_view.v_ads_zzqs_ord_order_dtl_super_multi_d v
          WHERE
            (
              SUBSTR(v.order_date, 1, 4) between '2023' and '2023'
            )
        ) F1
      GROUP BY
        F1.dt,
        F1.businessline
    ) M1
),
ds2 as (
  SELECT
    M1.dt AS dt,
    M1.businessline AS businessline,
    M1.finish_gmv AS finish_gmv,
    null AS finish_gmv_db_ratio,
    null AS finish_gmv_db_value,
    M1._grp_v as _grp_v
  FROM
    (
      SELECT
        F1.dt AS dt,
        F1.businessline AS businessline,
        sum(F1.finish_gmv) AS finish_gmv,
        grouping(F1.dt, F1.businessline) as _grp_v
      from
        (
          select
            coalesce(v.businessline, '(null)') as businessline,
            coalesce(
              v.dim_order_source_channel_category_lvl1_name,
              '(null)'
            ) as dim_order_source_channel_category_lvl1_name,
             SUBSTR(v.order_date, 1, 4) as dt,
            v.finish_gmv as finish_gmv
          from
            bi_view.v_ads_zzqs_ord_order_dtl_super_multi_d v
          WHERE
            (
              (
                 SUBSTR(v.order_date, 1, 4) between '2021' and '2021'
              )

            )
        ) F1
      GROUP BY
        F1.dt,
        F1.businessline
    ) M1
)
select
  if(ds_cur._grp_v = 3, '列总计', ds_cur.dt) as dt,
  if(ds_cur._grp_v = 1, '列小计', ds_cur.businessline) as businessline,
  ds_cur.finish_gmv,
  try(
    (ds_cur.finish_gmv * 1.0000) / (ds_db.finish_gmv * 1.0000) - 1
  ) as finish_gmv_db_ratio,
  try(ds_cur.finish_gmv - ds_db.finish_gmv) as finish_gmv_db_value,
  case
    when ds_cur._grp_v = 3 then 'col_total'
    when ds_cur._grp_v = 1 then 'col_subtotal'
    else null
  end as _grp_v
from
  ds1 ds_cur
  left join ds2 ds_db on ds_cur.dt = date_format(
    date_add('year', 2, date_parse(ds_db.dt, '%Y')),
    '%Y'
  )
  and ds_cur.businessline = ds_db.businessline
ORDER BY
  /*sort*/
  ds_cur.dt asc;

--周




 --汇总
  with ds1 as (
  SELECT
    M1.businessline AS businessline,
    M1.finish_gmv AS finish_gmv,
    null AS finish_gmv_hb_ratio,
    null AS finish_gmv_hb_value,
    M1._grp_v as _grp_v
  FROM
    (
      SELECT
        F1.businessline AS businessline,
        sum(F1.finish_gmv) AS finish_gmv,
        grouping(F1.businessline) as _grp_v
      from
        (
          select
            coalesce(v.businessline, '(null)') as businessline,
            coalesce(
              v.dim_order_source_channel_category_lvl1_name,
              '(null)'
            ) as dim_order_source_channel_category_lvl1_name,
            v.finish_gmv as finish_gmv
          from
            bi_view.v_ads_zzqs_ord_order_dtl_super_multi_d v
          WHERE
            (
              SUBSTR(v.order_date, 1, 10) between '2023-08-21' and '2023-08-22'
            )
        ) F1
      GROUP BY
        F1.businessline
    ) M1
),
ds2 as (
  SELECT
    M1.businessline AS businessline,
    M1.finish_gmv AS finish_gmv,
    null AS finish_gmv_db_ratio,
    null AS finish_gmv_db_value,
    M1._grp_v as _grp_v
  FROM
    (
      SELECT
        F1.businessline AS businessline,
        sum(F1.finish_gmv) AS finish_gmv,
        grouping( F1.businessline) as _grp_v
      from
        (
          select
            coalesce(v.businessline, '(null)') as businessline,
            coalesce(
              v.dim_order_source_channel_category_lvl1_name,
              '(null)'
            ) as dim_order_source_channel_category_lvl1_name,
            SUBSTR(v.order_date, 1, 10) as dt,
            v.finish_gmv as finish_gmv
          from
            bi_view.v_ads_zzqs_ord_order_dtl_super_multi_d v
          WHERE
            (
              (
                SUBSTR(v.order_date, 1, 10) between '2023-07-20' and '2023-07-21'
              )
            )
        ) F1
      GROUP BY
        F1.businessline
    ) M1
)
select
  if(ds_cur._grp_v = 1, '列小计', ds_cur.businessline) as businessline,
  ds_cur.finish_gmv,
  try(
    (ds_cur.finish_gmv * 1.0000) / (ds_db.finish_gmv * 1.0000) - 1
  ) as finish_gmv_db_ratio,
  try(ds_cur.finish_gmv - ds_db.finish_gmv) as finish_gmv_db_value,
  case
    when ds_cur._grp_v = 3 then 'col_total'
    when ds_cur._grp_v = 1 then 'col_subtotal'
    else null
  end as _grp_v
from
  ds1 ds_cur
  left join ds2 ds_db on ds_cur.businessline = ds_db.businessline
ORDER BY
  /*sort*/
  ds_cur.businessline asc;