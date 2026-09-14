package com.bi.queryer.ssm.portal.template.vo;

import lombok.Data;

import java.util.List;

/**
 * 根据多组「模板名称 + 视图名称」生成临时看板名称（调用 LLM 工具）
 */
@Data
public class GenerateTmpAnalysisTplNameReq {

	/**
	 * 模板与视图一一对应的列表（批量）；每组必须同时提供模板名称与视图名称
	 */
	private List<QueryTemplateName> tplViewPairs;

	/**
	 * 模型名称，可选；未传时使用配置项默认值
	 */
	private String modelName;

	@Data
	public static class QueryTemplateName {
		/**
		 * 模板名称
		 */
		private String templateName;

		/**
		 * 该模板下视图名称
		 */
		private String viewName;
	}
}
