-- 当前日期 2023-08-31
-- 选择2023-07 ~ 2023-08 自定义对比  2023-06 ~ 2023-07
/* ssm-contributor */
with ds1 as (
  SELECT
    M1.dt AS dt,
    M1.businessline AS businessline,
    M1.pay_user AS pay_user,
    null as pay_user_hb_ratio,
    null as pay_user_tb_y_ratio,
    null as pay_user_c_cmp_ratio,
    M1._grp_v as _grp_v
  FROM
    (
      SELECT
        F1.dt AS dt,
        F1.businessline AS businessline,
        count(DISTINCT F1.pay_user) AS pay_user,
        grouping(F1.dt, F1.businessline) as _grp_v
      from
        (
          select
            coalesce(v.businessline, '(null)') as businessline,
            REPLACE(SUBSTR(v.order_date, 1, 7), '-', '') as dt,
            v.pay_user as pay_user
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
    M1.pay_user AS pay_user,
    null as pay_user_hb_ratio,
    null as pay_user_tb_y_ratio,
    null as pay_user_c_cmp_ratio,
    M1._grp_v as _grp_v
  FROM
    (
      SELECT
        F1.dt AS dt,
        F1.businessline AS businessline,
        count(DISTINCT F1.pay_user) AS pay_user,
        grouping(F1.dt, F1.businessline) as _grp_v
      from
        (
          select
            coalesce(v.businessline, '(null)') as businessline,
            REPLACE(SUBSTR(v.order_date, 1, 7), '-', '') as dt,
            v.pay_user as pay_user
          from
            bi_view.v_ads_zzqs_ord_order_dtl_super_multi_d v
          WHERE
            (
              (
                REPLACE(SUBSTR(v.order_date, 1, 7), '-', '') between '202307' and '202308'
              )
              or (
                REPLACE(SUBSTR(v.order_date, 1, 7), '-', '') between '202207' and '202208'
              )
              or (
                REPLACE(SUBSTR(v.order_date, 1, 7), '-', '') between '202306' and '202306'
              )
            )
            and (
              SUBSTR(v.order_date, 1, 10) not BETWEEN '2023-07-31' and '2023-07-31'
            )
            and (
              SUBSTR(v.order_date, 1, 10) not BETWEEN '2022-07-31' and '2022-07-31'
            )
        ) F1
      GROUP BY
        F1.dt,
        F1.businessline
    ) M1
),
ds3 as (
  SELECT
    M1.dt AS dt,
    M1.businessline AS businessline,
    M1.pay_user AS pay_user,
    null as pay_user_hb_ratio,
    null as pay_user_tb_y_ratio,
    null as pay_user_c_cmp_ratio,
    M1._grp_v as _grp_v
  FROM
    (
      SELECT
        F1.dt AS dt,
        F1.businessline AS businessline,
        count(DISTINCT F1.pay_user) AS pay_user,
        grouping(F1.dt, F1.businessline) as _grp_v
      from
        (
          select
            coalesce(v.businessline, '(null)') as businessline,
            REPLACE(SUBSTR(v.order_date, 1, 7), '-', '') as dt,
            v.pay_user as pay_user
          from
            bi_view.v_ads_zzqs_ord_order_dtl_super_multi_d v
          WHERE
            (
              (
                REPLACE(SUBSTR(v.order_date, 1, 7), '-', '') between '202306' and '202307'
              )
            )
            and (
              SUBSTR(v.order_date, 1, 10) not BETWEEN '2023-07-31' and '2023-07-31'
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
  ds_cur.pay_user,
  try(
    (ds_cur.pay_user * 1.0000) / (ds_hb.pay_user * 1.0000) - 1
  ) as pay_user_hb_ratio,
  try(
    (ds_cur.pay_user * 1.0000) / (ds_tb_y.pay_user * 1.0000) - 1
  ) as pay_user_tb_y_ratio,
  try(
    (ds_cur.pay_user * 1.0000) / (ds_c_cmp.pay_user * 1.0000) - 1
  ) as pay_user_c_cmp_ratio,
  case
    when ds_cur._grp_v = 3 then 'col_total'
    when ds_cur._grp_v = 1 then 'col_subtotal'
    else null
  end as _grp_v
from
  ds1 ds_cur
  left join ds2 ds_hb on ds_cur.dt = date_format(
    date_add('month', 1, date_parse(ds_hb.dt, '%Y%m')),
    '%Y%m'
  )
  and coalesce(ds_cur.businessline, '-9999') = coalesce(ds_hb.businessline, '-9999')
  left join ds2 ds_tb_y on ds_cur.dt = date_format(
    date_add('year', 1, date_parse(ds_tb_y.dt, '%Y%m')),
    '%Y%m'
  )
  and coalesce(ds_cur.businessline, '-9999') = coalesce(ds_tb_y.businessline, '-9999')
  left join ds3 ds_c_cmp on ds_cur.dt = date_format(
    date_add('month', 1, date_parse(ds_c_cmp.dt, '%Y%m')),
    '%Y%m'
  )
  and coalesce(ds_cur.businessline, '-9999') = coalesce(ds_c_cmp.businessline, '-9999')
ORDER BY
  /*sort*/
  ds_cur.dt asc