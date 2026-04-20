package com.xiaozhuo.util;

import com.alibaba.fastjson.JSON;

public class JsonUtil {
    // 对象转 JSON 字符串
    public static String toJson(Object obj) {
        if (obj == null) {
            return "{}";
        }
        return JSON.toJSONString(obj);
    }

    // JSON 字符串转对象
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        return JSON.parseObject(json, clazz);
    }
}