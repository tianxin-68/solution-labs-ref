package com.bi.queryer.ssm.mgr.exportImport;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.OutputStream;
import java.net.URLEncoder;

/**
 * @author contributor
 */
@Service
public class ExportImportService {

    public void downloadTemplate(String type, HttpServletResponse response) {

        MgrType mgrType = MgrType.get(type);
        String fileName = mgrType.getName();

        String path = Thread.currentThread().getContextClassLoader().getResource("").getPath();

        path = path +"template/"+fileName;

        File file = new File(path);
        if (file.exists()) {

            try {
                response.setHeader("content-disposition", "Attachment;filename=" + URLEncoder.encode(fileName, "utf-8"));
                OutputStream out = response.getOutputStream();
                out.write(FileUtils.readFileToByteArray(file));
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
