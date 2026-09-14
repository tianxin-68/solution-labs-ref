package com.bi.queryer.ssm.util;

import com.bi.queryer.ssm.engine.result.ResultDataSet;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.sys.db.DataType;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.csvreader.CsvWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public abstract class CsvUtil {

    /**
     * 展平后的列信息
     */
    public static class FlatColumn {
        private String code;
        private String flatTitle;
        private String dataType;

        public FlatColumn(String code, String flatTitle, String dataType) {
            this.code = code;
            this.flatTitle = flatTitle;
            this.dataType = dataType;
        }

        public String getCode() {
            return code;
        }

        public String getFlatTitle() {
            return flatTitle;
        }

        public String getDataType() {
            return dataType;
        }
    }

    public static class CsvDataset {
        public String csvContent = "";
        public String[] headers = new String[0];
        public Integer recordSize = 0;
        public Double storageSize = 0.0;
        public String csvDatasetUrl = "";
        public String previewCsvDatasetUrl = "";
    }

    public static CsvDataset toCsv(ResultDataSet dataset){
        return toCsv(dataset, ',');
    }

    public static CsvDataset toCsv(ResultDataSet dataset,char splitChar) {
        CsvDataset csvDataset = new CsvDataset();
        if (dataset == null ) {
            return csvDataset;
        }

        // 展平多级表头为一级表头
        List<FlatColumn> flatColumns = flattenColumns(dataset.getColumns());
        if (flatColumns.isEmpty()) {
            return csvDataset;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CsvWriter csvWriter = null;
        try {

            // 根据系统编码添加bom文件头，处理csv乱码问题
            /*
            byte[] uft8bom = {(byte) 0xef, (byte) 0xbb, (byte) 0xbf};
            outputStream.write(uft8bom);
            outputStream.flush();
             */

            csvWriter = new CsvWriter(outputStream, splitChar, StandardCharsets.UTF_8);

            // 写入表头
            String[] headers = new String[flatColumns.size()];
            for (int i = 0; i < flatColumns.size(); i++) {
                String flatTitle = flatColumns.get(i).getFlatTitle();
                // 去掉无权限提示后缀，避免表头，调用方识别错误
                if (flatTitle != null) {
                    flatTitle = flatTitle.replace(BIConsts.NO_AUTH_COLUMN_TIPS, "");
                }
                headers[i] = flatTitle;
            }
            csvWriter.writeRecord(headers);

            csvDataset.headers = headers;

            // 写入数据行
            for (Map<String, Object> row : dataset.getRows()) {
                String[] values = new String[flatColumns.size()];
                for (int i = 0; i < flatColumns.size(); i++) {
                    FlatColumn flatColumn = flatColumns.get(i);
                    Object value = row.get(flatColumn.getCode());
                    // 去掉数值的千分位
                    if(value != null){
                        if(DataType.getType(flatColumn.getDataType()) != DataType.String) {
                            value = value.toString().replace(",", "");
                        }else {
                            value = value.toString().replace(",", "，");
                        }
                        // 若是日期，则去掉日期的同环比的附加信息
                        // 如：20250301 六 || 年同(-1) 20240301 五  -> 20250301
                        if("dt".equalsIgnoreCase(flatColumn.getCode()) && value.toString().contains("||")  && value.toString().contains(" ")) {
                            value = value.toString().split("\\|\\|")[0];
                            value = value.toString().split(" ")[0];
                        }
                    }
                    values[i] = value == null ? "" : String.valueOf(value);
                }
                csvWriter.writeRecord(values);
            }

            csvWriter.flush();
            outputStream.flush();
            csvDataset.csvContent = outputStream.toString(StandardCharsets.UTF_8.name());
            csvDataset.recordSize = dataset.getRows().size();
            csvDataset.storageSize = (csvDataset.csvContent.getBytes(StandardCharsets.UTF_8.name()).length * 1.00 / 1024);
            return csvDataset;
        } catch (IOException e) {
            System.out.println("CSV转换失败"+ e.getMessage());
            e.printStackTrace();
            return csvDataset;
        } finally {
            try {
                if (csvWriter != null) {
                    csvWriter.close();
                }
                outputStream.close();
            } catch (IOException e) {
                System.out.println("关闭CSV写入流失败"+ e.getMessage());
                e.printStackTrace();
            }
            return csvDataset;
        }
    }

    /**
     * 展平多级表头为一级表头
     *
     * @param columns 原始列定义
     * @return 展平后的列定义列表
     */
    public static List<FlatColumn> flattenColumns(List<ResultDataSetColumn> columns) {
        List<FlatColumn> flatColumns = new ArrayList<>();
        if (BIUtil.isEmpty(columns)) {
            return flatColumns;
        }

        for (ResultDataSetColumn column : columns) {
            flattenColumnRecursive(column, "", flatColumns);
        }

        return flatColumns;
    }

    /**
     * 递归展平列，处理多级表头
     *
     * @param column      当前列
     * @param parentTitle 父级标题路径
     * @param flatColumns 展平后的列列表
     */
    public static void flattenColumnRecursive(ResultDataSetColumn column, String parentTitle, List<FlatColumn> flatColumns) {
        if (column == null) {
            return;
        }

        String currentTitle = BIUtil.isEmpty(column.getTitle()) ? "" : column.getTitle();
        String fullTitle;

        if (BIUtil.isEmpty(parentTitle)) {
            fullTitle = currentTitle;
        } else {
            fullTitle = parentTitle + "_" + currentTitle;
        }

        String dataType = BIUtil.isEmpty(column.getDataType()) ? DataType.String.toString() : column.getDataType();

        // 如果当前列有子列，递归处理子列
        if (!BIUtil.isEmpty(column.getChildren())) {
            for (ResultDataSetColumn child : column.getChildren()) {
                flattenColumnRecursive(child, fullTitle, flatColumns);
            }
        } else {
            // 叶子节点，添加到展平列表
            // 如果没有code，使用title作为code
            String code = BIUtil.isEmpty(column.getCode()) ? column.getTitle() : column.getCode();
            flatColumns.add(new FlatColumn(code, fullTitle, dataType));
        }
    }

}
