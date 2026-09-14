package com.bi.queryer.ssm.governance.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量操作结果：总数 / 成功数 / 失败数 / 失败明细。
 */
@Data
public class BatchResult {

    private int total;
    private int success;
    private int failed;
    /** 失败明细：每项含 id 与失败原因 */
    private List<Fail> fails = new ArrayList<>();

    public void markSuccess() {
        this.success++;
    }

    public void markFail(Long id, String reason) {
        this.failed++;
        this.fails.add(new Fail(id, reason));
    }

    @Data
    public static class Fail {
        private final Long id;
        private final String reason;
    }
}
