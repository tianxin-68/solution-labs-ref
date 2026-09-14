package com.bi.queryer.util.period;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 提供一些农历相关信息
 * @author contributor
 */
public class Lunar {
	private Calendar solar;
	private int lunarYear;
	private int lunarMonth;
	private int lunarDay;
	private boolean isLeap;
	private boolean isLeapYear;
	private int solarYear;
	private int solarMonth;
	private int solarDay;
	private int cyclicalYear = 0;
	private int cyclicalMonth = 0;
	private int cyclicalDay = 0;
	private int maxDayInMonth = 29;

	private boolean isFinded = false;
	private boolean isSFestival = false;
	private boolean isLFestival = false;
	private String sFestivalName = "";
	private String lFestivalName = "";
	private String description = "";
	private boolean isHoliday = false;

	private final static int[] lunarInfo = {
			0x4bd8, 0x4ae0, 0xa570, 0x54d5, 0xd260, 0xd950, 0x5554, 0x56af,
			0x9ad0, 0x55d2, 0x4ae0, 0xa5b6, 0xa4d0, 0xd250, 0xd295, 0xb54f,
			0xd6a0, 0xada2, 0x95b0, 0x4977, 0x497f, 0xa4b0, 0xb4b5, 0x6a50,
			0x6d40, 0xab54, 0x2b6f, 0x9570, 0x52f2, 0x4970, 0x6566, 0xd4a0,
			0xea50, 0x6a95, 0x5adf, 0x2b60, 0x86e3, 0x92ef, 0xc8d7, 0xc95f,
			0xd4a0, 0xd8a6, 0xb55f, 0x56a0, 0xa5b4, 0x25df, 0x92d0, 0xd2b2,
			0xa950, 0xb557, 0x6ca0, 0xb550, 0x5355, 0x4daf, 0xa5b0, 0x4573,
			0x52bf, 0xa9a8, 0xe950, 0x6aa0, 0xaea6, 0xab50, 0x4b60, 0xaae4,
			0xa570, 0x5260, 0xf263, 0xd950, 0x5b57, 0x56a0, 0x96d0, 0x4dd5,
			0x4ad0, 0xa4d0, 0xd4d4, 0xd250, 0xd558, 0xb540, 0xb6a0, 0x95a6,
			0x95bf, 0x49b0, 0xa974, 0xa4b0, 0xb27a, 0x6a50, 0x6d40, 0xaf46,
			0xab60, 0x9570, 0x4af5, 0x4970, 0x64b0, 0x74a3, 0xea50, 0x6b58,
			0x5ac0, 0xab60, 0x96d5, 0x92e0, 0xc960, 0xd954, 0xd4a0, 0xda50,
			0x7552, 0x56a0, 0xabb7, 0x25d0, 0x92d0, 0xcab5, 0xa950, 0xb4a0,
			0xbaa4, 0xad50, 0x55d9, 0x4ba0, 0xa5b0, 0x5176, 0x52bf, 0xa930,
			0x7954, 0x6aa0, 0xad50, 0x5b52, 0x4b60, 0xa6e6, 0xa4e0, 0xd260,
			0xea65, 0xd530, 0x5aa0, 0x76a3, 0x96d0, 0x4afb, 0x4ad0, 0xa4d0,
			0xd0b6, 0xd25f, 0xd520, 0xdd45, 0xb5a0, 0x56d0, 0x55b2, 0x49b0,
			0xa577, 0xa4b0, 0xaa50, 0xb255, 0x6d2f, 0xada0, 0x4b63, 0x937f,
			0x49f8, 0x4970, 0x64b0, 0x68a6, 0xea5f, 0x6b20, 0xa6c4, 0xaaef,
			0x92e0, 0xd2e3, 0xc960, 0xd557, 0xd4a0, 0xda50, 0x5d55, 0x56a0,
			0xa6d0, 0x55d4, 0x52d0, 0xa9b8, 0xa950, 0xb4a0, 0xb6a6, 0xad50,
			0x55a0, 0xaba4, 0xa5b0, 0x52b0, 0xb273, 0x6930, 0x7337, 0x6aa0,
			0xad50, 0x4b55, 0x4b6f, 0xa570, 0x54e4, 0xd260, 0xe968, 0xd520,
			0xdaa0, 0x6aa6, 0x56df, 0x4ae0, 0xa9d4, 0xa4d0, 0xd150, 0xf252, 0xd520
	};
	private final static int[] solarTermInfo = {
			0,  21208,  42467,  63836,  85337, 107014, 128867, 150921,
			173149, 195551, 218072, 240693, 263343, 285989, 308563, 331033,
			353350, 375494, 397447, 419210, 440795, 462224, 483532, 504758
	};
	private final static String[] Tianan = {
			"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"
	};
	private final static String[] Deqi = {
			"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"
	};
	private final static String[] Animals = {
			"鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪"
	};
	private final static String[] solarTerm = {
			"小寒", "大寒", "立春", "雨水", "惊蛰", "春分",
			"清明", "谷雨", "立夏", "小满", "芒种", "夏至",
			"小暑", "大暑", "立秋", "处暑", "白露", "秋分",
			"寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
	};
	private final static String[] lunarString1 = {
			"零", "一", "二", "三", "四", "五", "六", "七", "八", "九"
	};
	private final static String[] lunarString2 = {
			"初", "十", "廿", "三", "正", "腊", "冬", "闰"
	};

