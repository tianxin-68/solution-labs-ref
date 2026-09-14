package com.bi.queryer.ssm.mgr.dataset;

import com.bi.queryer.ssm.mgr.dataset.model.DatasetAddReq;
import com.bi.queryer.ssm.mgr.dataset.model.DatasetRsq;
import com.bi.queryer.ssm.mgr.dataset.model.DatasetUpdateReq;
import com.bi.queryer.ssm.mgr.dataset.model.SSMDatasetAuthDim;
import com.bi.queryer.sys.common.SSMResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Author: contributor
 * @CreateTime: 2024-04-15  13:45
 * @Description: 数据集管理-前端控制器
 */
@RestController
@Scope("prototype")
@RequestMapping("/ssm/dataset")
public class DatasetController {

    @Autowired
    private DatasetService datasetService;

    /**
     * 数据集新增
     *
     * @param datasetAddReq
     * @return
     */
    @RequestMapping("add")
    @ResponseBody
    public SSMResponseMessage<String> add(@RequestBody DatasetAddReq datasetAddReq) {
        return datasetService.add(datasetAddReq);
    }

    /**
     * 数据集修改
     *
     * @param datasetUpdateReq
     * @return
     */
    @RequestMapping("update")
    @ResponseBody
    public SSMResponseMessage<String> update(@RequestBody DatasetUpdateReq datasetUpdateReq) {
        return datasetService.update(datasetUpdateReq);
    }

    /**
     * 数据集获取
     *
     * @param datasetId
     * @return
     */
    @RequestMapping("get")
    @ResponseBody
    public SSMResponseMessage<DatasetRsq> get(String datasetId) {
        return datasetService.get(datasetId);
    }

    /**
     * 数据集删除
     *
     * @param datasetId
     * @return
     */
    @RequestMapping("deleteById")
    @ResponseBody
    public SSMResponseMessage deleteById(String datasetId) {
        return datasetService.deleteById(datasetId);
    }

    /**
     * 获取可切换数据集列表，状态可用，并且查询权限
     * @return
     */
    @RequestMapping("list")
    @ResponseBody
    public SSMResponseMessage<List<DatasetRsq>> list(){
        return datasetService.list();
    }

    /**
     * 工单权限申请场景的数据集列表，排除配置项 ssm.workorder.auth.apply.exclude.dataset.id.list 中的 datasetId。
     */
    @RequestMapping("listForAuthApply")
    @ResponseBody
    public SSMResponseMessage<List<DatasetRsq>> listForAuthApply() {
        return datasetService.listForAuthApply();
    }

    /**
     * 获取所有数据集列表
     * @return
     */
    @RequestMapping("listAll")
    @ResponseBody
    public SSMResponseMessage<List<DatasetRsq>> listAll() {
        List<DatasetRsq> result = datasetService.listAll();
        return SSMResponseMessage.success("", result);
    }

    /**
     * 通过模板id获取数据集
     * @param tplId
     * @return
     */
    @RequestMapping("getDatasetByTplId")
    @ResponseBody
    public SSMResponseMessage<DatasetRsq> getDatasetByTplId(String tplId) {
        return datasetService.getDatasetByTplId(tplId);
    }

    /**
     * 同步数据集与行级权限维度的关系
     */
    @RequestMapping("syncDatasetAuthDim")
    @ResponseBody
    public SSMResponseMessage syncDatasetAuthDim() {
        return datasetService.syncDatasetAuthDim();
    }

    /**
     * 根据数据集id获取行级权限维度编码
     *
     * @param datasetId 数据集id
     * @return 行级权限维度列表
     */
    @RequestMapping("getDatasetAuthDim")
    @ResponseBody
    public SSMResponseMessage<List<SSMDatasetAuthDim>> getDatasetAuthDim(String datasetId) {
        return datasetService.getDatasetAuthDim(datasetId);
    }
}
