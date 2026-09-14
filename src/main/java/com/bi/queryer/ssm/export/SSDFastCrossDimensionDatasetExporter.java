package com.bi.queryer.ssm.export;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.analysis.cfg.total.AnalysisTotalConfig;
import com.bi.queryer.ssm.engine.analysis.operator.OperatorFactory;
import com.bi.queryer.ssm.engine.analysis.operator.impl.TotalOperator;
import com.bi.queryer.ssm.engine.config.QueryConfigure;
import com.bi.queryer.ssm.engine.config.field.QueryField;
import com.bi.queryer.ssm.engine.cross.CrossDimensionQueryEngine;
import com.bi.queryer.ssm.enums.AnalysisTotalType;
import com.bi.queryer.ssm.util.SSDUtil;
import com.bi.queryer.sys.db.DBUtil;
import com.bi.queryer.ssm.engine.result.ResultDataSetColumn;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.download.FileDownloadType;
import com.csvreader.CsvWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.Charset;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 快速导出datagrid数据，用于处理无格式大数据量导出，支持csv和xlsx
 * ！！！注，此导出类仅用于报表设计器配置的datagrid
 * 导出步骤：
 * 1、先通过数据源获取sql
 * 2、通过jdbc接口获取列元数据，用于确定列的顺序和标题
 * 3、对于不可分页的数据源（presto）
 * 3.1 一次性查询所有记录
 * 3.2 每10W条记录存储一个文件
 * 4、对于可分页的数据源
 * 4.1 每次分页查询10W条记录
 * 4.2 每10W条记录存储一个文件
 * 5、对所有文件进行zip压缩返回到response输出流中
 *
 * @author contributor
 */
public class SSDFastCrossDimensionDatasetExporter extends SSDFastDataExporter {

    protected CrossDimensionQueryEngine engine;

    public SSDFastCrossDimensionDatasetExporter(HttpServletRequest request, HttpServletResponse response, Integer totalSize, QueryEngine engine) {
        super(request, response, totalSize, engine);
        this.engine = (CrossDimensionQueryEngine) engine;
    }

