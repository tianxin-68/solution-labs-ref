package com.bi.queryer.ssm.qa;

import com.bi.queryer.ssm.meta.SSDQuestionAndAnswer;
import com.bi.queryer.ssm.query.SSDQueryService;
import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.enums.Enabled;
import com.bi.queryer.sys.user.UserManager;
import com.bi.queryer.sys.user.vo.User;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * User: contributor
 * Date: 2020/2/4
 * Time: 12:28
 * Description:
 */
@Service
@Scope("prototype")
@Qualifier("SSDQuestAndAnswerService")
public class SSDQuestAndAnswerService {

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private BaseDao dao = null;

    @Autowired
    private SSDQueryService ssdQueryService;

    /**
     * 查询问题和回答表
     * @param sSDQuestionAndAnswer
     * @return
     */
    public ResponseMessage getQuestionAndAnswerList(SSDQuestionAndAnswer sSDQuestionAndAnswer) {
        ResponseMessage result = new ResponseMessage();
        if( sSDQuestionAndAnswer.getPageNum()!=null&&sSDQuestionAndAnswer.getPageSize()!=null){
            Integer pageSize = sSDQuestionAndAnswer.getPageSize();
            Integer pageNum = (sSDQuestionAndAnswer.getPageNum() - 1) * pageSize;
            sSDQuestionAndAnswer.setPageNum(pageNum);
            PageHelper.startPage(pageNum, pageSize);
            List<SSDQuestionAndAnswer> list = (List<SSDQuestionAndAnswer>) dao.queryObjectList("ssm.query.queryQuestionAndAnswer", sSDQuestionAndAnswer);
            PageInfo<SSDQuestionAndAnswer> pageInfo = new PageInfo<>(list);
            Integer total = dao.queryCount("ssm.query.queryQuestionAndAnswerCount", sSDQuestionAndAnswer);
            pageInfo.setTotal(total);
            result.setData(pageInfo);
            return result;
        }
        List<SSDQuestionAndAnswer> list = (List<SSDQuestionAndAnswer>) dao.queryObjectList("ssm.query.queryQuestionAndAnswer", sSDQuestionAndAnswer);
        result.setData(list);
        return result;
    }

    /**
     * 模板问题回答
     * @param sSDQuestionAndAnswer
     * @return  问题回答id
     */
    public ResponseMessage saveQuestionAndAnswer(SSDQuestionAndAnswer sSDQuestionAndAnswer) {
        ResponseMessage result = new ResponseMessage();
        try{
            User user = UserManager.get();
            if(user==null||StringUtils.isBlank(user.getName())){
                return new ResponseMessage(false,"为获取到用户信息！");
            }

            if(StringUtils.isBlank(sSDQuestionAndAnswer.getObjectTitle())){
                sSDQuestionAndAnswer.setObjectTitle("多维分析");
            }

           /* if(StringUtils.isNotBlank(sSDQuestionAndAnswer.getObjectId())&&StringUtils.isNotBlank(sSDQuestionAndAnswer.getObjectType())){
                if("field".equalsIgnoreCase(sSDQuestionAndAnswer.getObjectType())){
                    String objectTitle = (String)dao.queryObject("fieldDef.getFieldTitleByFieldId",sSDQuestionAndAnswer.getObjectId());
                    if(StringUtils.isNotBlank(objectTitle)){
                        sSDQuestionAndAnswer.setObjectTitle(objectTitle);
                    }else{
                        sSDQuestionAndAnswer.setObjectTitle("多维分析");
                    }
                }else if("category".equalsIgnoreCase(sSDQuestionAndAnswer.getObjectType())){
                    String objectTitle = (String)dao.queryObject("fieldCtg.getCtgNameByCtgId",sSDQuestionAndAnswer.getObjectId());
                    if(StringUtils.isNotBlank(objectTitle)){
                        sSDQuestionAndAnswer.setObjectTitle(objectTitle);
                    }else{
                        sSDQuestionAndAnswer.setObjectTitle("多维分析");
                    }
                }else if("all".equalsIgnoreCase(sSDQuestionAndAnswer.getObjectType())){
                    sSDQuestionAndAnswer.setObjectTitle("多维分析");
                    sSDQuestionAndAnswer.setObjectId("-1");

                }
            }else{
                sSDQuestionAndAnswer.setObjectId("-1");
                sSDQuestionAndAnswer.setObjectType("all");
                sSDQuestionAndAnswer.setObjectTitle("多维分析");
            }*/

            if(StringUtils.isNotBlank(sSDQuestionAndAnswer.getQuestionOwner())){
                sSDQuestionAndAnswer.setQuestionOwner(sSDQuestionAndAnswer.getQuestionOwner());
            }else{
                sSDQuestionAndAnswer.setQuestionOwner(user.getName());
            }

            if(StringUtils.isNotBlank(sSDQuestionAndAnswer.getQuestionContent())){
                sSDQuestionAndAnswer.setQuestionContent(sSDQuestionAndAnswer.getQuestionContent());
            }else{
                sSDQuestionAndAnswer.setQuestionContent("空缺");
            }

            sSDQuestionAndAnswer.setCreatedBy(user.getName());
            sSDQuestionAndAnswer.setQuestionTime(simpleDateFormat.format(new Date()));

            dao.insert("ssm.query.insertQuestionAndAnswer", sSDQuestionAndAnswer);
            if(sSDQuestionAndAnswer.getQuestionId()!=null){
                result.setData(sSDQuestionAndAnswer.getQuestionId());
            }

        }catch(Throwable e){
            e.printStackTrace();
            result.setMessage(e.getMessage());
        }
        return result;
    }

