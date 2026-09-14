package com.bi.queryer.ssm.export;

import com.bi.queryer.ssm.engine.QueryEngine;
import com.bi.queryer.ssm.engine.export.ExportEngine;
import com.bi.queryer.ssm.engine.export.QueryExportFactory;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.util.download.FileDownloadType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.net.URLEncoder;

/**
 * @Author contributor
 * @Date 20:30 2023-08-14
 * @Description 多维分析导出器
 **/
public class SSMExporter {
    protected HttpServletRequest request;

    protected HttpServletResponse response;

    protected QueryEngine engine;

    protected FileDownloadType fileType = FileDownloadType.CSV;

    protected Integer totalSize = -1;

    protected OutputStream out = null;

    /**
     * 单个文件最大记录数
     */
    protected Integer singleFileMaxSize = 50000;

    /**
     * 导出时的线程数
     */
    protected Integer exportThreadCount = 5;

    /**
     * 文件绝对路径前缀
     */
    protected String fileAbsPathPrefix = "grid";

    /**
     * 导出sql
     */
    protected String exportSql = "";

    public SSMExporter(HttpServletRequest request, HttpServletResponse response, Integer totalSize, QueryEngine engine) {
        this.request = request;
        this.response = response;
        this.engine = engine;
        this.totalSize = totalSize;

        singleFileMaxSize = Integer.valueOf(SC.v("export.single.file.max.size",  "50000"));
    }

    /**
     * 导出
     */
    public ResponseMessage export(String fileName, Double maxSize) {
        boolean success = false;
        String exceptionFlag = "false";
        fileType = FileDownloadType.CSV; // 暂时只支持csv

        long t1 = System.currentTimeMillis();

        InputStream is = null;
        try {

            // 初始化
            this.init();

            BigDecimal maxExportRows;
            if(maxSize!=null&&maxSize>0) {
                maxExportRows = new BigDecimal(maxSize);
            }else{
                maxExportRows = new BigDecimal(SC.v("ssm.export.rows","200"));
            }

            BigDecimal bigDecimal = maxExportRows.multiply(new BigDecimal(10000));
            BigDecimal total = new BigDecimal(totalSize);
            if(total.compareTo(bigDecimal)==1) {
                // 通知前端
                String name = SC.v("ssd.admin", "contributor(contributor)");
                String resMsgStr = "【多维分析】导出数据量已超过" + maxExportRows + "万条，无法正常导出。若导出更多数据，请邮件申请，具体申请内容和流程见多维分析常见问题QA中序号3：https://docs.qq.com/doc/DT09SYWRib0xVTVVF";
                ResponseMessage resMsg = new ResponseMessage(false, resMsgStr);
                return resMsg;
            }

            String realFileNamePrefix = request.getRealPath("/").replaceAll("\\\\", "/") + "/download/" + fileName ;
            // 添加时间戳，避免服务器写入文件重复
            realFileNamePrefix = realFileNamePrefix + "_" + System.currentTimeMillis();

            ExportEngine exporter = QueryExportFactory.createQueryExportEngine(engine, totalSize);
            String resultFileName = realFileNamePrefix + ".csv";
            exporter.export(resultFileName, this.exportSql);

            response.setHeader("rptRows", String.valueOf(exporter.getTotalSize()));//导出条数

            String attachmentName = fileName + (resultFileName.contains(".zip") ? ".zip" : fileType.getExtName());
            response.setHeader("content-disposition", "attachment;filename=" + URLEncoder.encode(attachmentName, "utf-8"));

            // 文件输出
            is = new FileInputStream(new File(resultFileName));
            byte[] tempBytes = new byte[1024];
            int byteRead = 0;
            // 读入多个字节到字节数组中，byteread为一次读入的字节数
            while ((byteRead = is.read(tempBytes)) != -1) {
                out.write(tempBytes, 0, byteRead);
            }
            out.flush();
            success = true;


        }catch(Exception e) {
            e.printStackTrace();
            exceptionFlag = e.getMessage();
            success = false;

        } finally {
            try {
                response.setHeader("error_msg", URLEncoder.encode(exceptionFlag,"UTF-8"));//是否报错
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        long t2 = System.currentTimeMillis();
        System.out.println("导出总耗时(毫秒)：" + (t2 - t1)  );

        return  new ResponseMessage();
    }


    protected void init() {
        try {
            response.setContentType("application/x-msdownload;charset=UTF-8");
            if (out == null) {
                out = response.getOutputStream();
            }

            if(totalSize <= 0) {
                this.exportSql = engine.buildSql();
                String totalSizeSql = this.exportSql;
                /*
                if(totalSizeSql.contains(BIConsts.ORDER_BY)){
                    totalSizeSql = exportSql.substring(0, totalSizeSql.indexOf(BIConsts.ORDER_BY));
                }
                 */
                totalSize = engine.getDataSetTotalSize(totalSizeSql);
            }

        }catch(Exception e) {
            e.printStackTrace();
        }
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

    public void setEngine(QueryEngine engine) {
        this.engine = engine;
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
