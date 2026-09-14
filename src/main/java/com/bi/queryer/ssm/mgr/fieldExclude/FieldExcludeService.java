package com.bi.queryer.ssm.mgr.fieldExclude;

import com.bi.queryer.ssm.meta.MetaFieldCategory;
import com.bi.queryer.ssm.meta.MetaFieldExclude;
import com.bi.queryer.util.excelUtil.ExcelHelper;
import com.bi.queryer.sys.base.AbstractTransaction;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.db.DataSourceType;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.StringUtil;
import com.bi.queryer.util.component.datagrid.DataGrid;
import com.bi.queryer.util.component.datagrid.DataGridColumn;
import com.bi.queryer.util.component.datagrid.IDataGridDataSetProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author contributor
 */
@Service
public class FieldExcludeService implements IDataGridDataSetProvider {

    @Autowired
    private BaseDao dao;

    /**
     * 构建datagrid
     */
    public DataGrid buildDataGrid(Map<String, String> paramMap) {
        DataGrid grid = new DataGrid(-1, -1);
        String width = paramMap.get("width");
        String height = paramMap.get("height");
        if(!StringUtil.isEmpty(width) && !StringUtil.isEmpty(height)){
            grid.setWidth(Integer.valueOf(width));
            grid.setHeight(Integer.valueOf(height));
        }
        grid.setAutoSize(true);
        grid.setShowExport(false);
        grid.setFormatLink(false);
        grid.setPagination(true);// 使用分页
        grid.setShowHideColumnButton(false);


        DataGridColumn column = new DataGridColumn("code", "字段编码", -1); // 添加列
        grid.addColumn(column);

        column = new DataGridColumn("name", "字段名称", -1, "left"); // 添加列
        grid.addColumn(column);

        column = new DataGridColumn("excludeCode", "互斥字段编码", -1, "left"); // 添加列
        grid.addColumn(column);

        column = new DataGridColumn("excludeName", "互斥字段名称", -1, "left"); // 添加列
        grid.addColumn(column);

        column = new DataGridColumn("categoryId", "目录id", -1,"left");
        column.setHidden(true);
        grid.addColumn(column);

        column = new DataGridColumn("categoryName", "目录名称", -1,"left");
        grid.addColumn(column);

        column = new DataGridColumn("excludeCategoryId", "互斥目录id", -1,"left");
        column.setHidden(true);
        grid.addColumn(column);

        column = new DataGridColumn("excludeCategoryName", "互斥目录名称", -1,"left");
        grid.addColumn(column);

        column = new DataGridColumn("excludeDesc", "互斥描述", -1); // 添加列
        grid.addColumn(column);

        column = new DataGridColumn("operator", "操作");
        column.setOperator(true);
        column.addOperation("删除","deleteRow");

        column.setFrozen(true);
        column.setCanHide(false);
        column.setWidth(160);
        grid.addColumn(column);


        grid.setDataSetProvider(this, paramMap);
        return grid;
    }

    @Override
    public List<MetaFieldExclude> getDataSet(Map queryParamMap) {
        buidQueryParam(queryParamMap);
        return dao.queryMapList("fieldExclude.queryFieldExcludeList", queryParamMap);
    }

    protected void buidQueryParam(Map paramMap) {
    }

    @Override
    public Integer getDataSetTotalSize(Map queryParamMap) {
        buidQueryParam(queryParamMap);
        return  dao.queryCount("fieldExclude.queryFieldExcludeListCount", queryParamMap);
    }

