package com.xiaozhuo.test;

import com.xiaozhuo.entity.CouponUser;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.Impl.CouponServiceImpl;
import com.xiaozhuo.service.CouponService;

import java.util.List;
import java.util.Map;

/**
 * 优惠券抢购功能完整测试
 */
public class CouponGrabTest {

    public static void main(String[] args) {
        CouponService couponService = new CouponServiceImpl();

        System.out.println("========== 优惠券抢购功能测试 ==========\n");

        Long userId = 6L;
        Long activityId = 1L;

        // 1. 查询活动详情
        System.out.println("【测试1】查询活动详情");
        Result<Map<String, Object>> detailResult = couponService.getActivityDetail(activityId);
        if (detailResult.getCode() == 200) {
            Map<String, Object> data = detailResult.getData();
            System.out.println("✅ 活动标题: " + data.get("title"));
            System.out.println("   总库存: " + data.get("totalStock"));
            System.out.println("   剩余库存: " + data.get("remainingStock"));
            System.out.println("   开始时间: " + data.get("startTime"));
            System.out.println("   结束时间: " + data.get("endTime"));
        } else {
            System.out.println("❌ 查询失败: " + detailResult.getMsg());
            return;
        }

        // 2. 第一次抢购
        System.out.println("\n【测试2】用户首次抢购");
        Result<Map<String, Object>> grabResult = couponService.grabCoupon(userId, activityId);
        String couponCode = null;

        if (grabResult.getCode() == 200) {
            Map<String, Object> data = grabResult.getData();
            couponCode = (String) data.get("couponCode");
            System.out.println("✅ 抢购成功!");
            System.out.println("   优惠券码: " + couponCode);
            System.out.println("   过期时间: " + data.get("expireTime"));
        } else {
            System.out.println("❌ 抢购失败: " + grabResult.getMsg());
        }

        // 3. 重复抢购（应该失败）
        System.out.println("\n【测试3】用户重复抢购（应失败）");
        grabResult = couponService.grabCoupon(userId, activityId);
        if (grabResult.getCode() == 400) {
            System.out.println("✅ 正确拦截: " + grabResult.getMsg());
        } else {
            System.out.println("❌ 未正确拦截，状态码: " + grabResult.getCode());
        }

        // 4. 查询我的优惠券
        System.out.println("\n【测试4】查询我的优惠券列表");
        Result<List<CouponUser>> myCouponsResult = couponService.getMyCoupons(userId, 1, 10);
        if (myCouponsResult.getCode() == 200) {
            List<CouponUser> coupons = myCouponsResult.getData();
            System.out.println("✅ 查询成功，共 " + coupons.size() + " 张优惠券");

            for (CouponUser coupon : coupons) {
                System.out.println("   - 券码: " + coupon.getCouponCode());
                System.out.println("     状态: " + (coupon.getStatus() == 0 ? "未使用" : "已使用"));
                System.out.println("     过期时间: " + coupon.getExpireTime());
            }
        } else {
            System.out.println("❌ 查询失败: " + myCouponsResult.getMsg());
        }

        // 5. 再次查询活动详情（验证库存减少）
        System.out.println("\n【测试5】再次查询活动详情（验证库存）");
        detailResult = couponService.getActivityDetail(activityId);
        if (detailResult.getCode() == 200) {
            Map<String, Object> data = detailResult.getData();
            System.out.println("✅ 剩余库存: " + data.get("remainingStock"));
            System.out.println("   提示: 应该比初始库存少 1");
        }

        System.out.println("\n========== 测试完成 ==========");
    }
}