	/**
	 * 国历节日 *表示放假日
	 */
	private final static String[] sFtv = {
			"0101*元旦",		"0214 情人节",	"0308 妇女节",		"0312 植树节",
			"0315 消费者权益日",	"0401 愚人节",	"0501*劳动节",		"0504 青年节",
			"0509 郝维节",		"0512 护士节",	"0601 儿童节",		"0701 建党节 香港回归纪念",
			"0801 建军节",		"0808 父亲节",	"0816 燕衔泥节",	"0909 毛泽东逝世纪念",
			"0910 教师节",		"0928 孔子诞辰","1001*国庆节",		"1006 老人节",
			"1024 联合国日",	"1111 光棍节",	"1112 孙中山诞辰纪念",	"1220 澳门回归纪念",
			"1225 圣诞节",		"1226 毛泽东诞辰纪念"
	};
	/**
	 * 农历节日 *表示放假日
	 */
	private final static String[] lFtv = {
			"0101*春节",	"0106 定光佛诞",	"0115 元宵节",
			"0208 释迦牟尼佛出家",	"0215 释迦牟尼佛涅槃",	"0209 海空上师诞",
			"0219 观世音菩萨诞",	"0221 普贤菩萨诞",	"0316 准提菩萨诞",
			"0404 文殊菩萨诞",	"0408 释迦牟尼佛诞",	"0415 佛吉祥日——释迦牟尼佛诞生、成道、涅槃三期同一庆(即南传佛教国家的卫塞节)",
			"0505*端午节",		"0513 伽蓝菩萨诞",	"0603 护法韦驮尊天菩萨诞",
			"0619 观世音菩萨成道——此日放生、念佛，功德殊胜",
			"0707 七夕情人节",	"0713 大势至菩萨诞",	"0715 中元节",
			"0724 龙树菩萨诞",	"0730 地藏菩萨诞",	"0815*中秋节",
			"0822 燃灯佛诞",	"0909 重阳节",		"0919 观世音菩萨出家纪念日",
			"0930 药师琉璃光如来诞","1005 达摩祖师诞",	"1107 阿弥陀佛诞",
			"1208 释迦如来成道日，腊八节",			"1224 小年",
			"1229 华严菩萨诞",	"0100*除夕"
	};
	/**
	 * 某月的第几个星期几
	 */
	private static String[] wFtv = {
			"0520 母亲节", "0716 合作节", "0730 被奴役国家周"
	};

