package com.bi.queryer.sys.user.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 多维权限申请提交结果
 */
public class SsmAuthApplyResultVo {

    /** 是否已直接生效（如仅删除行级权限，未创建工单） */
    private boolean directApply;

    private List<String> taskIds = new ArrayList<>();

    public boolean isDirectApply() {
        return directApply;
    }

    public void setDirectApply(boolean directApply) {
        this.directApply = directApply;
    }

    public List<String> getTaskIds() {
        return taskIds;
    }

    public void setTaskIds(List<String> taskIds) {
        this.taskIds = taskIds;
    }
}
