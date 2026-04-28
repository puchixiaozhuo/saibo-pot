package com.xiaozhuo.servlet.user;

import com.alibaba.fastjson.JSON;
import com.xiaozhuo.config.AppStartupListener;
import com.xiaozhuo.entity.VideoInfo;
import com.xiaozhuo.exception.BusinessException;
import com.xiaozhuo.handler.GlobalExceptionHandler;
import com.xiaozhuo.ioc.ApplicationContext;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.VideoService;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * VideoServlet - 使用IoC容器和统一异常处理
 */
@WebServlet("/api/video/*")
public class VideoServlet extends HttpServlet {

    private VideoService videoService;

    @Override
    public void init() throws ServletException {
        super.init();

        ApplicationContext context = AppStartupListener.applicationContext;
        if (context != null) {
            this.videoService = context.getBean(VideoService.class);
        } else {
            throw new ServletException("ApplicationContext not initialized");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null) {
                throw new BusinessException(400, "请求路径错误");
            }

            if (pathInfo.startsWith("/list")) {
                handleGetVideoList(req, resp);
            } else if (pathInfo.matches("/\\d+")) {
                Long id = Long.parseLong(pathInfo.substring(1));
                handleGetVideoById(id, resp);
            } else if (pathInfo.startsWith("/author/")) {
                handleGetVideosByAuthor(req, pathInfo, resp);
            } else if (pathInfo.startsWith("/category/")) {
                handleGetVideosByCategory(req, pathInfo, resp);
            } else if (pathInfo.startsWith("/search")) {
                handleSearchVideos(req, resp);
            } else if (pathInfo.startsWith("/download/")) {
                handleGetDownloadUrl(req, pathInfo, resp);
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
            String pathInfo = req.getPathInfo();
            if (pathInfo == null) {
                throw new BusinessException(400, "请求路径错误");
            }

            if (pathInfo.equals("/upload")) {
                handleUploadVideo(req, resp);
            } else if (pathInfo.matches("/\\d+/like")) {
                Long id = Long.parseLong(pathInfo.substring(1, pathInfo.indexOf("/like")));
                handleLikeVideo(req, id, resp);
            } else if (pathInfo.matches("/\\d+/unlike")) {
                Long id = Long.parseLong(pathInfo.substring(1, pathInfo.indexOf("/unlike")));
                handleUnlikeVideo(req, id, resp);
            } else if (pathInfo.matches("/\\d+/cache")) {
                Long id = Long.parseLong(pathInfo.substring(1, pathInfo.indexOf("/cache")));
                handleCacheVideo(id, resp);
            } else {
                throw new BusinessException(404, "接口不存在");
            }
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
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                Long id = Long.parseLong(pathInfo.substring(1));
                handleDeleteVideo(userId, id, resp);
            } else {
                throw new BusinessException(400, "请求路径错误");
            }
        } catch (Exception e) {
            GlobalExceptionHandler.handleException(resp, e, req.getRequestURI());
        }
    }

    private void handleGetVideoById(Long id, HttpServletResponse resp) throws IOException {
        Result<VideoInfo> result = videoService.getVideoById(id);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleGetVideoList(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int pageNum = getIntParameter(req, "pageNum", 1);
        int pageSize = getIntParameter(req, "pageSize", 10);
        Result<List<VideoInfo>> result = videoService.getVideoList(pageNum, pageSize);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleGetVideosByAuthor(HttpServletRequest req, String pathInfo, HttpServletResponse resp) throws IOException {
        String[] parts = pathInfo.split("/");
        Long authorId = Long.parseLong(parts[2]);
        int pageNum = getIntParameter(req, "pageNum", 1);
        int pageSize = getIntParameter(req, "pageSize", 10);
        Result<List<VideoInfo>> result = videoService.getVideosByAuthorId(authorId, pageNum, pageSize);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleGetVideosByCategory(HttpServletRequest req, String pathInfo, HttpServletResponse resp) throws IOException {
        String[] parts = pathInfo.split("/");
        Long categoryId = Long.parseLong(parts[2]);
        int pageNum = getIntParameter(req, "pageNum", 1);
        int pageSize = getIntParameter(req, "pageSize", 10);
        Result<List<VideoInfo>> result = videoService.getVideosByCategoryId(categoryId, pageNum, pageSize);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleSearchVideos(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String keyword = req.getParameter("keyword");
        int pageNum = getIntParameter(req, "pageNum", 1);
        int pageSize = getIntParameter(req, "pageSize", 10);
        Result<List<VideoInfo>> result = videoService.searchVideos(keyword, pageNum, pageSize);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleUploadVideo(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (!ServletFileUpload.isMultipartContent(req)) {
            throw new BusinessException(400, "请使用multipart/form-data格式上传");
        }

        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "未登录，请先登录");
        }

        DiskFileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        upload.setHeaderEncoding("UTF-8");

        List<FileItem> items = upload.parseRequest(req);

        VideoInfo video = new VideoInfo();
        video.setAuthorId(userId);
        byte[] videoBytes = null;
        String videoFileName = null;
        byte[] coverBytes = null;
        String coverFileName = null;

        for (FileItem item : items) {
            if (item.isFormField()) {
                String fieldName = item.getFieldName();
                String fieldValue = item.getString("UTF-8");

                switch (fieldName) {
                    case "title":
                        video.setTitle(fieldValue);
                        break;
                    case "description":
                        video.setDescription(fieldValue);
                        break;
                    case "categoryId":
                        video.setCategoryId(Long.parseLong(fieldValue));
                        break;
                    case "resolution":
                        video.setResolution(fieldValue);
                        break;
                }
            } else {
                String fileName = item.getName();
                if (fileName != null && !fileName.isEmpty()) {
                    if (item.getFieldName().equals("video")) {
                        videoBytes = item.get();
                        videoFileName = fileName;
                    } else if (item.getFieldName().equals("cover")) {
                        coverBytes = item.get();
                        coverFileName = fileName;
                    }
                }
            }
        }

        if (videoBytes == null || videoFileName == null) {
            throw new BusinessException(400, "视频文件不能为空");
        }

        if (video.getTitle() == null || video.getTitle().trim().isEmpty()) {
            throw new BusinessException(400, "标题不能为空");
        }

        Result<Map<String, Object>> result = videoService.uploadVideo(video, videoBytes, videoFileName, coverBytes, coverFileName);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleDeleteVideo(Long userId, Long id, HttpServletResponse resp) throws IOException {
        Result<Void> result = videoService.deleteVideo(id);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleLikeVideo(HttpServletRequest req, Long videoId, HttpServletResponse resp) throws IOException {
        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "未登录，请先登录");
        }
        Result<Void> result = videoService.likeVideo(userId, videoId);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleUnlikeVideo(HttpServletRequest req, Long videoId, HttpServletResponse resp) throws IOException {
        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "未登录，请先登录");
        }
        Result<Void> result = videoService.unlikeVideo(userId, videoId);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleCacheVideo(Long videoId, HttpServletResponse resp) throws IOException {
        Result<Map<String, Object>> result = videoService.cacheVideoToLocal(videoId);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private void handleGetDownloadUrl(HttpServletRequest req, String pathInfo, HttpServletResponse resp) throws IOException {
        String[] parts = pathInfo.split("/");
        Long videoId = Long.parseLong(parts[2]);
        Result<VideoInfo> result = videoService.getVideoDownloadUrl(videoId);
        resp.getWriter().write(JSON.toJSONString(result));
    }

    private int getIntParameter(HttpServletRequest req, String name, int defaultValue) {
        String value = req.getParameter(name);
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
