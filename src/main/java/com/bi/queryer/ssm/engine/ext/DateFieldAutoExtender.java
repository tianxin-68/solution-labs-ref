package com.bi.queryer.ssm.engine.ext;

import com.bi.queryer.ssm.engine.function.IFunction;
import com.bi.queryer.ssm.enums.FieldDataType;
import com.bi.queryer.ssm.enums.FieldFilterType;
import com.bi.queryer.ssm.enums.FieldValueFilterType;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.ssm.meta.MetaTable;
import com.bi.queryer.ssm.meta.SSDMetaCacheManager;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.util.BIUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 日期字段自动扩展器
 * @author contributor
 *
 */
public class DateFieldAutoExtender implements IFieldAutoExtender{

	/**
	 * 将日期字段扩展为：日、周、月、季、年
	 */
	@Override
	public List<MetaField> extend(MetaField src) {
		String extendList = src.getFieldExtendList();
		// 兼容处理
		if("Auto-Extend".equalsIgnoreCase(src.getShowFormatExpression())) {
			extendList = "y,q,m,w,d";
		}

		// 扩展字段类别
		List<MetaField> extendFields = new ArrayList<MetaField>();

		if(BIUtil.isEmpty(extendList)) {
			return extendFields;
		}
		extendList = extendList.toLowerCase();
		boolean extendWeek = extendList.contains("w");
		boolean extendMonth = extendList.contains("m");
		boolean extendQuarter = extendList.contains("q");
		boolean extendYear = extendList.contains("y");

		String title = src.getTitle();
		// 日 :不添加到结果列表中
		src.setShowFormatExpression(IFunction.Format_Date);
		src.setFilterShowType(FieldFilterType.DateRange.getCode());

		/**
		 * 周、季修改id、code、name、title、数据类型、格式化表达式、过滤显示类型、维度表id、维度表、keyname、过滤表id、过滤表名
		 */
		// 周
		if(extendWeek) {
			MetaField weekField = src.clone();
			String weekTitle = title.replace("日期", "年周");
			weekField.setId(src.getId() + "_ext_w");
			weekField.setCode(src.getCode() + "_ext_w");
			weekField.setName(SC.v("ssm.date.week.name.field", "week_long_desc"));
			weekField.setTitle(weekTitle);
			weekField.setDataType(FieldDataType.String.toString());
			weekField.setShowFormatExpression(null);
			weekField.setFilterShowType(FieldFilterType.WeekRange.getCode());
			weekField.setShowOrder(src.getShowOrder() + 0.001);

			String dateTableFullName = SC.v("ssm.date.table.fullname", "bi_dim.dim_date");
			MetaTable dateTable = SSDMetaCacheManager.getTableByFullName(dateTableFullName);
			if (dateTable != null) {
				String dateTableId = dateTable.getId();
				weekField.setDimTableId(dateTableId); // 日期维表
				weekField.setDimTableName(dateTableFullName);
				weekField.setFactTableId(null);
				weekField.setFactTableName(null);
				MetaField weekKeyField = SSDMetaCacheManager.getFieldByName(dateTableId, SC.v("ssm.date.week.id.field", "week_id"));
				if (weekKeyField != null) {
					weekField.setFieldKeyId(weekKeyField.getId());
					weekField.setFieldKeyName(weekKeyField.getName());
					weekField.setFieldKeyType(weekKeyField.getDataType());
				}
				weekField.setFilterTableId(dateTableId);
				weekField.setFilterTableName(dateTableFullName);
				src.addExtend(weekField);
				extendFields.add(weekField);
			}
		}

		/**
		 * 月、年修改id、code、title、格式化表达式、过滤类型
		 *
		 */
		// 月
		if(extendMonth) {
			MetaField monthField = src.clone();
			String monthTitle = title.replace("日期", "年月");
			monthField.setId(src.getId() + "_ext_m");
			monthField.setCode(src.getCode() + "_ext_m");
			monthField.setTitle(monthTitle);
			monthField.setShowFormatExpression(IFunction.Format_Month);
			monthField.setFilterShowType(FieldFilterType.MonthRange.getCode());
			monthField.setShowOrder(src.getShowOrder() + 0.002);
			src.addExtend(monthField);
			extendFields.add(monthField);
		}

		// 年
		if(extendYear) {
			MetaField yearField = src.clone();
			String yearTitle = title.replace("日期", "年份");
			yearField.setId(src.getId() + "_ext_y");
			yearField.setCode(src.getCode() + "_ext_y");
			yearField.setTitle(yearTitle);
			yearField.setShowFormatExpression(IFunction.Format_Year);
			yearField.setFilterShowType(FieldFilterType.YearRange.getCode());
			yearField.setShowOrder(src.getShowOrder() + 0.1);
			src.addExtend(yearField);
			extendFields.add(yearField);
		}

		// 季
		/*
		MetaField quarterField = src.clone();
		String quarterTitle = title.replace("日期", "季度");
		quarterField.setId(src.getId() + "_ext_q");
		quarterField.setCode(src.getId() + "_ext_q");
		quarterField.setName("QUARTER_NAME");
		quarterField.setTitle(quarterTitle);
		quarterField.setDataType(FieldDataType.String.toString());
		quarterField.setShowFormatExpression(null);
		quarterField.setFilterShowType("quarter-range");
		quarterField.setDimTableId("1025"); // 日期维表
		quarterField.setDimTableName("SSD.V_DIM_DATE");
		quarterField.setFactTableId(null);
		quarterField.setFactTableName(null);
		quarterField.setFieldKeyName("QUARTER_ID");
		quarterField.setFilterTableId("1025");
		quarterField.setFilterTableName("SSD.V_DIM_DATE");
		src.addExtend(quarterField);
		extendFields.add(quarterField);
		*/
		return extendFields;
	}

}
