package com.bi.queryer.ssm.enums;

public enum SseEventType {

    STREAM_OUTPUT_START("stream_output_start", "流式输出：开始"),

    STREAM_OUTPUTTING("stream_outputting", "流式输出：进行中"),

    STREAM_OUTPUT_END("stream_output_end", "流式输出：结束"),

    DEBUG("debug", "调试"),

    MESSAGE_END("message_end", "消息结束"),

    CODE_OUTPUT_START("code_output_start", "代码输出：开始"),

    CODE_OUTPUTTING("code_outputting", "代码输出：进行中"),

    CODE_OUTPUT_END("code_output_end", "代码输出：结束"),

    /** OLAP 对话：单次 execute 结束后的聚合结果（token、全文 answer、可选上下文摘要） */
    EXECUTE_RESULT("execute_result", "OLAP 执行结果聚合");

    private String code;
    private String desc;

    SseEventType(String code, String desc){
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public static SseEventType get(String str){
        for(SseEventType t : SseEventType.values()){
            if(t.toString().equalsIgnoreCase(str) || t.getCode().equalsIgnoreCase(str)){
                return t;
            }
        }
        return null;
    }
}
