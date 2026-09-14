package com.bi.queryer.ssm.engine.ext;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.ssm.enums.AggExpressionType;
import com.bi.queryer.ssm.meta.MetaField;
import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIConsts;

import java.util.ArrayList;
import java.util.List;

/**
 * 指标字段自动扩展器（按聚合方式自动扩展）
 * @author contributor
 *
 */
public class MeasureFieldAutoExtender implements IFieldAutoExtender{

	@Override
	public List<MetaField> extend(MetaField src) {
		// 扩展字段类别
		List<MetaField> extendFields = new ArrayList<MetaField>();

		if(Enabled.isFalse(src.getIsMeasure())){
			return extendFields;
		}

		String title = src.getTitle();


		/**
		 * 日均 + 非空日均
		 */
		List<AggExpressionType> aggTypes = new ArrayList<>();
		aggTypes.add(AggExpressionType.Avg_By_Day);
		aggTypes.add(AggExpressionType.Avg_By_Day_Real);

		int index = 0;
		for(AggExpressionType aggType : aggTypes){
			index++;
			MetaField avgField = src.clone();
			avgField.setIsShow(Enabled.NO.getId());
			avgField.setId(src.getId() + "_" + aggType.getCode());
			avgField.setCode(src.getCode() + "_" + aggType.getCode());
			String formatString = avgField.getShowFormatExpression();
			//showFormatExpression为空，根据字段类型赋值
			if (StrUtil.isEmpty(formatString)) {
				DataType dataType = DataType.getType(avgField.getDataType());
				if (dataType.isInteger()) {
					formatString = "###,###,##0";
				} else if (DataType.Double == dataType) {
					formatString = "###,###,##0.00";
				}
			}
			formatString = formatString + BIConsts.AVG_FORMAT_SUFFIX;
			avgField.setShowFormatExpression(formatString);

			avgField.setTitle(title);
			avgField.setShowOrder(src.getShowOrder() + (index * 0.001));
			src.addExtend(avgField);
			extendFields.add(avgField);
		}

		return extendFields;
	}

}