    public ResponseMessage saveFieldExclude(MetaFieldExclude metaFieldExclude){
        ResponseMessage result = new ResponseMessage();

        try {

            User user = UserManager.get();

            //1 先删除
            dao.delete("fieldExclude.deleteFieldExclude",metaFieldExclude);

            //2 插入
            if(BIUtil.isNotEmpty(metaFieldExclude.getFieldExcludeList())){

                for (MetaFieldExclude mfe:metaFieldExclude.getFieldExcludeList() ) {

                    if("fieldExclude".equalsIgnoreCase(metaFieldExclude.getType())||"fieldCtgExclude".equalsIgnoreCase(metaFieldExclude.getType())){

                        mfe.setCode(metaFieldExclude.getCode());
                        mfe.setName(metaFieldExclude.getName());

                    }else{

                        mfe.setCategoryId(metaFieldExclude.getCategoryId());
                        mfe.setCategoryName(metaFieldExclude.getCategoryName());
                    }

                    mfe.setExcludeDesc(metaFieldExclude.getExcludeDesc());
                    mfe.setIsActive(Enabled.YES.getId());
                    mfe.setCreatedBy(user.getName());
                }

                dao.insert("fieldExclude.insertIntoFieldExclude",metaFieldExclude);
            }

            //判断互斥是否是目录和目录,不是就按原来逻辑返回
            if(BIUtil.isEmpty(metaFieldExclude.getCategoryId())){
                return result;
            }
            //先递归查出目录下面所有的目录，然后每个都保存一遍
            List<MetaFieldCategory> list = new ArrayList<>();
            queryExcludeChildrenCtg(list,metaFieldExclude.getCategoryId(),1);
            for(MetaFieldCategory entity : list){

                if(BIUtil.isNotEmpty(metaFieldExclude.getFieldExcludeList())){

                    for (MetaFieldExclude mfe:metaFieldExclude.getFieldExcludeList() ) {
                        mfe.setCategoryId(entity.getId());
                        //1 先删除
                        dao.delete("fieldExclude.deleteFieldExcludeCtg",mfe);
                       /* //1、如果存在，就跳过
                        Integer count = dao.queryCount("fieldExclude.queryCtgToCtgExcludeExist",mfe);
                        if(count>0){
                            continue;
                        }*/

                        mfe.setCategoryName(entity.getName());
                        mfe.setExcludeDesc(metaFieldExclude.getExcludeDesc());
                        mfe.setIsActive(Enabled.YES.getId());
                        mfe.setCreatedBy(user.getName());
                    }

                    dao.insert("fieldExclude.insertIntoFieldExclude",metaFieldExclude);
                }
            }

        }catch (Exception e){
            return new ResponseMessage(false,e.getMessage());
        }

        return result;
    }


    //查询下游依赖递归递归，建立子树形结构
    private void queryExcludeChildrenCtg(List<MetaFieldCategory> ChildList,String parentCtgId,Integer level){
        List<MetaFieldCategory> list = (List<MetaFieldCategory>)dao.queryObjectList("fieldCtg.queryChildFieldCtgByParentId",parentCtgId);
        if(!list.isEmpty()){
            ChildList.addAll(list);
            for (MetaFieldCategory entry : list) {
               if(BIUtil.isNotEmpty(entry.getId())){
                   System.out.println(entry.getName() + "--" + level);
                   queryExcludeChildrenCtg(ChildList,entry.getId(),level+1);
               }
            }
        }

    }

    public ResponseMessage deleteFieldExclude(MetaFieldExclude metaFieldExclude){

        ResponseMessage result = new ResponseMessage();

        try {

            dao.delete("fieldExclude.deleteFieldExcludeSingle",metaFieldExclude);
        }catch (Exception e){
            return new ResponseMessage(false,e.getMessage());
        }

        return result;
    }

    public ResponseMessage getFieldExcludeIds(Map<String,String> map){
        ResponseMessage result = new ResponseMessage();

        List<String> ids = (List<String>)dao.queryObjectList("fieldExclude.getFieldExcludeIds",map);
        String desc = (String)dao.queryObject("fieldExclude.getFieldExcludeDesc",map);

        Map<String,Object> mapResult = new HashMap<>();
        mapResult.put("ids",ids);
        mapResult.put("desc",desc);

        result.setData(mapResult);

        return result;

    }

