package com.bi.queryer.ssm.qa;

import com.bi.queryer.ssm.meta.SSDQuestionAndAnswer;
import com.bi.queryer.sys.base.BaseController;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.util.BIUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * User: contributor
 * Date: 2022/9/26
 * Time: 12:16
 * Description:
 * 问题与回答
 */
@Controller
@Scope("prototype")
@RequestMapping("ssd/question")
public class SSDQuestAndAnswerController extends BaseController{

    @Autowired
    protected SSDQuestAndAnswerService service = null;

    /**
     * 查询问题回答
     * @return
     */
    @RequestMapping("getQuestionAndAnswerList")
    @ResponseBody
    public ResponseMessage getQuestionAndAnswerList(){
        SSDQuestionAndAnswer sSDQuestionAndAnswer = this.createQuestionAndAnswerRequest();
        return service.getQuestionAndAnswerList(sSDQuestionAndAnswer);
    }

    /**
     * 保存问题回答
     * @return
     */
    @RequestMapping("saveQuestionAndAnswer")
    @ResponseBody
    public ResponseMessage saveQuestionAndAnswer(){
        SSDQuestionAndAnswer sSDQuestionAndAnswer = this.createQuestionAndAnswerRequest();
        return service.saveQuestionAndAnswer(sSDQuestionAndAnswer);
    }

    /**
     * 更新问题回答
     * @return
     */
    @RequestMapping("updateQuestionAndAnswer")
    @ResponseBody
    public ResponseMessage updateQuestionAndAnswer(){
        SSDQuestionAndAnswer sSDQuestionAndAnswer = this.createQuestionAndAnswerRequest();
        return service.updateQuestionAndAnswer(sSDQuestionAndAnswer);
    }

    /**
     * 删除问题回答
     * @return
     */
    @RequestMapping("deleteQuestionAndAnswer")
    @ResponseBody
    public ResponseMessage deleteQuestionAndAnswer(){
        Long questionId = longValue("questionId");
        return service.deleteQuestionAndAnswer(questionId);
    }

    /**
     * 更新问题回答排序
     * @return
     */
    @RequestMapping("updateQuestionAndAnswerSort")
    @ResponseBody
    public ResponseMessage updateQuestionAndAnswerSort(){
        SSDQuestionAndAnswer sSDQuestionAndAnswer = this.createQuestionAndAnswerRequest();
        return service.updateQuestionAndAnswerSort(sSDQuestionAndAnswer);
    }

    /**
     * 从请求中创建问题回答类
     * @return
     */
    protected SSDQuestionAndAnswer createQuestionAndAnswerRequest(){
        SSDQuestionAndAnswer entity = new SSDQuestionAndAnswer();
        Integer pageSize = this.intValue("pageSize") ;
        if(pageSize!=null) {
            entity.setPageSize(pageSize);
        }
        Integer pageNum = this.intValue("pageNum") ;
        if(pageNum!=null) {
            entity.setPageNum(pageNum);
        }
        Long questionId = this.longValue("questionId");
        if(questionId!=null) {
            entity.setQuestionId(questionId);
        }
        String answerOwner = this.stringValue("answerOwner") ;
        if(BIUtil.isNotEmpty(answerOwner)) {
            entity.setAnswerOwner(answerOwner);
        }
        String questionOwner = this.stringValue("questionOwner") ;
        if(BIUtil.isNotEmpty(questionOwner)) {
            entity.setQuestionOwner(questionOwner);
        }
        String objectType = this.stringValue("objectType") ;
        if(BIUtil.isNotEmpty(objectType)) {
            entity.setObjectType(objectType);
        }
        String objectId = this.stringValue("objectId") ;
        if(BIUtil.isNotEmpty(objectId)) {
            entity.setObjectId(objectId);
        }
        String objectTitle = this.stringValue("objectTitle") ;
        if(BIUtil.isNotEmpty(objectTitle)) {
            entity.setObjectTitle(objectTitle);
        }
        String questionTimeBegin = this.stringValue("questionTimeBegin") ;
        if(BIUtil.isNotEmpty(questionTimeBegin)) {
            entity.setQuestionTimeBegin(questionTimeBegin);
        }
        String questionTimeEnd = this.stringValue("questionTimeEnd") ;
        if(BIUtil.isNotEmpty(questionTimeEnd)) {
            entity.setQuestionTimeEnd(questionTimeEnd);
        }
        String questionContent = this.stringValue("questionContent") ;
        if(BIUtil.isNotEmpty(questionContent)) {
            entity.setQuestionContent(questionContent);
        }
        String questionTime = this.stringValue("questionTime") ;
        if(BIUtil.isNotEmpty(questionTime)) {
            entity.setQuestionTime(questionTime);
        }
        String answerContent = this.stringValue("answerContent") ;
        if(BIUtil.isNotEmpty(answerContent)) {
            entity.setAnswerContent(answerContent);
        }
        Integer isActive = this.intValue("isActive") ;
        if(isActive!=null) {
            entity.setIsActive(isActive);
        }
        Integer isAnswer = this.intValue("isAnswer") ;
        if(isAnswer!=null) {
            entity.setIsAnswer(isAnswer);
        }
        Double sortId = this.doubleValue("sortId") ;
        if(sortId!=null) {
            entity.setSortId(sortId);
        }
        String orderBy = this.stringValue("orderBy") ;
        if(BIUtil.isNotEmpty(orderBy)) {
            entity.setOrderBy(orderBy);
        }

        return entity;
    }
}
