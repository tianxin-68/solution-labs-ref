package com.bi.queryer.ssm.engine.ext;

import com.bi.queryer.ssm.meta.MetaField;

import java.util.List;

/**
 * 字段自动扩展器
 * @author contributor
 *
 */
public interface IFieldAutoExtender {
	public List<MetaField> extend(MetaField src);
}
