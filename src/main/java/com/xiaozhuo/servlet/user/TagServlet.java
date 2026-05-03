
package com.xiaozhuo.servlet.user;

import com.alibaba.fastjson.JSON;
import com.xiaozhuo.config.AppStartupListener;
import com.xiaozhuo.exception.BusinessException;
import com.xiaozhuo.handler.GlobalExceptionHandler;
import com.xiaozhuo.ioc.ApplicationContext;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.TagService;
import com.xiaozhuo.entity.VideoTag;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/api/tag/*")
public class TagServlet extends HttpServlet {

    private TagService tagService;

    @Override
    public void init() throws ServletException {
        super.init();

        ApplicationContext context = AppStartupListener.applicationContext;
        if (context != null) {
            this.tagService = context.getBean(TagService.class);
        } else {
            throw new ServletException("ApplicationContext not initialized");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                handleGetAllTags(resp);
            } else if (pathInfo.matches("/\\d+")) {
                Long tagId = Long.parseLong(pathInfo.substring(1));
                handleGetTagById(tagId, resp);
            } else if (pathInfo.matches("/video/\\d+")) {
                Long videoId = Long.parseLong(pathInfo.substring(7));
                handleGetTagsByVideoId(videoId, resp);
            } else if (pathInfo.matches("/videos/\\d+")) {
                Long tagId = Long.parseLong(pathInfo.substring(8));
                handleGetVideosByTagId(tagId, resp);
            } else {
                throw new BusinessException(404, "接口不存在");
            }
        } catch (Exception e) {
            GlobalExceptionHandler.handleException(resp, e, req.getRequestURI());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            Long userId = (Long) req.getAttribute("userId");
            if (userId == null) {
                throw new BusinessException(401, "未登录，请先登录");
            }

            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                handleCreateTag(req, resp);
            } else if (pathInfo.matches("/video/\\d+/add")) {
                Long videoId = Long.parseLong(pathInfo.substring(7, pathInfo.indexOf("/add")));
                handleAddTagsToVideo(req, resp, videoId);
            } else {
                throw new BusinessException(404, "接口不存在");
            }
        } catch (Exception e) {
            GlobalExceptionHandler.handleException(resp, e, req.getRequestURI());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            Long userId = (Long) req.getAttribute("userId");
            if (userId == null) {
                throw new BusinessException(401, "未登录，请先登录");
            }

            String pathInfo = req.getPathInfo();
            if (pathInfo == null || !pathInfo.matches("/\\d+")) {
                throw new BusinessException(400, "请求路径错误");
            }

            Long tagId = Long.parseLong(pathInfo.substring(1));
            handleUpdateTag(req, resp, tagId);
        } catch (Exception e) {
            GlobalExceptionHandler.handleException(resp, e, req.getRequestURI());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            Long userId = (Long) req.getAttribute("userId");
            if (userId == null) {
                throw new BusinessException(401, "未登录，请先登录");
            }

            String pathInfo = req.getPathInfo();
            if (pathInfo == null) {
                throw new BusinessException(400, "请求路径错误");
            }

            if (pathInfo.matches("/\\d+")) {
                Long tagId = Long.parseLong(pathInfo.substring(1));
                handleDeleteTag(tagId, resp);
            } else if (pathInfo.matches("/video/\\d+/\\d+")) {
                String[] parts = pathInfo.substring(7).split("/");
                Long videoId = Long.parseLong(parts[0]);
                Long tagId = Long.parseLong(parts[1]);
                handleRemoveTagFromVideo(videoId, tagId, resp);
            } else {
                throw new BusinessException(404, "接口不存在");
            }
        } catch (Exception e) {
            GlobalExceptionHandler.handleException(resp, e, req.getRequestURI());
        }
    }

    private void handleCreateTag(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = req.getReader().readLine()) != null) {
            body.append(line);
        }

        if (body.length() == 0) {
            resp.getWriter().write(JSON.toJSONString(Result.fail(400, "请求体不能为空")));
            return;
        }

        VideoTag tag = JSON.parseObject(body.toString(), VideoTag.class);
        Result result = tagService.createTag(tag);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleUpdateTag(HttpServletRequest req, HttpServletResponse resp, Long tagId) throws IOException {
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = req.getReader().readLine()) != null) {
            body.append(line);
        }

        if (body.length() == 0) {
            resp.getWriter().write(JSON.toJSONString(Result.fail(400, "请求体不能为空")));
            return;
        }

        VideoTag tag = JSON.parseObject(body.toString(), VideoTag.class);
        tag.setId(tagId);
        Result result = tagService.updateTag(tag);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleDeleteTag(Long tagId, HttpServletResponse resp) throws IOException {
        Result result = tagService.deleteTag(tagId);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleGetAllTags(HttpServletResponse resp) throws IOException {
        Result result = tagService.getAllTags();
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleGetTagById(Long tagId, HttpServletResponse resp) throws IOException {
        Result result = tagService.getTagById(tagId);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleAddTagsToVideo(HttpServletRequest req, HttpServletResponse resp, Long videoId) throws IOException {
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = req.getReader().readLine()) != null) {
            body.append(line);
        }

        if (body.length() == 0) {
            resp.getWriter().write(JSON.toJSONString(Result.fail(400, "请求体不能为空")));
            return;
        }

        Map<String, Object> data = JSON.parseObject(body.toString(), Map.class);
        List<Long> tagIds = (List<Long>) data.get("tagIds");

        Result result = tagService.addTagsToVideo(videoId, tagIds);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleRemoveTagFromVideo(Long videoId, Long tagId, HttpServletResponse resp) throws IOException {
        Result result = tagService.removeTagFromVideo(videoId, tagId);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleGetTagsByVideoId(Long videoId, HttpServletResponse resp) throws IOException {
        Result result = tagService.getTagsByVideoId(videoId);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleGetVideosByTagId(Long tagId, HttpServletResponse resp) throws IOException {
        Result result = tagService.getVideosByTagId(tagId);
        resp.getWriter().write(JSON.toJSONString(result));
    }
}
