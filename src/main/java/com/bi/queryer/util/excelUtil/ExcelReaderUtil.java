package com.bi.queryer.util.excelUtil;

import com.bi.queryer.util.BIUtil;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

public class ExcelReaderUtil {

    //excel2007扩展名
    public static final String EXCEL07_EXTENSION = ".xlsx";


    //读取xlsx格式
    public static String[][] readExcelXlsx(InputStream in) throws Exception {

        String[][] result = new String[][]{};

        long t1 = System.currentTimeMillis();
        ExcelXlsxReader excelXls = new ExcelXlsxReader();
        List<List<String>> dataList = excelXls.process(in);

        long t2 = System.currentTimeMillis();

        if (BIUtil.isNotEmpty(dataList)) {
            result = new String[dataList.size()][dataList.get(0).size()];
            for (int i = 0; i < dataList.size(); i++) {
                List<String> cols = dataList.get(i);
                for (int j = 0; j < cols.size(); j++) {
                    result[i][j] = cols.get(j);
                }
            }
        }

        System.out.println("解析excel耗时：" + (t2 - t1) / 1000);


        return result;
    }

    public static void main(String[] args) throws Exception {
        String path = "D:\\12月-技师红包 (1).xlsx";
        InputStream is = new FileInputStream(path);
        ExcelReaderUtil.readExcelXlsx(is);
    }
}