    public void exportMethod(String searchTxt, String type, HttpServletResponse response){

        Map<String,Object> map = new HashMap<>();
        map.put("searchTxt",searchTxt);
        map.put("pageRowLower",0);
        map.put("prePageSize",Integer.MAX_VALUE);

        List<MetaFieldExclude> list = (List<MetaFieldExclude>)dao.queryObjectList("fieldExclude.queryFieldExcludeList",map);

        String[] cloumnNames = new String[] { "字段code", "字段名称", "互斥字段code", "互斥字段名称", "目录id", "互斥字段分类id","互斥描述","是否有效" };

        List<String[]> cloumnValues = new ArrayList<>();

        if (list != null && list.size() > 0) {

            for (MetaFieldExclude metaFieldExclude : list) {
                String[] strs = new String[]{
                        metaFieldExclude.getCode(),
                        metaFieldExclude.getName(),
                        metaFieldExclude.getExcludeCode(),
                        metaFieldExclude.getExcludeName(),
                        metaFieldExclude.getCategoryId(),
                        metaFieldExclude.getExcludeCategoryId(),
                        metaFieldExclude.getExcludeDesc(),
                        metaFieldExclude.getIsActive() == 0 ? "否" : "是"
                };
                cloumnValues.add(strs);
            }
        }

        ExcelHelper.exportExcel(cloumnNames,cloumnValues,type,response);

    }

    public ResponseMessage importMethod(MultipartFile files){

        ResponseMessage result = new ResponseMessage();

        // 获取前端传过来的file
        InputStream inputStream = null;
        StringBuilder msg = new StringBuilder();

        try{

            if(files!=null) {

                String fileName = new String(files.getOriginalFilename().getBytes("ISO-8859-1"), "UTF-8");
                inputStream = files.getInputStream();

                List<MetaFieldExclude> list = ExcelHelper.convertToList(MetaFieldExclude.class, fileName, inputStream, 1, 8, 0);

                if (list.size() == 0) {
                    return new ResponseMessage(false,"文件内容为空");
                }

                for (int i = 0; i < list.size(); i++) {
                    MetaFieldExclude metaFieldExclude = list.get(i);
                    if(BIUtil.isEmpty(metaFieldExclude.getCode())&&BIUtil.isEmpty(metaFieldExclude.getCategoryId()) ){
                        msg.append("第" + (i + 1) + "行字段code与目录id不能同时为空,");
                    }
                    if(BIUtil.isEmpty(metaFieldExclude.getExcludeCode())&&BIUtil.isEmpty(metaFieldExclude.getExcludeCategoryId())){
                        msg.append("第" + (i + 1) + "行互斥字段code与互斥字段分类id不能同时为空,");
                    }

                    if(BIUtil.isNotEmpty(metaFieldExclude.getCode())&&BIUtil.isNotEmpty(metaFieldExclude.getCategoryId()) ){
                        msg.append("第" + (i + 1) + "行字段code与目录id不能同时有值,");
                    }
                    if(BIUtil.isNotEmpty(metaFieldExclude.getExcludeCode())&&BIUtil.isNotEmpty(metaFieldExclude.getExcludeCategoryId())){
                        msg.append("第" + (i + 1) + "行互斥字段code与互斥字段分类id不能同时有值,");
                    }
                }

                if (BIUtil.isNotEmpty(msg.toString())) {
                    result.setSuccess(false);
                    result.setMessage(msg.toString());
                    return result;
                } else {

                    User user = UserManager.get();

                    dao.executeTranscation(DataSourceType.Default, new AbstractTransaction() {
                        @Override
                        public void execute() {
                            for (MetaFieldExclude metaFieldExclude : list) {
                                //插入数据库
                                metaFieldExclude.setCreatedBy(user.getName());
                                dao.insert("fieldExclude.insertIntoFieldExcludeSingle", metaFieldExclude);

                            }
                        }
                    });


                }
            }

        }catch (Exception e){
            return new ResponseMessage(false,e.getMessage());
        }

        return result;
    }
}
