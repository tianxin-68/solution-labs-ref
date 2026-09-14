package com.bi.queryer.util.excelUtil;

import com.bi.queryer.ssm.mgr.exportImport.Constant;
import com.bi.queryer.ssm.mgr.exportImport.MgrType;
import com.bi.queryer.ssm.mgr.exportImport.SSDExcel;
import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.util.BIUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author contributor
 */
public class ExcelHelper {

    /**
     * 将excel文件流转换成指定的对象集合
     *
     * @param t 类
     * @param fileName
     *            文件全名
     * @param stream
     *            excel文件流
     * @return 指定对象集合
     */
    public static <T> List<T> convertToList(Class<T> t, String fileName, InputStream stream, int rowsnum,
                                            int colnum, int sheet) {
        List<T> listResult = new ArrayList<T>();
        try {
            //通过行列起始读取excel流
            String[][] arrays = readExcel(fileName, stream, rowsnum, colnum, sheet,0);

            if (arrays != null) {
                for (int i = 0; i < arrays.length; i++) {
                    //给类对象赋值
                    T cal = getCurrentList(t, arrays[i],true);
                    listResult.add(cal);
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return listResult;
    }

    public static <T> List<T> convertToList(Class<T> t, String fileName, FileInputStream stream, int rowsnum,
                                            int colnum,int sheet,boolean isOrder) {
        List<T> listResult = new ArrayList<T>();
        try {
            //通过行列起始读取excel流
            String[][] arrays = readExcel(fileName, stream, rowsnum, colnum, sheet,0);

            if (arrays != null) {
                for (int i = 0; i < arrays.length; i++) {
                    //给类对象赋值
                    T cal = getCurrentList(t, arrays[i],isOrder);
                    listResult.add(cal);
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return listResult;
    }

    public static List<JSONObject> convertToJSONObjectList(List<String> names,List<String> dataTypes, String fileName, InputStream stream, int rowsnum,
                                           int colnum, int sheet,int isBigDataImport) {

        List<JSONObject> listResult = new ArrayList<>();
        try {

            //通过行列起始读取excel流
            String[][] arrays = readExcel(fileName, stream, rowsnum, colnum, sheet,isBigDataImport);

            if (arrays != null) {
                for (int i = 0; i < arrays.length; i++) {
                    //给类对象赋值
                    JSONObject cal = getJSONObject(names,dataTypes, arrays[i]);
                    listResult.add(cal);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return listResult;
    }

    public static List<JSONObject> convertToJSONObjectList(List<String> names,List<String> dataTypes,List<Integer> idxList, String fileName, InputStream stream, int rowsnum,
                                                           int colnum, int sheet,int isBigDataImport) {

        List<JSONObject> listResult = new ArrayList<>();
        try {

            //通过行列起始读取excel流
            String[][] arrays = readExcel(fileName, stream, rowsnum, colnum, sheet,isBigDataImport);

            if (arrays != null) {
                for (int i = 0; i < arrays.length; i++) {
                    //给类对象赋值
                    JSONObject cal = getJSONObject(names,dataTypes, idxList,arrays[i]);
                    listResult.add(cal);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return listResult;
    }

    /**
     * 读取Excel文件到二维字符串数组
     *
     * @param fileName 文件名
     * @param stream 文件流
     * @return 二维字符串数组
     * @throws IOException
     */
    private static String[][] readExcel(String fileName, InputStream stream, int rowsnum, int colnum,int sheet,int isBigDataImport)
            throws IOException {
        if (stream == null) {
            return null;
        }

        //是否为大数据量导入
        if (!Enabled.value(isBigDataImport)) {
            String postfix = getPostfix(fileName);
            Workbook wb = null;

            //根据后缀初始化Workbook对象
            if (!Constant.EMPTY.equals(postfix)) {
                if (Constant.OFFICE_EXCEL_2003_POSTFIX.equals(postfix)) {
                    wb = getXLSWorkbook(stream);
                } else if (Constant.OFFICE_EXCEL_2010_POSTFIX.equals(postfix)) {
                    wb = getXLSXWorkbook(stream);
                }
                //读取excel文件
                return readWorkbook(wb, rowsnum, colnum, sheet);
            }
        } else {

            try {

                return ExcelReaderUtil.readExcelXlsx(stream);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return null;
    }

    /**
     * 获得文件后缀名
     *
     * @param path
     * @return
     */
    private static String getPostfix(String path) {
        if (path == null || Constant.EMPTY.equals(path.trim())) {
            return Constant.EMPTY;
        }

        if (path.contains(Constant.POINT)) {
            return path.substring(path.lastIndexOf(Constant.POINT) + 1, path.length());
        }

        return Constant.EMPTY;
    }

    /**
     * 获得xls格式的Workbook
     *
     * @param stream
     * @return
     * @throws IOException
     */
    private static Workbook getXLSWorkbook(InputStream stream) throws IOException {
        Workbook wb = new HSSFWorkbook(stream);

        return wb;
    }

    /**
     * 获得xlsx格式的Workbook
     *
     * @param stream
     * @return
     * @throws IOException
     */
    private static Workbook getXLSXWorkbook(InputStream stream) throws IOException {
        Workbook wb = new XSSFWorkbook(stream);

        return wb;
    }

    /**
     * 读取指定行、列的excel数据到二维字符串数组
     *
     * @param wb excel对象
     * @param rowsnum 行
     * @param colnum 列
     * @return 二维字符串数组
     * @throws IOException
     */
    private static String[][] readWorkbook(Workbook wb, int rowsnum, int colnum,int sheetnum) throws IOException {
        //过滤空行
        Sheet sheet = filterBlankLine(wb.getSheetAt(sheetnum), rowsnum);
        //获取最后一行
        int rows = sheet.getLastRowNum();

        //获取列数
        int columns = sheet.getRow(0).getPhysicalNumberOfCells();

        //行数从0开始,计算时+1
        String[][] data = new String[rows + 1 - rowsnum][columns];

        for (int rownum = rowsnum; rownum <= sheet.getLastRowNum(); rownum++) {
            Row row = sheet.getRow(rownum);
            if (row == null) {
                continue;
            }

            String value;
            for (int cellnum = 0; cellnum < columns; cellnum++) {
                Cell cell = row.getCell(cellnum);
                if (cell == null) {
                    continue;
                } else {
                    value = "";
                }
                //给不同格式的单元格赋值
                switch (cell.getCellType()) {
                    //文本
                    case Cell.CELL_TYPE_STRING:
                        value = cell.getRichStringCellValue().getString();
                        break;
                    //数值
                    case Cell.CELL_TYPE_NUMERIC:
                        short format = cell.getCellStyle().getDataFormat();
                        if (DateUtilLocal.isCellDateFormatted(cell)||format==31) {
                            Date theDate = cell.getDateCellValue();
                            SimpleDateFormat dff = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                            Calendar calendar   =   new GregorianCalendar();
                            calendar.setTime(theDate);
                            theDate=calendar.getTime();

                            value = dff.format(theDate);
                        }else {
                            //为数值赋值，避免大数据变成科学表达式
                            value = NumberToTextConverter.toText(cell.getNumericCellValue());
                        }
                        break;
                    //bool
                    case Cell.CELL_TYPE_BOOLEAN:
                        value = Boolean.toString(cell.getBooleanCellValue());
                        break;
                    //公式
                    case Cell.CELL_TYPE_FORMULA:

                        try {
                            DataFormatter df = new DataFormatter();
                            FormulaEvaluator formulaEval = wb
                                    .getCreationHelper()
                                    .createFormulaEvaluator();
                            value = df.formatCellValue(cell, formulaEval);
                        }catch (Exception e){
                            try {
                                value = String.valueOf(cell.getNumericCellValue());
                            }catch (Exception e1){

                                try {
                                    value = String.valueOf(cell.getRichStringCellValue());
                                }catch (Exception e2){
                                    e2.printStackTrace();
                                }

                            }
                        }

                        if(BIUtil.isNotEmpty(value)){
                            if(value.endsWith(".0")){
                                value = value.substring(0,value.indexOf("."));
                            }

                            if(value.endsWith("%")){
                                value = value.substring(0,value.length()-1);
                                value = value.replaceAll("\n", "");

                                BigDecimal bigDecimal = new BigDecimal(value);
                                BigDecimal b100 = new BigDecimal(100);
                                bigDecimal = bigDecimal.divide(b100);

                                value = bigDecimal.toPlainString();
                            }
                        }


                        break;
                    default:
                        value = "";
                }

                //特殊处理单引号 （'会导致sql报错）
//                if(BIUtil.isNotEmpty(value)) {
//                    if (value.indexOf("'") != -1) {
//                        value= value.replaceAll("'","\"");
//                    }
//                }

                //处理前后空格与换行
                if(BIUtil.isNotEmpty(value)) {
                    value = value.trim();
                }

                data[rownum - rowsnum][cellnum] = value;
            }
        }

        return data;
    }

    /**
     * 给当前传入实际类型的实例赋值
     *
     * @param t
     * @param args
     * @return
     */
    private static <T> T getCurrentList(Class<T> t, String[] args,boolean isOrder) {
        T bean = null;
        Field field = null;

        try {
            bean = t.newInstance();
            Field[] fields = t.getDeclaredFields();

            List<Field> fl=new ArrayList<>();

            if(isOrder){
                fl = getOrderedField(fields);
            }else{
                fl = Arrays.asList(fields);
            }

            for (int i = 0; i < fl.size(); i++) {

                try{

                    // 根据变量名获得变量对象
                    field = fl.get(i);
                    if (field != null) {
                        field.setAccessible(true);
                        // 赋值给bean对象对应的值

                        if(field.getType()==String.class){
                            field.set(bean, args[i]);
                        }else if (field.getType()==Date.class){

                            if(BIUtil.isEmpty(args[i])){
                                field.set(bean, null);
                            }else {
                                SimpleDateFormat formatter = null;

                                if (!args[i].isEmpty() && args[i].indexOf(":") > -1) {
                                    formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                } else {
                                    formatter = new SimpleDateFormat("yyyy-MM-dd");
                                }

                                if (args[i].length() == 4) {
                                    args[i] = args[i] + "-01-01";
                                }
                                Date date = formatter.parse(args[i]);
                                field.set(bean, date);
                            }


                        }else if(field.getType()==Integer.class) {
                            Integer num = 0;
                            if (field.getAnnotation(SSDExcel.class).trueOrFalse()) {
                                num = args[i].equalsIgnoreCase("是") ? 1 : 0;
                            }else{

                                if(BIUtil.isNotEmpty(args[i])){
                                    //excel解析时，1解析为1.0
                                    if(args[i].indexOf(".")!=-1){
                                        args[i] = args[i].substring(0,args[i].indexOf("."));
                                    }
                                    num = Integer.parseInt(args[i]);
                                }
                            }
                            field.set(bean, num);
                        }else if(field.getType()==Double.class) {

                            Double num = (double) 0;
                            if (BIUtil.isNotEmpty(args[i])) {
                                num = Double.parseDouble(args[i]);
                            }
                            field.set(bean, num);

                        }else{
                            field.set(bean, args[i]);
                        }

                    }

                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        return bean;
    }

    private static JSONObject getJSONObject(List<String> names,List<String> dataTypes,String[] args){
        JSONObject jsonObject = new JSONObject();

        try {

            for (int i = 0; i < names.size(); i++) {

                try {
                    String name = names.get(i);
                    String type = dataTypes.get(i);

                    if(!"pkid".equalsIgnoreCase(name)){

                        if(DataType.String.toString().equalsIgnoreCase(type)){
                            String str = args[i];
                            if(BIUtil.isNotEmpty(str)){
                                if(str.endsWith(" 00:00:00")){
                                    str = str.substring(0,str.indexOf(" "));
                                }
                            }
                            jsonObject.put(name,str);
                        }else if (DataType.Date.toString().equalsIgnoreCase(type)||DataType.Datetime.toString().equalsIgnoreCase(type)){

                            if(BIUtil.isEmpty(args[i])){
                                jsonObject.put(name,null);
                            }else {
                                jsonObject.put(name,args[i]);
                            }
                        }else if(DataType.getType(type).isInteger()) {
                            Integer num = 0;
                            if(BIUtil.isNotEmpty(args[i])){

                                //excel解析时，1解析为1.0
                                if(args[i].indexOf(".")!=-1){
                                    args[i] = args[i].substring(0,args[i].indexOf("."));
                                }

                                args[i] =  args[i].replaceAll(",","");

                                num = Integer.parseInt(args[i]);
                            }
                            jsonObject.put(name,num);
                        }else if(DataType.Double.toString().equalsIgnoreCase(type)) {

                            Double num = (double) 0;
                            if (BIUtil.isNotEmpty(args[i])) {

                                args[i] =  args[i].replaceAll(",","");

                                if(args[i].endsWith("%")){
                                    args[i] = args[i].substring(0,args[i].length()-1);
                                    args[i] = args[i].replaceAll("\n", "");

                                    BigDecimal bigDecimal = new BigDecimal(args[i]);
                                    BigDecimal b100 = new BigDecimal(100);
                                    bigDecimal = bigDecimal.divide(b100);

                                    args[i]  = bigDecimal.toPlainString();
                                }
                                num = Double.parseDouble(args[i]);

                                String regx = "^((-?\\d+.?\\d*)[Ee]{1}(-?\\d+))$";//科学计数法正则表达式
                                Pattern pattern = Pattern.compile(regx);
                                if(BIUtil.isNotEmpty(num.toString())&&pattern.matcher(num.toString()).matches()) {
                                    jsonObject.put(name,new BigDecimal(num));
                                }else{
                                    jsonObject.put(name,num);
                                }
                            }else{
                                jsonObject.put(name,num);
                            }


                        }else{
                            jsonObject.put(name,args[i]);
                        }
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }

            }

        }catch (Exception e){
            return jsonObject;
        }


        return jsonObject;
    }

    /**
     * 通过字段映射关系对应excel
     * @param names
     * @param dataTypes
     * @param idxList
     * @param args
     * @return
     */
    private static JSONObject getJSONObject(List<String> names,List<String> dataTypes,List<Integer> idxList,String[] args) {
        JSONObject jsonObject = new JSONObject();

        try {

            for (int i = 0; i < names.size(); i++) {

                //字段对应excel的索引
                Integer idx = idxList.get(i);

                if (idx != -1) {
                    try {
                        String name = names.get(i);
                        String type = dataTypes.get(i);
                        if (DataType.String.toString().equalsIgnoreCase(type)) {
                            String str = args[idx];
                            if (BIUtil.isNotEmpty(str)) {
                                if (str.endsWith(" 00:00:00")) {
                                    str = str.substring(0, str.indexOf(" "));
                                }
                            }
                            jsonObject.put(name, str);
                        } else if (DataType.Date.toString().equalsIgnoreCase(type) || DataType.Datetime.toString().equalsIgnoreCase(type)) {

                            if (BIUtil.isEmpty(args[idx])) {
                                jsonObject.put(name, null);
                            } else {
                                jsonObject.put(name, args[idx]);
                            }
                        } else if (DataType.getType(type).isInteger()) {
                            Integer num = 0;
                            if (BIUtil.isNotEmpty(args[idx])) {

                                //excel解析时，1解析为1.0
                                if (args[idx].indexOf(".") != -1) {
                                    args[idx] = args[idx].substring(0, args[idx].indexOf("."));
                                }

                                args[idx] =  args[idx].replaceAll(",","");

                                num = Integer.parseInt(args[idx]);
                            }
                            jsonObject.put(name, num);
                        } else if (DataType.Double.toString().equalsIgnoreCase(type)) {

                            Double num = (double) 0;
                            if (BIUtil.isNotEmpty(args[idx])) {

                                args[idx] =  args[idx].replaceAll(",","");

                                if(args[idx].endsWith("%")){

                                    args[idx] = args[idx].substring(0,args[idx].length()-1);
                                    args[idx] =  args[idx].replaceAll("\n", "");

                                    BigDecimal bigDecimal = new BigDecimal(args[idx] );
                                    BigDecimal b100 = new BigDecimal(100);
                                    bigDecimal = bigDecimal.divide(b100);

                                    args[idx]  = bigDecimal.toPlainString();
                                }

                                num = Double.parseDouble(args[idx]);

                                String regx = "^((-?\\d+.?\\d*)[Ee]{1}(-?\\d+))$";//科学计数法正则表达式
                                Pattern pattern = Pattern.compile(regx);
                                if(BIUtil.isNotEmpty(num.toString())&&pattern.matcher(num.toString()).matches()) {
                                    jsonObject.put(name,new BigDecimal(num));
                                }else{
                                    jsonObject.put(name,num);
                                }
                            }else{
                                jsonObject.put(name,num);
                            }

                        } else {
                            jsonObject.put(name, args[idx]);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }


            }

        } catch (Exception e) {
            return jsonObject;
        }


        return jsonObject;
    }
    /**
     * 过滤空行
     *
     * @return
     */
    private static Sheet filterBlankLine(Sheet sheet, int rowsnum) {

        boolean flag = false;
        for (int i = rowsnum; i <= sheet.getLastRowNum();) {
            Row r = sheet.getRow(i);
            if (r == null) {
                // 如果是空行（即没有任何数据、格式），直接把它以下的数据往上移动
                sheet.shiftRows(i + 1, sheet.getLastRowNum(), -1);
                continue;
            }
            flag = false;
            //遍历单元格
            for (Cell c : r) {
                //判断单元格是否为空
                if (c.getCellType() != Cell.CELL_TYPE_BLANK) {
                    flag = true;
                    break;
                }
            }
            if (flag) {
                i++;
                continue;
            } else {
                // 如果是空白行（即可能没有数据，但是有一定格式）
                // 如果到了最后一行，直接将那一行remove掉
                if (i == sheet.getLastRowNum())
                    sheet.removeRow(r);
                else {
                    // 如果还没到最后一行，则数据往上移一行
                    sheet.shiftRows(i + 1, sheet.getLastRowNum(), -1);
                }
            }
        }
        return sheet;
    }

    /**
     * 获取当前类型中定义的变量名称
     *
     * @param t 类
     * @return 变量名称的集合
     */
    private static List<String> getFieldName(Class<?> t) {
        List<String> list = new ArrayList<>();
        // 循环此字段数组，获取属性的值
        for (Field field : t.getDeclaredFields()) {
            //设置
            field.setAccessible(true);
            String name = field.getName();
            list.add(name);
        }

        return list;
    }

    private static List<Field> getOrderedField(Field[] fields){
        // 用来存放所有的属性域
        List<Field> fieldList = new ArrayList<>();

        boolean flag =true;
        // 过滤带有注解的Field
        for(Field f:fields){
            if(f.getAnnotation(SSDExcel.class)!=null){
                fieldList.add(f);
                flag = false;
            }
        }

        if(flag){
            return Arrays.asList(fields);
        }

        fieldList.sort(Comparator.comparingInt(m -> m.getAnnotation(SSDExcel.class).order()));
        return fieldList;
    }

    /**
     * 多维分析管理导出
     * @param cloumnNames
     * @param cloumnValues
     * @param type
     * @param response
     */
    public static void exportExcel(String[] cloumnNames, List<String[]> cloumnValues, String type, HttpServletResponse response){

        try{

            //获取文件路径
            MgrType mgrType = MgrType.get(type);
            String fileName = mgrType.getName();
            String path = Thread.currentThread().getContextClassLoader().getResource("").getPath()+"template/"+fileName;
            FileInputStream tps = new FileInputStream(new File(path));
            XSSFWorkbook wb = new XSSFWorkbook(tps);

            XSSFSheet sheet = wb.getSheetAt(0);
            XSSFRow row = null;

            int nI;
            for (nI = 0; nI < cloumnValues.size(); nI++) {
                row = sheet.createRow(nI + 1);

                for(int k =0;k<cloumnValues.get(nI).length;k++){
                    row.createCell(k).setCellValue(cloumnValues.get(nI)[k]);
                }
            }

            response.setHeader("content-disposition", "attachment;filename=" + URLEncoder.encode(fileName, "utf-8"));
            OutputStream out = response.getOutputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            byte[] xlsBytes = baos.toByteArray();
            out.write(xlsBytes);
            out.close();

        }catch (Exception e){
            e.printStackTrace();
        }


    }

    /**
     * 无模板导出 （支持大数据量导出）
     * @param cloumnNames
     * @param cloumnValues
     * @param response
     */
    public static void exportExcel(String fileName,List<String> cloumnNames, List<List<String>> cloumnValues, HttpServletResponse response) {

        OutputStream out = null;

        try {

            SXSSFWorkbook wb = new SXSSFWorkbook();

            Sheet sheet = wb.createSheet(fileName);
            Row row = null;

            //表头
            row = sheet.createRow(0);
            int nI;
            for (nI = 0; nI < cloumnNames.size(); nI++) {
                row.createCell(nI).setCellValue(cloumnNames.get(nI));
            }

            //数据
            for (nI = 0; nI < cloumnValues.size(); nI++) {
                row = sheet.createRow(nI + 1);

                for (int k = 0; k < cloumnValues.get(nI).size(); k++) {
                    row.createCell(k).setCellValue(cloumnValues.get(nI).get(k));
                }
            }

            response.setHeader("content-disposition", "attachment;filename=" + URLEncoder.encode(fileName, "utf-8") + ".xlsx");
            out = response.getOutputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            byte[] xlsBytes = baos.toByteArray();
            out.write(xlsBytes);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {

                if (out != null) {
                    out.close();
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
