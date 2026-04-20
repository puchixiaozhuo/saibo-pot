package com.xiaozhuo.constant;

public final class MessageConstant {

    private MessageConstant() {
        throw new IllegalStateException("Constant class");
    }

    public static final String USER_NOT_FOUND = "用户不存在";
    public static final String USER_ALREADY_EXISTS = "用户名已存在";
    public static final String USER_DISABLED = "账号已被禁用";
    public static final String PASSWORD_ERROR = "密码错误";
    public static final String REGISTER_SUCCESS = "注册成功";
    public static final String LOGIN_SUCCESS = "登录成功";
    public static final String LOGOUT_SUCCESS = "登出成功";

    public static final String TOKEN_INVALID = "Token 无效或已过期";
    public static final String TOKEN_MISSING = "请先登录";
    public static final String TOKEN_REFRESH_SUCCESS = "Token 刷新成功";

    public static final String VIDEO_NOT_FOUND = "视频不存在";
    public static final String VIDEO_DELETED = "视频已被删除";
    public static final String VIDEO_ADD_SUCCESS = "视频发布成功";
    public static final String VIDEO_DELETE_SUCCESS = "视频删除成功";

    public static final String COMMENT_NOT_FOUND = "评论不存在";
    public static final String COMMENT_ADD_SUCCESS = "评论成功";
    public static final String COMMENT_DELETE_SUCCESS = "评论删除成功";
    public static final String COMMENT_CONTENT_TOO_LONG = "评论内容过长";

    public static final String ALREADY_FOLLOWED = "已关注该用户";
    public static final String FOLLOW_SUCCESS = "关注成功";
    public static final String UNFOLLOW_SUCCESS = "取消关注成功";
    public static final String CANNOT_FOLLOW_SELF = "不能关注自己";

    public static final String ALREADY_LIKED = "已点过赞";
    public static final String LIKE_SUCCESS = "点赞成功";
    public static final String UNLIKE_SUCCESS = "取消点赞成功";

    public static final String PERMISSION_DENIED = "权限不足，无法访问";
    public static final String PARAM_ERROR = "参数错误";
    public static final String SYSTEM_ERROR = "系统繁忙，请稍后重试";
}
