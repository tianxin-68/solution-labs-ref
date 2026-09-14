package com.bi.queryer.ssm.mgr.exportImport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

/**
 * 导入导出处理
 * @author contributor
 */
@RestController
@Scope("prototype")
@RequestMapping("exportImport")
public class ExportImportContoller {

    @Autowired
    private ExportImportService exportImportService = null;

    @RequestMapping("downloadTemplate")
    public void downloadTemplate(String type, HttpServletResponse response){
        exportImportService.downloadTemplate(type,response);
    }
}