    /**
     * 不分页导出csv
     *
     * @return 文件名
     */
    protected String exportCSVByNoPagination() {
        String resultFileName = "";  // 最终返回的文件名

        final List<String> fileNames = splitFileNames();
        if (fileNames == null || fileNames.isEmpty()) {
            return resultFileName;
        }

        // 线程数
        int threadCount = fileNames.size() <= exportThreadCount ? fileNames.size() : exportThreadCount;
        Map<Integer, List<Integer>> threadProcessFiles = new HashMap<Integer, List<Integer>>(); // <线程索引,List<文件索引>>
        for (int i = 0; i < fileNames.size(); i++) {
            Integer threadId = i % threadCount;
            if (threadProcessFiles.containsKey(threadId)) {
                threadProcessFiles.get(threadId).add(i);
            } else {
                List<Integer> fileIndexes = new ArrayList<Integer>();
                fileIndexes.add(i);
                threadProcessFiles.put(threadId, fileIndexes);
            }
        }

        // 数据集
        final SSDFastCrossDimensionDataset dataset = buildDataset();
        final List<ResultDataSetColumn> titles = dataset.titles;
        final CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            final List<Integer> exportFileIndexes = threadProcessFiles.get(i);
            ExecutorService executorService = Executors.newSingleThreadExecutor();

            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    for (Integer index : exportFileIndexes) {
                        Integer fromIndex = index * singleFileMaxSize;
                        Integer toIndex = (index + 1) * singleFileMaxSize;
                        if (toIndex > totalSize) {
                            toIndex = totalSize;
                        }
                        if (dataset.rows == null) {
                            continue;
                        }
                        if (dataset.rows.size() < toIndex) {
                            toIndex = dataset.rows.size();
                        }
                        List<Map<String, Object>> rows = dataset.rows.subList(fromIndex, toIndex);
                        exportCSVByMultiTitle(fileNames.get(index), titles, rows);
                    }
                    // 子线程处理完后，通知主线程减少计数
                    countDownLatch.countDown();
                }
            });
        }

        try {
            countDownLatch.await();
            resultFileName = this.getResultFileName(fileNames);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultFileName;
    }

    /**
     * csv导出
     */
    protected void exportCSVByMultiTitle(String fileName, List<ResultDataSetColumn> columns, List<Map<String, Object>> rows) {
        if (columns == null || columns.isEmpty() || rows == null) {
            return;
        }
        long t1 = System.currentTimeMillis();
        OutputStream fileOut = null;
        CsvWriter csvWriter = null;
        try {
            String sysEncoding = System.getProperty("file.encoding");
            fileOut = new FileOutputStream(new File(fileName));
        	/*
			if ("UTF-8".equalsIgnoreCase(sysEncoding)) {
				fileOut.write(0xFEFF);
				fileOut.flush();
			}
			*/

            // 根据系统编码添加bom文件头，处理csv乱码问题
            byte[] uft8bom = {(byte) 0xef, (byte) 0xbb, (byte) 0xbf};
            fileOut.write(uft8bom);
            fileOut.flush();

            csvWriter = new CsvWriter(fileOut, ',', Charset.forName("UTF-8"));

            List<List<String>> headerTitles = buildExportHeaderTitles(columns);
            for (List<String> headerTitle : headerTitles) {
                String[] headers = new String[headerTitle.size()];
                headerTitle.toArray(headers);
                csvWriter.writeRecord(headers);
            }

            // 写内容
            List<ResultDataSetColumn> exportColumns = ListUtil.list(false);
            columns.forEach(item -> {
                exportColumns.addAll(item.getLeafChildren());
            });
            for (Map<String, Object> row : rows) {
                String[] content = new String[exportColumns.size()];
                for(int i=0;i<exportColumns.size();i++){
                    Object value = row.get(exportColumns.get(i).getCode());
                    content[i] = ObjectUtil.isEmpty(value)?"":value.toString();
                }
                csvWriter.writeRecord(content, true);
            }

            csvWriter.flush();
            fileOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (csvWriter != null) {
                    csvWriter.close();
                }
                if (fileOut != null) {
                    fileOut.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        long t2 = System.currentTimeMillis();
        System.out.println("导出单个文件【" + fileName + "】耗时(毫秒)：" + (t2 - t1));
    }

    private List<List<String>> buildExportHeaderTitles(List<ResultDataSetColumn> columns) {
        List<List<String>> headerTitles = ListUtil.list(false);
        Integer maxLevel = columns.stream().max((v1, v2) -> Integer.compare(v1.getMaxLevel(), v2.getMaxLevel())).get().getMaxLevel();
        for (int i = 0; i < maxLevel; i++) {
            List<String> titleArr = ListUtil.list(false);
            headerTitles.add(titleArr);
        }
        buildHeaderData(columns, headerTitles, 0);
        return headerTitles;
    }

    private void buildHeaderData(List<ResultDataSetColumn> columns, List<List<String>> headerTitles, int level) {
        for (ResultDataSetColumn column : columns) {
            headerTitles.get(level).add(column.getTitle());
            if (CollectionUtil.isEmpty(column.getChildren())) {
                for (int i = level + 1; i < headerTitles.size(); i++) {
                    headerTitles.get(i).add("");
                }
            } else {
                Integer childNum = column.getLeafNum();
                for (int i = 1; i < childNum; i++) {
                    headerTitles.get(level).add("");
                }
                buildHeaderData(column.getChildren(), headerTitles, level + 1);
            }
        }
    }

    /**
     * 获取数据集
     *
     * @return
     */
    protected SSDFastCrossDimensionDataset buildDataset() {
        SSDFastCrossDimensionDataset dataset = new SSDFastCrossDimensionDataset();

        if (StringUtil.isEmpty(this.exportSql)) {
            return dataset;
        }

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {

            conn = DBUtil.getConn(DBUtil.getDataSourceType().getKey());
            stmt = conn.createStatement();

            long t1 = System.currentTimeMillis();

            stmt.setFetchSize(20000);
            stmt.execute(this.exportSql);
            rs = stmt.getResultSet();

            long t2 = System.currentTimeMillis();
            System.out.println("查询SQL耗时(毫秒)：" + (t2 - t1));

            if (rs == null) {
                return dataset;
            }
            rs.setFetchSize(20000);

            // 元数据
            ResultSetMetaData metadata = rs.getMetaData();

            int columnCount = metadata.getColumnCount();

//            List<String> titles = new ArrayList<String>();

            Map<Integer, String> exportIndexes = new LinkedHashMap<>();
            Map<Integer, String> appendStringMap = new HashMap<>();

            QueryConfigure queryConfigure = engine.getConfig();
            Map<String, QueryField> queryFields = queryConfigure.getResult().getFields().stream().collect(Collectors.toMap(QueryField::getCode, QueryField -> QueryField, (f1, f2) -> f1));

            //迭代元数据
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metadata.getColumnLabel(i);
//                String title = columnName;
                if(columnName.contains(BIConsts.ROW_TOTAL_COLUMN_CODE)){
                    // 行总计特殊处理
//                    title = BIConsts.ROW_TOTAL_COLUMN_TITLE;
                    appendStringMap.put(i, "");
                }else {
                    String queryFieldName = columnName.split(BIConsts.COLUMN_DIM_FIELD_SUFFIX)[0];
                    QueryField queryField = queryFields.get(queryFieldName);
                    if (queryField == null) {
                        continue;
                    }
                    appendStringMap.put(i, queryField.getMeta().getExportAppendString() == null ? "" : queryField.getMeta().getExportAppendString());
//                    String fieldTitle = BIUtil.isEmpty(queryField.getDisplayTitle()) ? queryField.getMeta().getTitle() : queryField.getDisplayTitle();
//                    title = this.parseTitle(fieldTitle);
                }
                exportIndexes.put(i, columnName);
//                titles.add(title);
            }
            long t3 = System.currentTimeMillis();
            System.out.println("获取元数据耗时(毫秒)：" + (t3 - t2));

            //迭代结果集
            List<Map<String, Object>> rows = new ArrayList<>(totalSize);
            Map<String, Boolean> isNullColumns = new HashMap<>(); // 是否是空列
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (Integer exportIndex : exportIndexes.keySet()) {
                    Object value = rs.getObject(exportIndex);
                    // 处理NaN数字，避免查询格式化后出现乱码
                    if(value instanceof Double) {
                        Double d = (Double) value;
                        if (Double.isNaN(d) || Double.isInfinite(d)) {
                            value = null;
                        }
                    }
                    String columnCode = exportIndexes.get(exportIndex);
                    boolean isNullColumn = isNullColumns.get(columnCode) == null ? true : isNullColumns.get(columnCode);
                    isNullColumns.put(columnCode, isNullColumn && value == null);
                    row.put(columnCode, value == null ? "" : appendStringMap.get(exportIndex) + value);
                }
                rows.add(row);
            }

            long t4 = System.currentTimeMillis();
            System.out.println("获取所有记录(" + rows.size() + ")耗时(毫秒)：" + (t4 - t3));

            List<ResultDataSetColumn> columns = this.engine.buildDataSetColumns(isNullColumns);


            // 添加行总计列
            AnalysisTotalConfig totalConfig = this.engine.getConfig().getAnalysis().getTotal();

            if(totalConfig.isActive()){
                List<AnalysisTotalType> totalTypes = totalConfig.getItems().stream().map(c->c.getTotalType()).collect(Collectors.toList());

                List<ResultDataSetColumn> totalColumns = new ArrayList<>();
                for(AnalysisTotalType totalType : totalTypes){
                    TotalOperator totalOperator = OperatorFactory.getTotalOperator(totalType);
                    if(totalOperator != null){
                        totalColumns.addAll(totalOperator.createTotalColumns(this.engine.getConfig()));
                    }
                }
                columns.removeAll(totalColumns);
                columns.addAll(totalColumns);
            }

            dataset.titles = columns;
            dataset.rows = rows;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return dataset;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }

    public QueryEngine getEngine() {
        return engine;
    }

    public void setEngine(CrossDimensionQueryEngine engine) {
        this.engine = engine;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public FileDownloadType getFileType() {
        return fileType;
    }

    public void setFileType(FileDownloadType fileType) {
        this.fileType = fileType;
    }

    public Integer getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(Integer totalSize) {
        this.totalSize = totalSize;
    }

}
