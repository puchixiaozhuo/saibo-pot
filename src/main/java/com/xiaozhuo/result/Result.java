package com.xiaozhuo.result;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局统一接口返回格式
 * 实现 Serializable 支持序列化（如缓存/网络传输）
 */
public class Result<T> implements Serializable {
    // 序列化版本号
    private static final long serialVersionUID = 1L;

    // 状态码：200-成功 500-系统异常 400-参数错误 401-未登录 403-无权限
    private Integer code;
    // 提示信息
    private String msg;
    // 返回数据（泛型适配任意类型：对象/集合/基本类型）
    private T data;

    // 私有构造：仅通过静态方法创建
    private Result() {}
    private Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // #################### 静态快捷方法 ####################
    // 成功 - 无数据
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    // 成功 - 有数据
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    // 成功 - 自定义提示 + 数据
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(200, msg, data);
    }

    // 成功 - 带 Map 数据（适用于注册等场景）
    public static Result<Map<String, Object>> success(String msg, Map<String, Object> data) {
        return new Result<>(200, msg, data);
    }

    // 失败 - 自定义状态码 + 提示
    public static <T> Result<T> fail(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

    // 失败 - 通用系统异常
    public static <T> Result<T> error() {
        return new Result<>(500, "系统异常，请稍后重试", null);
    }

    // 失败 - 通用参数错误
    public static <T> Result<T> paramError() {
        return new Result<>(400, "参数错误，请检查后重试", null);
    }

    // 失败 - 未登录
    public static <T> Result<T> unLogin() {
        return new Result<>(401, "未登录，请先登录", null);
    }

    // 失败 - 无权限
    public static <T> Result<T> noAuth() {
        return new Result<>(403, "无权限访问", null);
    }

    // get/set方法（Servlet返回时需序列化，必须提供）
    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    // toString
    @Override
    public String toString() {
        return "Result{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
    }
}
