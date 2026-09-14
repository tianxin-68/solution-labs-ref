package com.bi.queryer.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.regex.Pattern;


/**
 * 常用的字符串处理静态方法类
 *
 */
public class StringUtil {
    final public static String DATE_FORMATE_ALL = "yyyy-MM-dd HH:mm:ss";

    final public static String DATE_FORMATE_DATE = "yyyy-MM-dd";

    // 定义数据格式
    final public static DecimalFormat DOUBLE_FORMATE = new DecimalFormat(
            "#####0.00");

    /**
     * 对一个字符串进行格式化，将a,x,s或者a;x;s类型的串处理为'a','x','s'，优先,其次;
     * */
    public static String formatSeparateStrForDb(String str){
    	if(StringUtil.isEmpty(str)){
    		return str;
    	}
    	String end="";
    	if(str.indexOf(",")>-1){
    		String[] str_subs=str.split(",");
    		end=castStringArrayToInsql(str_subs);
    	}else if(str.indexOf(";")>-1){
    		String[] str_subs=str.split(";");
    		end=castStringArrayToInsql(str_subs);
    	} else{//都没有，直接返回
    		end=str;
    	}
    	return end;
    }
    /**
     * 将字符串中所有的空格去掉
     */
    public static String cutSpace(String str) {
        if (str == null)
            return null;
        char[] chr = str.toCharArray();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < chr.length; i++) {
            if (chr[i] != ' ' && chr[i] != '　' && chr[i] != ' ') {
                sb.append(chr[i]);
            }
        }
        return sb.toString();
    }

    /**
     * 截取一定长度字符串
     */
    static public String subString(Object obj, int Start, int End)
            throws Exception {
        if (obj == null)
            return "";
        String str = obj.toString();
        if (Start < 0) {
            Start = 0;
        }
        if (End > str.length() - Start) {
            End = str.length() - Start;
        }

        char chrArry[] = str.toCharArray();
        char tempArry[] = new char[End - Start];
        int n = 0;
        for (int i = Start; i < End; i++) {
            tempArry[n] = chrArry[i];
            n++;
        }
        return new String(tempArry);
    }

    /**
     * 判断字符串在另一字符串中出现的次数
     */
    public static int countMatches(String str, String sub) {
        if (str == null)
            return 0;
        int count = 0;
        for (int idx = 0; (idx = str.indexOf(sub, idx)) != -1; idx += sub
                .length())
            count++;

        return count;
    }

    /**
     * 去除右边的空格等特殊字符
     */
    public static String rTrim(String source) {

        if (source == null || source.equalsIgnoreCase("")) {
            return source;
        } else {
            String flag = "";
            flag = source.substring(source.length() - 1);
            while (flag.equals("    ") || flag.equals("\t") || flag.equals("\r")
                    || flag.equals("\n")) {
                source = source.substring(0, source.length() - 1);
                if (source.length() == 0) {
                    break;
                } else {
                    flag = source.substring(source.length() - 1);
                }
            }
        }
        return source;

    }

    /**
     * 清除特特殊的符号"<",">","'"全部换在全角
     *
     * @param 原字符串
     * @return 处理后的字符
     */
    public static String clearSpecialChar(String source) {
        String specila[] = { "<", ">", "'" };
        String tarchar[] = { "〈", "〉", "‘" };
        if (source != null && !source.equalsIgnoreCase("")) {
            for (int i = 0; i < specila.length; i++) {
                source = source.replaceAll(specila[i], tarchar[i]);

            }
        }
        return source;

    }

    /**
     * 清除特特殊的符号"<",">","'",回车，和换行全部换在全角
     *
     * @param source
     * @return 处理后的字符
     */
    public static String clearSpecialCharAll(String source) {
        String specila[] = { "<", ">", "'", "\r", "\n", "\t" };
        String tarchar[] = { "〈", "〉", "‘", "", "", "" };
        if (source != null && !source.equalsIgnoreCase("")) {
            for (int i = 0; i < specila.length; i++) {
                source = source.replaceAll(specila[i], tarchar[i]);

            }
        }
        return source;

    }

    /**
     * 判断字符串是不是全是数字
     *
     * @param strnum
     *            要判断的字符串
     * @return 全是数字返回true
     */
    public static boolean isNumber(String strnum) {
        Pattern p = Pattern.compile("^\\d+$");
        return p.matcher(strnum).find();

    }

    /**
     * 判断字符串是否是数字类型：包含整数和小数
     *
     * @param strnum
     * @return 是数字类型返回true
     */
    public static boolean isAllNumberType(String strnum) {
        Pattern p = Pattern.compile("^[0-9]+.?[0-9]*$");
        return p.matcher(strnum).find();

    }

    /**
     * 字符串转int，如果字符串为空或非数字型则为defaultInt
     *
     * @param str
     *            源字符串
     * @param defaultInt
     *            默认值
     * @return 转成int后的值
     */
    public static int safeStringToInt(String str, int defaultInt) {
        if (isEmpty(str) || !isNumber(str)) {
            return defaultInt;
        } else {
            return Integer.parseInt(str);
        }
    }

    /**
     * 字符串转double，如果字符串为空或非数字型则为defaultDouble
     *
     * @param str
     *            源字符串
     * @param defaultDouble
     *            默认值
     * @return 转成double后的值
     */
    public static double safeStringToDouble(String str, double defaultDouble) {
        if (isEmpty(str) || !isAllNumberType(str)) {
            return defaultDouble;
        } else {
            return Double.parseDouble(str);
        }
    }

    /**
     * 判断一个字符串数组中是否包含某字串
     *
     * @param str[]
     *            要检查的字符数组
     * @param s
     *            要检查的字符
     * @return 存在该字符则返回true
     */
    public static boolean arrayContainStr(String[] str, String s) {
        boolean rtn = false;
        if (str != null && str.length > 0 && s != null) {
            for (int i = 0; i < str.length; i++) {
                if (str[i] != null && str[i].equals(s)) {
                    rtn = true;
                    break;
                }
            }
        }
        return rtn;
    }

    /**
     * 判断字符串是否空串， 字符串为“null”或“NULL”时也视为空
     *
     * @param str
     *            要进行空值判断的字符串
     * @return 为空则返回true
     */
    public static boolean isEmpty(String str) {
        boolean rtn = false;
        if (str == null || str.equalsIgnoreCase("")
                || str.equalsIgnoreCase("null")) {
            rtn = true;
        }
        return rtn;

    }

    /**
     * 对空对象的处理
     *
     * @param str
     * @return
     */
    public static String ifNull(Object obj) {

        if (obj == null || obj.equals("null") || obj.equals("NULL")) {
            return "";
        }
        return obj.toString().trim();
    }

    /**
     * @param obj
     * @return
     */
    public static String ifNullToZero(Object obj) {
        if (obj == null || "".equals(obj) || "null".equals(obj)
                || "NULL".equals(obj)) {
            return "0";
        }
        return obj.toString();
    }

    /**
     * @param obj
     * @return
     */
    public static String ifNullToRep(Object obj,String rep) {
        if (obj == null || "".equals(obj) || "null".equals(obj)
                || "NULL".equals(obj)) {
            return rep;
        }
        return obj.toString();
    }

    /**
     * num为几，则小数点后几个0
     */
    public static String ifNullToZeroZ(Object obj, int num) {
        if (obj == null || "".equals(obj) || "null".equals(obj)
                || "NULL".equals(obj)) {
            String end = "0.0";
            for (int i = 1; i < num; i++) {
                end += "0";
            }
            return end;
        }
        return obj.toString();
    }



    /**
     * 处理字符串，如果字符串超过某个长度则截取一定长度的字符，后面用省略号替代。
     *
     * @param str
     *            原字符串
     * @param i
     *            截取多少个字符
     * @return
     */
    public static String tosubString(String str, int i) {
        int x = str.length();
        if (x > i) {
            String strtemp = str.substring(0, i);
            str = strtemp + "...";
        }
        return str;
    }

    /**
     * 读取文本文件
     *
     * @param FilePath
     * @return
     */
    public ArrayList GetListByTxt(String FilePath) {
        ArrayList list = null;
        try {
            File file = new File(FilePath);
            if (file.exists()) {
                list = new ArrayList();
                FileReader fread = new FileReader(FilePath);
                BufferedReader br = new BufferedReader(fread);
                String s1 = null;
                int line = 0;
                while ((s1 = br.readLine()) != null) {
                    ++line;
                    // System.out.println("line"+line+":"+s1);
                    list.add(s1);
                }
                br.close();
                fread.close();
            } else {
                return null;
            }

        } catch (IOException e) {
        }
        return list;
    }

    /**
     * 转化编码为gb2312
     *
     * @param src
     * @return
     */
    public static String togb312(String src) {
        String tar = null;
        try {
            tar = new String(src.getBytes("ISO-8859-1"), "GBK");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return tar;
    }

    public static String repString(String str) {
        str = str.replaceAll("\\\\", "\\\\\\\\");
        return str.replaceAll("'", "\\\\'");
    }

    /**
     * 如果字符串是空字符串，则返回null
     */
    public static String ifEmpty(String str) {

        if (str == null || str.equalsIgnoreCase("")
                || str.equalsIgnoreCase("null")) {
            return null;
        }
        return str.trim();

    }

    /**
     * 处理插入数据库时的单引号
     * */
    public static String optErrorSqlStr(String str){
        if(str==null||str.equals("")){
            return "";
        }else{
            return str.replaceAll("'", "''").trim();
        }
    }

	/**
	 * 将字符串数组转换为'','',''格式
	 * */
	public static String castStringArrayToInsql(String[] str){
		String end="";
		for(int i=0;i<str.length;i++){
			end=end+"'"+str[i]+"',";
		}
		end=dropStrLast(end,1,",");
		return end;
	}

    /**
     * 给字符串加单引号
     * */
    public static String addSqlSignForStr(String str){
        if(StringUtil.isEmpty(str)) return "";
        if(!str.endsWith("'")){str=str+"'";}
        if(!str.startsWith("'")){str="'"+str;}
        return str;
    }

    /**
     * 去掉字符串后几位，一般用于去掉拼接字符串的最后的,
     * @param rep 用于判断结尾是否为rep，是则去掉
     * */
    public static String dropStrLast(String str,int num,String rep){
        if(isEmpty(str)){return "";}
        if(str.endsWith(rep)){str= str.substring(0, str.length()-num);}
        return str;
    }
    /**
     * 去掉字符串后几位，一般用于去掉拼接字符串的最后的,
     * */
    public static String dropStrLast(String str,int num){
        if(isEmpty(str)){return "";}
        return str.substring(0, str.length()-num);
    }
    /**
     * 去掉字符串前几位，一般用于去掉拼接字符串的最前面的,
     * */
    public static String dropStrTop(String str,int num){
        if(isEmpty(str)){return "";}
        return str.substring(num);
    }

    /**
     * 获取字符串的长度，如果有中文，则每个中文字符计为2位
     *
     * @param value
     *            指定的字符串
     * @return 字符串的长度
     */
    public static int getChineselength(String value) {
        int valueLength = 0;
        String chinese = "[\u0391-\uFFE5]";
        /* 获取字段值的长度，如果含中文字符，则每个中文字符长度为2，否则为1 */
        for (int i = 0; i < value.length(); i++) {
            /* 获取一个字符 */
            String temp = value.substring(i, i + 1);
            /* 判断是否为中文字符 */
            if (temp.matches(chinese)) {
                /* 中文字符长度为2 */
                valueLength += 2;
            } else {
                /* 其他字符长度为1 */
                valueLength += 1;
            }
        }
        return valueLength;
    }

    /**
     * 打印对象里的属性值
     * */
    public static void printObj(String classname,Object o){

    		try {
    			Class<?> c = null;
    			c = Class.forName(classname);  //KpiVO.class.getName(),或者写死全路径
    			Field [] fields = c.getDeclaredFields();

    			for(Field f:fields){
    			    f.setAccessible(true);
    			    String field = f.toString().substring(f.toString().lastIndexOf(".")+1);         //取出属性名称
    	            System.out.println(field+" --> "+f.get(o));
    			}
    			System.out.println();
    		} catch (Exception e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}

    }

    /**
     * 生成长度为N的重复字符串
     * */
    public static String createRepeatString(String value,Integer num){
        String result = "";
        for(int i=0;i<num;i++){
            result+=value;
        }
        return result;
    }

//    public static void main(String [] avgs){
//    	String s1="asd,bsd;sd";
//    	String s=formatSeparateStrForDb(s1);
//    	System.out.println(s);
//    }
}