	/**
	 * 近3年同比对照日期
	 * 注：每年维护一次
	 */
	private static final String[][] yearTbMappingDays = {
//			{"2023-10-25", "2024-10-13",  "2025-11-01"},
//			{"2023-10-26", "2024-10-14",  "2025-11-02"},
//			{"2023-10-27", "2024-10-15",  "2025-11-03"},
//			{"2023-10-28", "2024-10-16",  "2025-11-04"},
//			{"2023-10-29", "2024-10-17",  "2025-11-05"},
//			{"2023-10-30", "2024-10-18",  "2025-11-06"},
//			{"2023-10-31", "2024-10-19",  "2025-11-07"},
//			{"2023-11-01", "2024-10-20",  "2025-11-08"},
//			{"2023-11-02", "2024-10-21",  "2025-11-09"},
//			{"2023-11-03", "2024-10-22",  "2025-11-10"},
//			{"2023-11-04", "2024-10-23",  "2025-11-11"},
//			{"2023-11-05", "2024-10-24",  "2025-11-12"},
//			{"2023-11-06", "2024-10-25",  "2025-11-13"},
//			{"2023-11-07", "2024-10-26",  "2025-11-14"},
//			{"2023-11-08", "2024-10-27",  "2025-11-15"},
//			{"2023-11-09", "2024-10-28",  "2025-11-16"},
//			{"2023-11-10", "2024-10-29",  "2025-11-17"},
//			{"2023-11-11", "2024-10-30",  "2025-11-18"},
//			{"2023-11-12", "2024-10-31",  "2025-11-19"},
			{"2023-11-13", "2024-11-01",  "2025-11-20"},
			{"2023-11-14", "2024-11-02",  "2025-11-21"},
			{"2023-11-15", "2024-11-03",  "2025-11-22"},
			{"2023-11-16", "2024-11-04",  "2025-11-23"},
			{"2023-11-17", "2024-11-05",  "2025-11-24"},
			{"2023-11-18", "2024-11-06",  "2025-11-25"},
			{"2023-11-19", "2024-11-07",  "2025-11-26"},
			{"2023-11-20", "2024-11-08",  "2025-11-27"},
			{"2023-11-21", "2024-11-09",  "2025-11-28"},
			{"2023-11-22", "2024-11-10",  "2025-11-29"},
			{"2023-11-23", "2024-11-11",  "2025-11-30"},
			{"2023-11-24", "2024-11-12",  "2025-12-01"},
			{"2023-11-25", "2024-11-13",  "2025-12-02"},
			{"2023-11-26", "2024-11-14",  "2025-12-03"},
			{"2023-11-27", "2024-11-15",  "2025-12-04"},
			{"2023-11-28", "2024-11-16",  "2025-12-05"},
			{"2023-11-29", "2024-11-17",  "2025-12-06"},
			{"2023-11-30", "2024-11-18",  "2025-12-07"},
			{"2023-12-01", "2024-11-19",  "2025-12-08"},
			{"2023-12-02", "2024-11-20",  "2025-12-09"},
			{"2023-12-03", "2024-11-21",  "2025-12-10"},
			{"2023-12-04", "2024-11-22",  "2025-12-11"},
			{"2023-12-05", "2024-11-23",  "2025-12-12"},
			{"2023-12-06", "2024-11-24",  "2025-12-13"},
			{"2023-12-07", "2024-11-25",  "2025-12-14"},
			{"2023-12-08", "2024-11-26",  "2025-12-15"},
			{"2023-12-09", "2024-11-27",  "2025-12-16"},
			{"2023-12-10", "2024-11-28",  "2025-12-17"},
			{"2023-12-11", "2024-11-29",  "2025-12-18"},
			{"2023-12-12", "2024-11-30",  "2025-12-19"},
			{"2023-12-13", "2024-12-01",  "2025-12-20"},
			{"2023-12-14", "2024-12-02",  "2025-12-21"},
			{"2023-12-15", "2024-12-03",  "2025-12-22"},
			{"2023-12-16", "2024-12-04",  "2025-12-23"},
			{"2023-12-17", "2024-12-05",  "2025-12-24"},
			{"2023-12-18", "2024-12-06",  "2025-12-25"},
			{"2023-12-19", "2024-12-07",  "2025-12-26"},
			{"2023-12-20", "2024-12-08",  "2025-12-27"},
			{"2023-12-21", "2024-12-09",  "2025-12-28"},
			{"2023-12-22", "2024-12-10",  "2025-12-29"},
			{"2023-12-23", "2024-12-11",  "2025-12-30"},
			{"2023-12-24", "2024-12-12",  "2025-12-31"},
			{"2023-12-25", "2024-12-13",  "2026-01-01"},
			{"2023-12-26", "2024-12-14",  "2026-01-02"},
			{"2023-12-27", "2024-12-15",  "2026-01-03"},
			{"2023-12-28", "2024-12-16",  "2026-01-04"},
			{"2023-12-29", "2024-12-17",  "2026-01-05"},
			{"2023-12-30", "2024-12-18",  "2026-01-06"},
			{"2023-12-31", "2024-12-19",  "2026-01-07"},
			{"2024-01-01", "2024-12-20",  "2026-01-08"},
			{"2024-01-02", "2024-12-21",  "2026-01-09"},
			{"2024-01-03", "2024-12-22",  "2026-01-10"},
			{"2024-01-04", "2024-12-23",  "2026-01-11"},
			{"2024-01-05", "2024-12-24",  "2026-01-12"},
			{"2024-01-06", "2024-12-25",  "2026-01-13"},
			{"2024-01-07", "2024-12-26",  "2026-01-14"},
			{"2024-01-08", "2024-12-27",  "2026-01-15"},
			{"2024-01-09", "2024-12-28",  "2026-01-16"},
			{"2024-01-09", "2024-12-29",  "2026-01-17"},
			{"2024-01-10", "2024-12-30",  "2026-01-18"},
			{"2024-01-11", "2024-12-31",  "2026-01-19"},
			{"2024-01-12", "2025-01-01",  "2026-01-20"},
			{"2024-01-13", "2025-01-02",  "2026-01-21"},
			{"2024-01-14", "2025-01-03",  "2026-01-22"},
			{"2024-01-15", "2025-01-04",  "2026-01-23"},
			{"2024-01-16", "2025-01-05",  "2026-01-24"},
			{"2024-01-17", "2025-01-06",  "2026-01-25"},
			{"2024-01-18", "2025-01-07",  "2026-01-26"},
			{"2024-01-19", "2025-01-08",  "2026-01-27"},
			{"2024-01-20", "2025-01-09",  "2026-01-28"},
			{"2024-01-21", "2025-01-10",  "2026-01-29"},
			{"2024-01-22", "2025-01-11",  "2026-01-30"},
			{"2024-01-23", "2025-01-12",  "2026-01-31"},
			{"2024-01-24", "2025-01-13",  "2026-02-01"},
			{"2024-01-25", "2025-01-14",  "2026-02-02"},
			{"2024-01-26", "2025-01-15",  "2026-02-03"},
			{"2024-01-27", "2025-01-16",  "2026-02-04"},
			{"2024-01-28", "2025-01-17",  "2026-02-05"},
			{"2024-01-29", "2025-01-18",  "2026-02-06"},
			{"2024-01-30", "2025-01-19",  "2026-02-07"},
			{"2024-01-31", "2025-01-20",  "2026-02-08"},
			{"2024-02-01", "2025-01-21",  "2026-02-09"},
			{"2024-02-02", "2025-01-22",  "2026-02-10"},
			{"2024-02-03", "2025-01-23",  "2026-02-11"},
			{"2024-02-04", "2025-01-24",  "2026-02-12"},
			{"2024-02-05", "2025-01-25",  "2026-02-13"},
			{"2024-02-06", "2025-01-26",  "2026-02-14"},
			{"2024-02-07", "2025-01-27",  "2026-02-15"},
			{"2024-02-08", "2025-01-28",  "2026-02-16"},
			{"2024-02-10", "2025-01-29",  "2026-02-17"},
			{"2024-02-11", "2025-01-30",  "2026-02-18"},
			{"2024-02-12", "2025-01-31",  "2026-02-19"},
			{"2024-02-13", "2025-02-01",  "2026-02-20"},
			{"2024-02-14", "2025-02-02",  "2026-02-21"},
			{"2024-02-15", "2025-02-03",  "2026-02-22"},
			{"2024-02-16", "2025-02-04",  "2026-02-23"},
			{"2024-02-17", "2025-02-05",  "2026-02-24"},
			{"2024-02-18", "2025-02-06",  "2026-02-25"},
			{"2024-02-19", "2025-02-07",  "2026-02-26"},
			{"2024-02-20", "2025-02-08",  "2026-02-27"},
			{"2024-02-21", "2025-02-09",  "2026-02-28"},
			{"2024-02-22", "2025-02-10",  "2026-03-01"},
			{"2024-02-23", "2025-02-11",  "2026-03-02"},
			{"2024-02-24", "2025-02-12",  "2026-03-03"},
			{"2024-02-25", "2025-02-13",  "2026-03-04"},
			{"2024-02-26", "2025-02-14",  "2026-03-05"},
			{"2024-02-27", "2025-02-15",  "2026-03-06"},
			{"2024-02-28", "2025-02-16",  "2026-03-07"},
			{"2024-02-29", "2025-02-17",  "2026-03-08"}
	};

	private final static Pattern sFreg = Pattern.compile("^(\\d{2})(\\d{2})([\\s\\*])(.+)$");
	private final static Pattern wFreg = Pattern.compile("^(\\d{2})(\\d)(\\d)([\\s\\*])(.+)$");

	private static GregorianCalendar utcCal = null;

