package com.xiaozhuo.service.Impl;

import com.xiaozhuo.annotation.Bean;
import com.xiaozhuo.annotation.Transactional;
import com.xiaozhuo.dao.CouponDao;
import com.xiaozhuo.dao.impl.CouponDaoImpl;
import com.xiaozhuo.entity.CouponActivity;
import com.xiaozhuo.entity.CouponUser;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.CouponService;
import com.xiaozhuo.util.ConnectionPool;
import com.xiaozhuo.util.RedisUtil;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Bean
public class CouponServiceImpl implements CouponService {

    private CouponDao couponDao = new CouponDaoImpl();

    private static final String COUPON_STOCK_PREFIX = "coupon:stock:";
    private static final String COUPON_USER_PREFIX = "coupon:user:";

    @Override
    @Transactional
    public Result<Map<String, Object>> grabCoupon(Long userId, Long activityId) {
        Connection conn = null;
        try {
            if (userId == null || activityId == null) {
                return Result.fail(400, "参数无效");
            }

            String stockKey = COUPON_STOCK_PREFIX + activityId;
            String userKey = COUPON_USER_PREFIX + userId + ":" + activityId;

            if (RedisUtil.exists(userKey)) {
                return Result.fail(400, "您已领取过该优惠券");
            }

            Long stock = RedisUtil.decr(stockKey);

            if (stock < 0) {
                RedisUtil.incr(stockKey);
                return Result.fail(400, "库存不足");
            }

            conn = ConnectionPool.getConnection();

            CouponActivity activity = couponDao.selectById(conn, activityId);
            if (activity == null) {
                RedisUtil.incr(stockKey);
                return Result.fail(404, "活动不存在");
            }

            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(activity.getStartTime())) {
                RedisUtil.incr(stockKey);
                return Result.fail(400, "活动未开始");
            }
            if (now.isAfter(activity.getEndTime())) {
                RedisUtil.incr(stockKey);
                return Result.fail(400, "活动已结束");
            }

            CouponUser existing = couponDao.selectByUserIdAndActivityId(conn, userId, activityId);
            if (existing != null) {
                RedisUtil.incr(stockKey);
                return Result.fail(400, "您已领取过该优惠券");
            }

            int rows = couponDao.decrementStockWithOptimisticLock(conn, activityId, activity.getVersion());
            if (rows == 0) {
                RedisUtil.incr(stockKey);
                return Result.fail(400, "库存不足或并发冲突，请重试");
            }

            String couponCode = generateCouponCode();
            CouponUser couponUser = new CouponUser();
            couponUser.setActivityId(activityId);
            couponUser.setUserId(userId);
            couponUser.setCouponCode(couponCode);
            couponUser.setStatus(0);
            couponUser.setExpireTime(LocalDateTime.now().plusDays(30));
            couponUser.setCreateTime(LocalDateTime.now());

            couponDao.insertCouponUser(conn, couponUser);

            RedisUtil.setex(userKey, 86400 * 30, "1");

            Map<String, Object> data = new HashMap<>();
            data.put("couponCode", couponCode);
            data.put("expireTime", couponUser.getExpireTime());

            return Result.success("抢购成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public Result<List<CouponUser>> getMyCoupons(Long userId, int pageNum, int pageSize) {
        Connection conn = null;
        try {
            if (userId == null) {
                return Result.fail(400, "用户ID不能为空");
            }

            if (pageNum < 1) pageNum = 1;
            if (pageSize < 1 || pageSize > 100) pageSize = 10;

            conn = ConnectionPool.getConnection();

            List<CouponUser> coupons = couponDao.selectByUserId(conn, userId, pageNum, pageSize);
            long total = couponDao.countByUserId(conn, userId);

            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("coupons", coupons);
            pageInfo.put("total", total);
            pageInfo.put("pageNum", pageNum);
            pageInfo.put("pageSize", pageSize);
            pageInfo.put("totalPages", (total + pageSize - 1) / pageSize);

            return Result.success("查询成功", coupons);

        } catch (Exception e) {
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public Result<Map<String, Object>> getActivityDetail(Long activityId) {
        Connection conn = null;
        try {
            if (activityId == null) {
                return Result.fail(400, "活动ID不能为空");
            }

            conn = ConnectionPool.getConnection();

            CouponActivity activity = couponDao.selectById(conn, activityId);
            if (activity == null) {
                return Result.fail(404, "活动不存在");
            }

            String stockKey = COUPON_STOCK_PREFIX + activityId;
            String cachedStock = RedisUtil.get(stockKey);

            Map<String, Object> data = new HashMap<>();
            data.put("id", activity.getId());
            data.put("title", activity.getTitle());
            data.put("description", activity.getDescription());
            data.put("totalStock", activity.getTotalStock());

            if (cachedStock != null) {
                data.put("remainingStock", Long.parseLong(cachedStock));
            } else {
                data.put("remainingStock", activity.getRemainingStock());
            }

            data.put("startTime", activity.getStartTime());
            data.put("endTime", activity.getEndTime());
            data.put("status", activity.getStatus());

            return Result.success("查询成功", data);

        } catch (Exception e) {
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    private String generateCouponCode() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "CPN" + timestamp + random;
    }

    @Override
    public Result<Boolean> initStockToRedis(Long activityId) {
        Connection conn = null;
        try {
            if (activityId == null) {
                return Result.fail(400, "活动ID不能为空");
            }

            conn = ConnectionPool.getConnection();

            CouponActivity activity = couponDao.selectById(conn, activityId);
            if (activity == null) {
                return Result.fail(404, "活动不存在");
            }

            String stockKey = COUPON_STOCK_PREFIX + activityId;

            RedisUtil.set(stockKey, String.valueOf(activity.getRemainingStock()));

            System.out.println("✅ 库存已同步到 Redis - 活动ID: " + activityId
                    + ", 库存: " + activity.getRemainingStock());

            return Result.success("库存初始化成功", true);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }
}