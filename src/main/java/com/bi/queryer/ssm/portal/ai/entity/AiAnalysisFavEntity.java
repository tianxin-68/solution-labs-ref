package com.bi.queryer.ssm.portal.ai.entity;

/**
 * AI 会话收藏（ssm_ai_analysis_fav）
 */
/** genAI_feature/v3.15.0_start */
public class AiAnalysisFavEntity {

    private Long favId;

    private Long chatId;

    private Double favSortId;

    private String createdBy;

    private String createdTime;

    private String updatedBy;

    private String updatedTime;

    public Long getFavId() {
        return favId;
    }

    public void setFavId(Long favId) {
        this.favId = favId;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Double getFavSortId() {
        return favSortId;
    }

    public void setFavSortId(Double favSortId) {
        this.favSortId = favSortId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(String updatedTime) {
        this.updatedTime = updatedTime;
    }
}
/** genAI_feature/v3.15.0_end */