	private static int toInt(String str) {
		try { return Integer.parseInt(str); }
		catch(Exception e) { return -1; }
	}
	private synchronized void findFestival() {
		int sM = this.getSolarMonth();
		int sD = this.getSolarDay();
		int lM = this.getLunarMonth();
		int lD = this.getLunarDay();
		int sy = this.getSolarYear();
		Matcher m;
		for (int i=0; i<Lunar.sFtv.length; i++) {
			m = Lunar.sFreg.matcher(Lunar.sFtv[i]);
			if (m.find()) {
				if (sM == Lunar.toInt(m.group(1)) && sD == Lunar.toInt(m.group(2))) {
					this.isSFestival = true;
					this.sFestivalName = m.group(4);
					if ("*".equals(m.group(3))) this.isHoliday = true;
					break;
				}
			}
		}
		for (int i=0; i<Lunar.lFtv.length; i++) {
			m = Lunar.sFreg.matcher(Lunar.lFtv[i]);
			if (m.find()) {
				if (lM == Lunar.toInt(m.group(1)) && lD == Lunar.toInt(m.group(2))) {
					this.isLFestival = true;
					this.lFestivalName = m.group(4);
					if ("*".equals(m.group(3))) this.isHoliday = true;
					break;
				}
			}
		}

		// 月周节日
		int w, d;
		for (int i=0; i<Lunar.wFtv.length; i++) {
			m = Lunar.wFreg.matcher(Lunar.wFtv[i]);
			if (m.find()) {
				if (this.getSolarMonth() == Lunar.toInt(m.group(1))) {
					w = Lunar.toInt(m.group(2));
					d = Lunar.toInt(m.group(3));
					if (this.solar.get(Calendar.WEEK_OF_MONTH)==w &&
							this.solar.get(Calendar.DAY_OF_WEEK)==d) {
						this.isSFestival = true;
						this.sFestivalName += "|" + m.group(5);
						if ("*".equals(m.group(4))) this.isHoliday = true;
					}
				}
			}
		}
		if(sy>1874 && sy<1909) this.description = "光绪" + (((sy-1874)==1)?"元":""+(sy-1874));
		if(sy>1908 && sy<1912) this.description = "宣统" + (((sy-1908)==1)?"元":String.valueOf(sy-1908));
		if(sy>1911 && sy<1950) this.description = "民国" + (((sy-1911)==1)?"元":String.valueOf(sy-1911));
		if(sy>1949) this.description = "共和国" + (((sy-1949)==1)?"元":String.valueOf(sy-1949));
		this.description += "年";
		this.sFestivalName = this.sFestivalName.replaceFirst("^\\|", "");
		this.isFinded = true;
	}

	/**
	 * 返回农历年闰月月份
	 * @param lunarYear
	 *            指定农历年份(数字)
	 * @return 该农历年闰月的月份(数字,没闰返回0)
	 */
	private static int getLunarLeapMonth(int lunarYear) {
		// 数据表中,每个农历年用16bit来表示,
		// 前12bit分别表示12个月份的大小月,最后4bit表示闰月
		// 若4bit全为1或全为0,表示没闰, 否则4bit的值为闰月月份
		int leapMonth = Lunar.lunarInfo[lunarYear - 1900] & 0xf;
		leapMonth = (leapMonth == 0xf ? 0 : leapMonth);
		return leapMonth;
	}

	/**
	 * 返回农历年闰月的天数
	 *
	 * @param lunarYear 指定农历年份(数字)
	 * @return 该农历年闰月的天数(数字)
	 */
	private static int getLunarLeapDays(int lunarYear) {
		// 下一年最后4bit为1111,返回30(大月)
		// 下一年最后4bit不为1111,返回29(小月)
		// 若该年没有闰月,返回0
		return Lunar.getLunarLeapMonth(lunarYear) > 0 ? ((Lunar.lunarInfo[lunarYear - 1899] & 0xf) == 0xf ? 30 : 29) : 0;
	}

	/**
	 * 返回农历年的总天数
	 * @param lunarYear 指定农历年份(数字)
	 * @return 该农历年的总天数(数字)
	 */
	private static int getLunarYearDays(int lunarYear) {
		// 按小月计算,农历年最少有12 * 29 = 348天
		int daysInLunarYear = 348;
		// 数据表中,每个农历年用16bit来表示,
		// 前12bit分别表示12个月份的大小月,最后4bit表示闰月
		// 每个大月累加一天
		for (int i = 0x8000; i > 0x8; i >>= 1) {
			daysInLunarYear += ((Lunar.lunarInfo[lunarYear - 1900] & i) != 0) ? 1
					: 0;
		}
		// 加上闰月天数
		daysInLunarYear += Lunar.getLunarLeapDays(lunarYear);

		return daysInLunarYear;
	}

	/**
	 * 返回农历年正常月份的总天数
	 * @param lunarYear
	 *            指定农历年份(数字)
	 * @param lunarMonth
	 *            指定农历月份(数字)
	 * @return 该农历年闰月的月份(数字,没闰返回0)
	 */
	private static int getLunarMonthDays(int lunarYear, int lunarMonth) {
		// 数据表中,每个农历年用16bit来表示,
		// 前12bit分别表示12个月份的大小月,最后4bit表示闰月
		int daysInLunarMonth = ((Lunar.lunarInfo[lunarYear - 1900] & (0x10000 >> lunarMonth)) != 0) ? 30
				: 29;
		return daysInLunarMonth;
	}
	/**
	 * 取 Date 对象中用全球标准时间 (UTC) 表示的日期
	 *
	 * @param date 指定日期
	 * @return UTC 全球标准时间 (UTC) 表示的日期
	 */
	public static synchronized int getUTCDay(Date date) {
		Lunar.makeUTCCalendar();
		synchronized (utcCal) {
			utcCal.clear();
			utcCal.setTimeInMillis(date.getTime());
			return utcCal.get(Calendar.DAY_OF_MONTH);
		}
	}

