package com.bi.queryer.util.download;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.bi.queryer.util.BIUtil;

/**
 * Excel表头构建器，用于处理多表头（复杂表头）。<br/>
 * 算法：<br/>
 * 初始化时，通过titles里的每个title的分割标示符（^）分解为多个 {@link HeadFieldInfo}<br/>
 * 1、找出所有分组（不重复的，每个原始分组名后带__level），如果有没有分组的默认为自己，但groupCount=0<br/>
 * 2、遍历每个分组在HeadFieldInfos的坐标：（第N列开始，第N行开始，合并N列，合并N行），最终转化为 {@link ExcelHeadCell}<br/>
 * <br/>
 * 表头分隔符：^<br/>
 * 如：原始title格式：grp1^grp2^title, 没有分组标题不带^
 * @author contributor
 */
public class ExcelHeadBuilder {
	
	public static final String Split_Flag = "^";
	
	protected String titles[] = null;
	protected String fields[] = null;
	protected int maxRowCount = 1;// 最大行数
	
	protected Map<String, HeadFieldInfo> fieldMap = new LinkedHashMap<String, HeadFieldInfo>();
	
	protected Set<String> allGroups = new LinkedHashSet<String>();
	
	public ExcelHeadBuilder(String[] fields, String[] titles) {
		this.titles = titles;
		this.fields = fields;
	}
	
	/**
	 * 构建Excel多表头信息
	 * @return
	 */
	public List<ExcelHeadCell> build(){
		
		initialize();
		
		List<ExcelHeadCell> headCells = new ArrayList<ExcelHeadCell>();
		
		// 先处理组
		for(String grp : allGroups){
			ExcelHeadCell grpCell = new ExcelHeadCell();
			grpCell.title =  grp.replaceAll("\\_\\_\\d+", "");
			int index =-1;
			for(String field : fieldMap.keySet()){
				index++;
				HeadFieldInfo info = fieldMap.get(field);
				int grpIndex = info.getGroupIndex(grp);
				if(grpIndex != -1){
					if(grpCell.colIndex == -1){
						grpCell.colIndex = index;
						grpCell.rowIndex = grpIndex;
					}
					grpCell.colSpan++;
					grpCell.rowSpan = maxRowCount / (info.groupCount + 1) ;
					grpCell.isGroup = (info.groupCount != 0);
					grpCell.field = info.field;
				}
			}
			grpCell.title = grpCell.title.replaceAll("\\_\\_" + grpCell.field, "");
			headCells.add(grpCell);
		}
		
		// 处理字段
		int index =-1;
		for(String field : fieldMap.keySet()){
			index++;
			HeadFieldInfo info = fieldMap.get(field);
			if(info.groupCount == 0){
				continue;
			}
			ExcelHeadCell fieldCell = new ExcelHeadCell();
			fieldCell.field = info.field;
			fieldCell.title = info.title.replaceAll("\\_\\_" + info.field , "");
			fieldCell.colIndex = index;
			fieldCell.rowIndex = info.groupCount;
			fieldCell.colSpan = 1;
			fieldCell.rowSpan =  maxRowCount / (info.groupCount + 1);
			fieldCell.isGroup = false;
			headCells.add(fieldCell);
		}
		
		return headCells;
	}
	
	/**
	 * 初始化
	 */
	protected void initialize(){
		Set<Integer> grpCountSet = new HashSet<Integer>();
		for(int i = 0; i < fields.length; i++){
			HeadFieldInfo info = new HeadFieldInfo();
			info.field = fields[i];
			info.groupCount = StringUtils.countMatches(titles[i], Split_Flag);
			if(info.groupCount != 0){
				info.title = StringUtils.substringAfterLast(titles[i], Split_Flag) + "__" + info.field;
				String gtitles[] = StringUtils.split(titles[i], Split_Flag);
				for(int k = 0; k < gtitles.length-1; k++){
					info.groups.add(gtitles[k] + "__" + k);
				}
			}else{// 没有分组的，默认为自己分组
				info.title = titles[i] + "__" + info.field;
				info.groupCount = 0;
				info.groups.add(info.title);
			}
			for(String g : info.groups){
				allGroups.add(g);
			}
			fieldMap.put(fields[i], info);
			grpCountSet.add(info.groupCount + 1);
		}
		
		Integer counts[] = new Integer[grpCountSet.size()];
		grpCountSet.toArray(counts);
		
		maxRowCount = BIUtil.minCommonMultiple(counts);
	}
	
	public static void main(String[] args) {
//		String fields[] = new String[]{"field1", "field2", "field3", "field4", "field5", "field6", "f_sum", "field7", "field8", "field9"};
//		String titles[] = new String[]{"title1", "title2", "title3", "title4", "title5", "title6", "合计", "title7", "title8", "title9"};
//		String titles[] = new String[]{"grp1^grp1.1^title1", "grp1^grp1.1^title2", "grp1^grp1.2^title3", "grp1^grp1.2^title4", "grp2^title5", "grp2^title6", "grp2^合计", "grp3^title7", "grp3^title8", "title9"};
//		String titles[] = new String[]{"grp1^grp1.1^title1", "grp1^grp1.1^title2", "grp1^grp1.2^title3", "grp1^grp1.2^title4", "grp2^title5", "grp2^title6", "合计", "grp3^title7", "grp3^title8", "title9"};
		String fields[] = new String[]{"field1", "field2", "field3", "field4"};
		String titles[] = new String[]{"title1", "title2", "title2", "title4"};
		ExcelHeadBuilder builder = new ExcelHeadBuilder(fields, titles);
		List<ExcelHeadCell> cells = builder.build();
		for(ExcelHeadCell c : cells){
			System.out.println(c);
		}
		/*
		String str = "grp1^title";
		String str2 = StringUtils.substringBeforeLast(str, Split_Flag);
		String str3 = StringUtils.substringAfterLast(str, Split_Flag);
		System.out.println(str2 + ":" + str3);
		
		int grpCount = StringUtils.countMatches(str, Split_Flag);
		System.out.println(grpCount);
		*/
		
		String str = "title1__field1";
		str = str.replaceAll("\\_\\_" + "field1", "");
		System.out.println(str);
	}
	
}

class HeadFieldInfo {
	protected String field;// 所属字段
	protected String title;// 去掉分组后的
	protected int groupCount;// 分组格式
	protected List<String> groups = new ArrayList<String>();// 所有分组
	
	public int getGroupIndex(String grp){
		return groups.indexOf(grp);
	}
}