    /**
     * 更新问题回答表
     * @param sSDQuestionAndAnswer
     * @return
     */
    public ResponseMessage updateQuestionAndAnswer(SSDQuestionAndAnswer sSDQuestionAndAnswer)  {
        ResponseMessage result = new ResponseMessage();
        try{
            if(sSDQuestionAndAnswer.getQuestionId()==null){
                return new ResponseMessage(false,"为获取到id！");
            }

            User user = UserManager.get();
            if(user==null||StringUtils.isBlank(user.getName())){
                return new ResponseMessage(false,"为获取到用户信息！");
            }

            if(StringUtils.isNotBlank(sSDQuestionAndAnswer.getAnswerContent())){
                sSDQuestionAndAnswer.setIsAnswer(1);
                sSDQuestionAndAnswer.setAnswerTime(simpleDateFormat.format(new Date()));
                if(StringUtils.isNotBlank(sSDQuestionAndAnswer.getAnswerOwner())){
                    sSDQuestionAndAnswer.setAnswerOwner(sSDQuestionAndAnswer.getAnswerOwner());
                }else{
                    sSDQuestionAndAnswer.setAnswerOwner(user.getName());
                }
            }

            sSDQuestionAndAnswer.setUpdatedBy(user.getName());

            dao.update("ssm.query.updateQuestionAndAnswer", sSDQuestionAndAnswer);

            SSDQuestionAndAnswer queryEntity = new SSDQuestionAndAnswer();
            queryEntity.setQuestionId(sSDQuestionAndAnswer.getQuestionId());
            List<SSDQuestionAndAnswer> list = (List<SSDQuestionAndAnswer>) dao.queryObjectList("ssm.query.queryQuestionAndAnswer", sSDQuestionAndAnswer);
            if(!list.isEmpty()&&Enabled.value(list.get(0).getIsAnswer())&&Enabled.value(list.get(0).getIsActive())){
                String message = "您在多维分析QA中提的问题，已经被" + user.getName() + "回答，请查看！" ;
                List<String> sendUserList = new ArrayList<>();
                sendUserList.add(list.get(0).getQuestionOwner());

                if(!sendUserList.isEmpty()){
                    ssdQueryService.sendMsg(message,sendUserList);
                }
            }

        }catch(Throwable e){
            e.printStackTrace();
            result.setMessage(e.getMessage());
        }
        return result;

    }

    /**
     * 删除问题回答表
     * @param questionId
     * @return
     */
    public ResponseMessage deleteQuestionAndAnswer(Long questionId)  {
        ResponseMessage result = new ResponseMessage();
        try{
            if(questionId==null){
                return new ResponseMessage(false,"为获取到id！");
            }

            dao.update("ssm.query.deleteQuestionAndAnswer", questionId);

        }catch(Throwable e){
            e.printStackTrace();
            result.setMessage(e.getMessage());
        }
        return result;

    }

    /**
     * 更新问题回答排序
     * @param sSDQuestionAndAnswer
     * @return
     */
    public ResponseMessage updateQuestionAndAnswerSort(SSDQuestionAndAnswer sSDQuestionAndAnswer)  {
        ResponseMessage result = new ResponseMessage();
        try{
            if(sSDQuestionAndAnswer.getQuestionId()==null){
                return new ResponseMessage(false,"为获取到id！");
            }
            User user = UserManager.get();
            if(user==null||StringUtils.isBlank(user.getName())){
                return new ResponseMessage(false,"为获取到用户信息！");
            }
            sSDQuestionAndAnswer.setUpdatedBy(user.getName());
            if(sSDQuestionAndAnswer.getSortId()==null){
                return new ResponseMessage(false,"请传入排序值");
            }
            dao.update("ssm.query.updateQuestionAndAnswerSort", sSDQuestionAndAnswer);
        }catch(Throwable e){
            e.printStackTrace();
            result.setMessage(e.getMessage());
        }
        return result;
    }

}