	private static synchronized void makeUTCCalendar() {
		if (Lunar.utcCal == null) {
			Lunar.utcCal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		}
	}
	/**
	 * 返回全球标准时间 (UTC) (或 GMT) 的 1970 年 1 月 1 日到所指定日期之间所间隔的毫秒数。
	 * @param y 指定年份
	 * @param m 指定月份
	 * @param d 指定日期
	 * @param h 指定小时
	 * @param min 指定分钟
	 * @param sec 指定秒数
	 * @return 全球标准时间 (UTC) (或 GMT) 的 1970 年 1 月 1 日到所指定日期之间所间隔的毫秒数
	 */
	public static synchronized long UTC(int y, int m, int d, int h, int min, int sec) {
		Lunar.makeUTCCalendar();
		synchronized (utcCal) {
			utcCal.clear();
			utcCal.set(y, m, d, h, min, sec);
			return utcCal.getTimeInMillis();
		}
	}

	/**
	 * 返回公历年节气的日期
	 * @param solarYear 指定公历年份(数字)
	 * @param index 指定节气序号(数字,0从小寒算起)
	 * @return 日期(数字,所在月份的第几天)
	 */
	private static int getSolarTermDay(int solarYear, int index) {
		long l = (long)31556925974.7 * (solarYear - 1900) + solarTermInfo[index] * 60000L;
		l = l + Lunar.UTC(1900,0,6,2,5,0);
		return Lunar.getUTCDay(new Date(l));
	}

	/**
	 * 通过 Date 对象构建农历信息
	 * @param date 指定日期对象
	 */
	public Lunar(Date date) {
		if (date == null)
			date = new Date();
		this.init(date.getTime());
	}

	/**
	 * 通过 TimeInMillis 构建农历信息
	 * @param TimeInMillis
	 */
	public Lunar(long TimeInMillis) {
		this.init(TimeInMillis);
	}

