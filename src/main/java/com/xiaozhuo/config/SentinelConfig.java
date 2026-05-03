package com.xiaozhuo.config;

import com.alibaba.csp.sentinel.init.InitFunc;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.circuitbreaker.CircuitBreakerStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel 初始化配置
 * 定义限流规则和熔断规则
 */
public class SentinelConfig implements InitFunc {

    @Override
    public void init() throws Exception {
        initFlowRules();
        initDegradeRules();

        System.out.println("✅ Sentinel rules initialized successfully");
    }

    /**
     * 初始化限流规则
     */
    private void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        // 1. 抢购接口限流：QPS 不超过 1000
        FlowRule couponGrabRule = new FlowRule();
        couponGrabRule.setResource("coupon:grab");
        couponGrabRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        couponGrabRule.setCount(1000);
        couponGrabRule.setLimitApp("default");
        rules.add(couponGrabRule);

        // 2. 登录接口限流：QPS 不超过 5（防暴力破解）
        FlowRule loginRule = new FlowRule();
        loginRule.setResource("user:login");
        loginRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        loginRule.setCount(5);
        loginRule.setLimitApp("default");
        rules.add(loginRule);

        // 3. 视频上传限流：QPS 不超过 10
        FlowRule videoUploadRule = new FlowRule();
        videoUploadRule.setResource("video:upload");
        videoUploadRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        videoUploadRule.setCount(10);
        videoUploadRule.setLimitApp("default");
        rules.add(videoUploadRule);

        // 4. Feed 流拉取限流：QPS 不超过 50
        FlowRule feedPullRule = new FlowRule();
        feedPullRule.setResource("feed:pull");
        feedPullRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        feedPullRule.setCount(50);
        feedPullRule.setLimitApp("default");
        rules.add(feedPullRule);

        // 5. 评论发表限流：QPS 不超过 20
        FlowRule commentAddRule = new FlowRule();
        commentAddRule.setResource("comment:add");
        commentAddRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        commentAddRule.setCount(20);
        commentAddRule.setLimitApp("default");
        rules.add(commentAddRule);

        FlowRuleManager.loadRules(rules);
        System.out.println("✅ Loaded " + rules.size() + " flow rules");
    }

    /**
     * 初始化熔断规则
     */
    private void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();

        // 1. 优惠券服务熔断：异常比例超过 50% 时熔断
        DegradeRule couponServiceRule = new DegradeRule();
        couponServiceRule.setResource("CouponService");
        couponServiceRule.setGrade(CircuitBreakerStrategy.ERROR_RATIO.getType());
        couponServiceRule.setCount(0.5);
        couponServiceRule.setTimeWindow(10);
        couponServiceRule.setMinRequestAmount(10);
        couponServiceRule.setStatIntervalMs(30000);
        rules.add(couponServiceRule);

        // 2. 视频服务熔断：响应时间超过 2秒 时熔断
        DegradeRule videoServiceRule = new DegradeRule();
        videoServiceRule.setResource("VideoService");
        videoServiceRule.setGrade(CircuitBreakerStrategy.SLOW_REQUEST_RATIO.getType());
        videoServiceRule.setCount(2000);
        videoServiceRule.setTimeWindow(10);
        videoServiceRule.setMinRequestAmount(10);
        videoServiceRule.setStatIntervalMs(30000);
        rules.add(videoServiceRule);

        // 3. Redis 操作熔断：异常比例超过 30% 时熔断
        DegradeRule redisRule = new DegradeRule();
        redisRule.setResource("RedisUtil");
        redisRule.setGrade(CircuitBreakerStrategy.ERROR_RATIO.getType());
        redisRule.setCount(0.3);
        redisRule.setTimeWindow(5);
        redisRule.setMinRequestAmount(5);
        redisRule.setStatIntervalMs(10000);
        rules.add(redisRule);

        DegradeRuleManager.loadRules(rules);
        System.out.println("✅ Loaded " + rules.size() + " degrade rules");
    }
}