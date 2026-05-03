package com.xiaozhuo.bean.dto;

import lombok.Data;
import java.util.List;

@Data
public class CouponActivityDTO {

    private Integer activityType;

    private String title;

    private String description;

    private String discountContent;

    private Integer totalStock;

    private String startTime;

    private String endTime;

    private Integer requiredWatchSeconds;

    private List<BatchConfigDTO> batches;

    private LotteryConfigDTO lotteryConfig;

    @Data
    public static class BatchConfigDTO {
        private Integer batchNumber;
        private Integer stockCount;
        private String releaseTime;
    }

    @Data
    public static class LotteryConfigDTO {
        private String lotteryTime;
        private Integer winnerCount;
    }
}