	/**
	 * 通过日期id构建农历
	 * @param dateId   yyyyMMdd
	 */
	public Lunar(Integer dateId){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		try {
			Date date = sdf.parse(dateId + "");
			this.init(date.getTime());
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	private void init(long timeInMillis) {
		this.solar = Calendar.getInstance();
		this.solar.setTimeInMillis(timeInMillis);
		Calendar baseDate = new GregorianCalendar(1900, 0, 31);
		long offset = (timeInMillis - baseDate.getTimeInMillis()) / 86400000;
		// 按农历年递减每年的农历天数，确定农历年份
		this.lunarYear = 1900;
		int daysInLunarYear = Lunar.getLunarYearDays(this.lunarYear);
		while (this.lunarYear < 2100 && offset >= daysInLunarYear) {
			offset -= daysInLunarYear;
			daysInLunarYear = Lunar.getLunarYearDays(++this.lunarYear);
		}
		// 农历年数字

		// 按农历月递减每月的农历天数，确定农历月份
		int lunarMonth = 1;
		// 所在农历年闰哪个月,若没有返回0
		int leapMonth = Lunar.getLunarLeapMonth(this.lunarYear);
		// 是否闰年
		this.isLeapYear = leapMonth > 0;
		// 闰月是否递减
		boolean leapDec = false;
		boolean isLeap = false;
		int daysInLunarMonth = 0;
		while (lunarMonth<13 && offset>0) {
			if (isLeap && leapDec) { // 如果是闰年,并且是闰月
				// 所在农历年闰月的天数
				daysInLunarMonth = Lunar.getLunarLeapDays(this.lunarYear);
				leapDec = false;
			} else {
				// 所在农历年指定月的天数
				daysInLunarMonth = Lunar.getLunarMonthDays(this.lunarYear, lunarMonth);
			}
			if (offset < daysInLunarMonth) {
				break;
			}
			offset -= daysInLunarMonth;

			if (leapMonth == lunarMonth && isLeap == false) {
				// 下个月是闰月
				leapDec = true;
				isLeap = true;
			} else {
				// 月份递增
				lunarMonth++;
			}
		}
		this.maxDayInMonth = daysInLunarMonth;
		// 农历月数字
		this.lunarMonth = lunarMonth;
		// 是否闰月
		this.isLeap = (lunarMonth == leapMonth && isLeap);
		// 农历日数字
		this.lunarDay = (int) offset + 1;
		// 取得干支历
		this.getCyclicalData();
	}

	/**
	 * 取干支历 不是历年，历月干支，而是中国的从立春节气开始的节月，是中国的太阳十二宫，阳历的。
	 * @param
	 */
	private void getCyclicalData() {
		this.solarYear = this.solar.get(Calendar.YEAR);
		this.solarMonth = this.solar.get(Calendar.MONTH);
		this.solarDay = this.solar.get(Calendar.DAY_OF_MONTH);
		// 干支历
		int cyclicalYear = 0;
		int cyclicalMonth = 0;
		int cyclicalDay = 0;

		// 干支年 1900年立春後为庚子年(60进制36)
		int term2 = Lunar.getSolarTermDay(solarYear, 2); // 立春日期
		// 依节气调整二月分的年柱, 以立春为界
		if (solarMonth < 1 || (solarMonth == 1 && solarDay < term2)) {
			cyclicalYear = (solarYear - 1900 + 36 - 1) % 60;
		} else {
			cyclicalYear = (solarYear - 1900 + 36) % 60;
		}

		// 干支月 1900年1月小寒以前为 丙子月(60进制12)
		int firstNode = Lunar.getSolarTermDay(solarYear, solarMonth * 2); // 传回当月「节」为几日开始
		// 依节气月柱, 以「节」为界
		if (solarDay < firstNode) {
			cyclicalMonth = ((solarYear - 1900) * 12 + solarMonth + 12) % 60;
		} else {
			cyclicalMonth = ((solarYear - 1900) * 12 + solarMonth + 13) % 60;
		}

		// 当月一日与 1900/1/1 相差天数
		// 1900/1/1与 1970/1/1 相差25567日, 1900/1/1 日柱为甲戌日(60进制10)
		cyclicalDay = (int) (Lunar.UTC(solarYear, solarMonth, solarDay, 0, 0, 0) / 86400000 + 25567 + 10) % 60;
		this.cyclicalYear = cyclicalYear;
		this.cyclicalMonth = cyclicalMonth;
		this.cyclicalDay = cyclicalDay;
	}

	/**
	 * 取农历年生肖
	 * @return 农历年生肖(例:龙)
	 */
	public String getAnimalString() {
		return Lunar.Animals[(this.lunarYear - 4) % 12];
	}

	/**
	 * 返回公历日期的节气字符串
	 * @return 二十四节气字符串,若不是节气日,返回空串(例:冬至)
	 */
	public String getTermString() {
		// 二十四节气
		String termString = "";
		if (Lunar.getSolarTermDay(solarYear, solarMonth * 2) == solarDay) {
			termString = Lunar.solarTerm[solarMonth * 2];
		} else if (Lunar.getSolarTermDay(solarYear, solarMonth * 2 + 1) == solarDay) {
			termString = Lunar.solarTerm[solarMonth * 2 + 1];
		}
		return termString;
	}


	/**
	 * 取得干支历字符串
	 *
	 * @return 干支历字符串(例:甲子年甲子月甲子日)
	 */
	public String getCyclicalDateString() {
		return this.getCyclicaYear() + "年" + this.getCyclicaMonth() + "月"
				+ this.getCyclicaDay() + "日";
	}

	/**
	 * 年份天干
	 * @return 年份天干
	 */
	public int getTiananY() {
		return Lunar.getTianan(this.cyclicalYear);
	}

	/**
	 * 月份天干
	 * @return 月份天干
	 */
	public int getTiananM() {
		return Lunar.getTianan(this.cyclicalMonth);
	}

	/**
	 * 日期天干
	 * @return 日期天干
	 */
	public int getTiananD() {
		return Lunar.getTianan(this.cyclicalDay);
	}

	/**
	 * 年份地支
	 * @return 年分地支
	 */
	public int getDeqiY() {
		return Lunar.getDeqi(this.cyclicalYear);
	}

	/**
	 * 月份地支
	 * @return 月份地支
	 */
	public int getDeqiM() {
		return Lunar.getDeqi(this.cyclicalMonth);
	}

	/**
	 * 日期地支
	 * @return 日期地支
	 */
	public int getDeqiD() {
		return Lunar.getDeqi(this.cyclicalDay);
	}

	/**
	 * 取得干支年字符串
	 * @return 干支年字符串
	 */
	public String getCyclicaYear() {
		return Lunar.getCyclicalString(this.cyclicalYear);
	}

	/**
	 * 取得干支月字符串
	 * @return 干支月字符串
	 */
	public String getCyclicaMonth() {
		return Lunar.getCyclicalString(this.cyclicalMonth);
	}

	/**
	 * 取得干支日字符串
	 * @return 干支日字符串
	 */
	public String getCyclicaDay() {
		return Lunar.getCyclicalString(this.cyclicalDay);
	}

	/**
	 * 返回农历日期字符串
	 * @return 农历日期字符串
	 */
	public String getLunarDayString() {
		return Lunar.getLunarDayString(this.lunarDay);
	}

	/**
	 * 返回农历日期字符串
	 * @return 农历日期字符串
	 */
	public String getLunarMonthString() {
		return (this.isLeap() ? "闰" : "") + Lunar.getLunarMonthString(this.lunarMonth);
	}

	/**
	 * 返回农历日期字符串
	 * @return 农历日期字符串
	 */
	public String getLunarYearString() {
		return Lunar.getLunarYearString(this.lunarYear);
	}

	/**
	 * 返回农历表示字符串
	 * @return 农历字符串(例:甲子年正月初三)
	 */
	public String getLunarDateString() {
		return this.getLunarYearString() + "年"+ this.getLunarMonthString() + "月" + this.getLunarDayString() + "日";
	}

	/**
	 * 农历年是否是闰月
	 * @return 农历年是否是闰月
	 */
	public boolean isLeap() {
		return isLeap;
	}

	/**
	 * 农历年是否是闰年
	 * @return 农历年是否是闰年
	 */
	public boolean isLeapYear() {
		return isLeapYear;
	}

	/**
	 * 当前农历月是否是大月
	 * @return 当前农历月是大月
	 */
	public boolean isBigMonth() {
		return this.getMaxDayInMonth()>29;
	}

	/**
	 * 当前农历月有多少天
	 * @return 当前农历月有多少天
	 */
	public int getMaxDayInMonth() {
		return this.maxDayInMonth;
	}

	/**
	 * 农历日期
	 * @return 农历日期
	 */
	public int getLunarDay() {
		return lunarDay;
	}

	/**
	 * 农历月份
	 * @return 农历月份
	 */
	public int getLunarMonth() {
		return lunarMonth;
	}

	/**
	 * 农历年份
	 * @return 农历年份
	 */
	public int getLunarYear() {
		return lunarYear;
	}

	/**
	 * 公历日期
	 * @return 公历日期
	 */
	public int getSolarDay() {
		return solarDay;
	}

	/**
	 * 公历月份
	 * @return 公历月份 (不是从0算起)
	 */
	public int getSolarMonth() {
		return solarMonth+1;
	}

	/**
	 * 公历年份
	 * @return 公历年份
	 */
	public int getSolarYear() {
		return solarYear;
	}

	/**
	 * 星期几
	 * @return 星期几(星期日为:1, 星期六为:7)
	 */
	public int getDayOfWeek() {
		return this.solar.get(Calendar.DAY_OF_WEEK);
	}

	/**
	 * 黑色星期五
	 * @return 是否黑色星期五
	 */
	public boolean isBlackFriday() {
		return (this.getSolarDay() == 13 && this.solar.get(Calendar.DAY_OF_WEEK)==6);
	}

	/**
	 * 是否是今日
	 * @return 是否是今日
	 */
	public boolean isToday() {
		Calendar clr = Calendar.getInstance();
		return clr.get(Calendar.YEAR)==this.solarYear &&
				clr.get(Calendar.MONTH)==this.solarMonth &&
				clr.get(Calendar.DAY_OF_MONTH)==this.solarDay;
	}

	public String toLunarString(){
		return getLunarYear() + "年" + getLunarMonthString() + "月" + getLunarDayString();
	}

	/**
	 * 取得公历节日名称
	 * @return 公历节日名称,如果不是节日返回空串
	 */
	public String getSFestivalName() {
		if (!this.isFinded) this.findFestival();
		return this.sFestivalName;
	}

	/**
	 * 取得农历节日名称
	 * @return 农历节日名称,如果不是节日返回空串
	 */
	public String getLFestivalName() {
		if (!this.isFinded) this.findFestival();
		return this.lFestivalName;
	}

	/**
	 * 是否是农历节日
	 * @return 是否是农历节日
	 */
	public boolean isLFestival() {
		if (!this.isFinded) this.findFestival();
		return this.isLFestival;
	}

	/**
	 * 是否是公历节日
	 * @return 是否是公历节日
	 */
	public boolean isSFestival() {
		if (!this.isFinded) this.findFestival();
		return this.isSFestival;
	}

	/**
	 * 是否是节日
	 * @return 是否是节日
	 */
	public boolean isFestival() {
		return this.isSFestival() || this.isLFestival();
	}

	/**
	 * 是否是放假日
	 * @return 是否是放假日
	 */
	public boolean isHoliday() {
		if (!this.isFinded) this.findFestival();
		return this.isHoliday;
	}

	/**
	 * 其它日期说明
	 * @return 日期说明(如:民国2年)
	 */
	public String getDescription() {
		if (!this.isFinded) this.findFestival();
		return this.description;
	}

	/**
	 * 干支字符串
	 * @param cyclicalNumber 指定干支位置(数字,0为甲子)
	 * @return 干支字符串
	 */
	private static String getCyclicalString(int cyclicalNumber) {
		return Lunar.Tianan[Lunar.getTianan(cyclicalNumber)] + Lunar.Deqi[Lunar.getDeqi(cyclicalNumber)];
	}

	/**
	 * 获得地支
	 * @param cyclicalNumber
	 * @return 地支 (数字)
	 */
	private static int getDeqi(int cyclicalNumber) {
		return cyclicalNumber % 12;
	}

	/**
	 * 获得天干
	 * @param cyclicalNumber
	 * @return 天干 (数字)
	 */
	private static int getTianan(int cyclicalNumber) {
		return cyclicalNumber % 10;
	}

	/**
	 * 返回指定数字的农历年份表示字符串
	 * @param lunarYear 农历年份(数字,0为甲子)
	 * @return 农历年份字符串
	 */
	private static String getLunarYearString(int lunarYear) {
		return Lunar.getCyclicalString(lunarYear - 1900 + 36);
	}

	/**
	 * 返回指定数字的农历月份表示字符串
	 * @param lunarMonth 农历月份(数字)
	 * @return 农历月份字符串 (例:正)
	 */
	private static String getLunarMonthString(int lunarMonth) {
		String lunarMonthString = "";
		if (lunarMonth == 1) {
			lunarMonthString = Lunar.lunarString2[4];
		} else {
			if (lunarMonth > 9)
				lunarMonthString += Lunar.lunarString2[1];
			if (lunarMonth % 10 > 0)
				lunarMonthString += Lunar.lunarString1[lunarMonth % 10];
		}
		return lunarMonthString;
	}

	/**
	 * 农历年偏移: 偏移找不到，则向前推移2天再匹配，若依旧无法匹配，则返回null
	 * @param date
	 * @param yearOffsetValue
	 * @return
	 */
	public static Date offsetLunarYear(Date date, Integer yearOffsetValue){
		Date result = offsetLunarYear(date, yearOffsetValue, true);
		if(result == null){
			Calendar testDate = Calendar.getInstance();
			testDate.setTime(date);
			for(int i = 1; i <= 2; i++) {
				testDate.add(Calendar.DAY_OF_YEAR, -1);
				result = offsetLunarYear(testDate.getTime(), yearOffsetValue, true);
				if(result != null){
					return  result;
				}
			}
		}
		return result;
	}

	/**
	 * 农历年偏移：偏移找不到，则返回null
	 * @param date
	 * @param yearOffsetValue
	 * @return 年同期的农历日期，示例： 2024-02-10(元月初一)  返回 2023-01-22(元月初一)
	 */
	public static Date offsetLunarYear(Date date, Integer yearOffsetValue, boolean notFoundReturnNull){
		if(yearOffsetValue == null || yearOffsetValue == 0){
			return date;
		}
		// 先通过mapping获取年份偏移
		Date result = offsetLunarYearByMapping(date, yearOffsetValue);
		if(result != null){
			return result;
		}

		Lunar lunar = new Lunar(date);
		Integer baseLunarYear = lunar.getLunarYear();
		String baseLunarMonthDayString = lunar.getLunarMonth() + ":" + lunar.getLunarDay();

		// 除夕特殊处理
		String cnNewYearMonthDayString = "1:1";
		boolean isBaseDateNewYearEva = false; // 基准日期是否是除夕
		Calendar testDate = Calendar.getInstance();
		testDate.setTime(date);
		testDate.add(Calendar.DAY_OF_YEAR, 1);
		Lunar testLunar = new Lunar(testDate.getTime());
		String testLunarMonthDayString = testLunar.getLunarMonth() + ":" + testLunar.getLunarDay();
		isBaseDateNewYearEva = cnNewYearMonthDayString.equals(testLunarMonthDayString);

		int index = -1;
		int maxCount = 2000;
		int offsetStep =  (yearOffsetValue < 0) ? -1 : 1;

		Calendar targetDate = Calendar.getInstance();
		targetDate.setTime(date);
		targetDate.add(Calendar.DAY_OF_YEAR, yearOffsetValue * 300);
		while(true){
			index++;
			targetDate.add(Calendar.DAY_OF_YEAR, offsetStep);
			Lunar offsetLunar = new Lunar(targetDate.getTime());
			Integer offsetLunarYear = offsetLunar.getLunarYear();
			String offsetLunarMonthDayString = offsetLunar.getLunarMonth() + ":" + offsetLunar.getLunarDay();
			if(baseLunarMonthDayString.equals(offsetLunarMonthDayString) // 农历月/日相同
					&& (baseLunarYear + yearOffsetValue) == offsetLunarYear // 农历的年份偏差相同
					&& !offsetLunar.isLeap()){ // 农历的闰月取第一个月份
				result = targetDate.getTime();
				break;
			}

			// 若是除夕，则获取春节的前一天
			if(isBaseDateNewYearEva  && (baseLunarYear + yearOffsetValue + 1) == offsetLunarYear){ // 此处需要+1，因为除夕到春节跨年
				testLunarMonthDayString = offsetLunar.getLunarMonth() + ":" + offsetLunar.getLunarDay();
				if(testLunarMonthDayString.equals(cnNewYearMonthDayString)){
					Calendar targetNewYearEva = Calendar.getInstance();
					targetNewYearEva.setTime(targetDate.getTime());
					targetNewYearEva.add(Calendar.DAY_OF_YEAR, -1);
					result = targetNewYearEva.getTime();
					break;
				}
			}
			if(index >= maxCount){
				break;
			}
		}

		return result;
	}

	public static Date offsetLunarYearByMapping(Date date, Integer yearOffsetValue){
		if(yearOffsetValue == null || yearOffsetValue == 0){
			return date;
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String dateStr = sdf.format(date);
		String targetDateStr = null;
		for(String[] mapping : yearTbMappingDays){
			int currentDateIndex = -1;
			for(int i = 0; i < mapping.length; i++){
				if(dateStr.equals(mapping[i])){
					currentDateIndex = i;
					break;
				}
			}
			// 未匹配到，不处理
			if(currentDateIndex == -1){
				continue;
			}
			int targetDateIndex = currentDateIndex + yearOffsetValue;
			if(targetDateIndex >= 0 && targetDateIndex < mapping.length){
				targetDateStr = mapping[currentDateIndex + yearOffsetValue];
			}
		}
		if(targetDateStr != null){
			try {
				return sdf.parse(targetDateStr);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/**i
	 * 返回指定数字的农历日表示字符串
	 * @param lunarDay 农历日(数字)
	 * @return 农历日字符串 (例: 廿一)
	 */
	private static String getLunarDayString(int lunarDay) {
		if (lunarDay<1 || lunarDay>30) return "";
		int i1 = lunarDay / 10;
		int i2 = lunarDay % 10;
		String c1 = Lunar.lunarString2[i1];
		String c2 = Lunar.lunarString1[i2];
		if (lunarDay < 11) c1 = Lunar.lunarString2[0];
		if (i2 == 0) c2 = Lunar.lunarString2[1];
		return c1 + c2;
	}

	public static void main(String[] args) {
		Calendar clr = Calendar.getInstance();
		clr.set(2025, 1, 27);
		Lunar lunar = new Lunar(clr.getTimeInMillis());
		/*
		String[] titles = { "日", "一", "二", "三", "四", "五", "六" };
		System.out.println(lunar.getSolarYear() + "年" +lunar.getSolarMonth() + "月"+ lunar.getSolarDay() + "日");
		System.out.println("星期" + titles[lunar.getDayOfWeek() - 1]);
		//System.out.println( lunar.getLunarMonthString() + "月" + lunar.getLunarDayString() + "日");
		System.out.println("农历节日:" + lunar.getLFestivalName());
		System.out.println(lunar.toLunarString());
		System.out.println(lunar.getCyclicaYear() + "年");
		System.out.println(lunar.getCyclicaMonth() + "月");
		System.out.println(lunar.getCyclicaDay() + "日");
		System.out.println(Lunar.Animals[lunar.getDeqiD()] + "日冲" + Lunar.Animals[(lunar.getDeqiD() + 6) % 12] + "生年");
		System.out.println( lunar.getDescription());
		if (!"".equals(lunar.getLFestivalName()))
			System.out.println("农历节日:" + lunar.getLFestivalName());
		if (!"".equals(lunar.getSFestivalName()))
			System.out.println("公历节日:" + lunar.getSFestivalName());
		if (lunar.isHoliday())
			System.out.println("(今天放假)");
		if (!"".equals(lunar.getTermString()))
			System.out.println("今日" + lunar.getTermString());
		if (lunar.isBlackFriday())
			System.out.println("黑色星期五");
		*/


		System.out.println("=============年同期=============");
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		System.out.println("基准日期：" + dateFormat.format(clr.getTime()));
		System.out.println("基准日期（农历）：" + lunar.toLunarString());
		for(int i = 1 ; i<= 3; i++){
			System.out.println("偏移年数：" + i);
			Date date = Lunar.offsetLunarYear(clr.getTime(), i * -1);
			if(date != null) {
				System.out.println(dateFormat.format(date));
				Lunar offsetLunar = new Lunar(date);
				System.out.println(offsetLunar.toLunarString());
			}else {
				System.out.println("无年同期");
			}
		}

		clr.set(2024, 11, 18);
		for(int i = 0 ; i <= 61; i++){
			List<String> info = new ArrayList<>();
			for(int k = 0; k < 3; k++){
				Date date = Lunar.offsetLunarYear(clr.getTime(), k * -1);
				if(date != null) {
					info.add(dateFormat.format(date));
				}else {
					info.add("无年同期");
				}
			}
			clr.add(Calendar.DAY_OF_YEAR, 1);
			System.out.println(info);
		}
	}

}