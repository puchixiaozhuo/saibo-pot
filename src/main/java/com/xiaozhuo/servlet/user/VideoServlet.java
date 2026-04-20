package com.xiaozhuo.servlet.user;

import com.alibaba.fastjson.JSON;
import com.xiaozhuo.entity.VideoInfo;
import com.xiaozhuo.service.VideoService;
import com.xiaozhuo.service.Impl.VideoServiceImpl;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/video/*")
public class VideoServlet extends HttpServlet {

    private VideoService videoService = new VideoServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            out.print(JSON.toJSONString(errorResult(400, "请求路径错误")));
            return;
        }

        try {
            if (pathInfo.startsWith("/list")) {
                handleGetVideoList(req, out);
            } else if (pathInfo.matches("/\\d+")) {
                Long id = Long.parseLong(pathInfo.substring(1));
                handleGetVideoById(id, out);
            } else if (pathInfo.startsWith("/author/")) {
                handleGetVideosByAuthor(req, pathInfo, out);
            } else if (pathInfo.startsWith("/category/")) {
                handleGetVideosByCategory(req, pathInfo, out);
            } else if (pathInfo.startsWith("/search")) {
                handleSearchVideos(req, out);
            } else if (pathInfo.startsWith("/download/")) {
                handleGetDownloadUrl(req, pathInfo, out);
            } else {
                out.print(JSON.toJSONString(errorResult(404, "接口不存在")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print(JSON.toJSONString(errorResult(500, "服务器内部错误：" + e.getMessage())));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            out.print(JSON.toJSONString(errorResult(400, "请求路径错误")));
            return;
        }

        try {
            if (pathInfo.equals("/upload")) {
                handleUploadVideo(req, out);
            } else if (pathInfo.matches("/\\d+/like")) {
                Long id = Long.parseLong(pathInfo.substring(1, pathInfo.indexOf("/like")));
                handleLikeVideo(req, id, out);
            } else if (pathInfo.matches("/\\d+/unlike")) {
                Long id = Long.parseLong(pathInfo.substring(1, pathInfo.indexOf("/unlike")));
                handleUnlikeVideo(req, id, out);
            }else if (pathInfo.matches("/\\d+/cache")) {
                Long userId = (Long) req.getAttribute("userId");
                if (userId == null) {
                    out.print(JSON.toJSONString(errorResult(401, "未登录，请先登录")));
                    return;
                }
                Long id = Long.parseLong(pathInfo.substring(1, pathInfo.indexOf("/cache")));
                handleCacheVideo(userId, id, out);
            } else {
                out.print(JSON.toJSONString(errorResult(404, "接口不存在")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print(JSON.toJSONString(errorResult(500, "服务器内部错误：" + e.getMessage())));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) {
            out.print(JSON.toJSONString(errorResult(401, "未登录，请先登录")));
            return;
        }

        String pathInfo = req.getPathInfo();
        if (pathInfo != null && pathInfo.matches("/\\d+")) {
            Long id = Long.parseLong(pathInfo.substring(1));
            handleDeleteVideo(userId, id, out);
        } else {
            out.print(JSON.toJSONString(errorResult(400, "请求路径错误")));
        }
    }

    private void handleGetVideoById(Long id, PrintWriter out) {
        Map<String, Object> result = videoService.getVideoById(id);
        out.print(JSON.toJSONString(result));
    }

    private void handleGetVideoList(HttpServletRequest req, PrintWriter out) {
        int pageNum = getIntParameter(req, "pageNum", 1);
        int pageSize = getIntParameter(req, "pageSize", 10);
        Map<String, Object> result = videoService.getVideoList(pageNum, pageSize);
        out.print(JSON.toJSONString(result));
    }

    private void handleGetVideosByAuthor(HttpServletRequest req, String pathInfo, PrintWriter out) {
        try {
            String[] parts = pathInfo.split("/");
            Long authorId = Long.parseLong(parts[2]);
            int pageNum = getIntParameter(req, "pageNum", 1);
            int pageSize = getIntParameter(req, "pageSize", 10);
            Map<String, Object> result = videoService.getVideosByAuthorId(authorId, pageNum, pageSize);
            out.print(JSON.toJSONString(result));
        } catch (Exception e) {
            out.print(JSON.toJSONString(errorResult(400, "参数错误")));
        }
    }

    private void handleGetVideosByCategory(HttpServletRequest req, String pathInfo, PrintWriter out) {
        try {
            String[] parts = pathInfo.split("/");
            Long categoryId = Long.parseLong(parts[2]);
            int pageNum = getIntParameter(req, "pageNum", 1);
            int pageSize = getIntParameter(req, "pageSize", 10);
            Map<String, Object> result = videoService.getVideosByCategoryId(categoryId, pageNum, pageSize);
            out.print(JSON.toJSONString(result));
        } catch (Exception e) {
            out.print(JSON.toJSONString(errorResult(400, "参数错误")));
        }
    }

    private void handleSearchVideos(HttpServletRequest req, PrintWriter out) {
        String keyword = req.getParameter("keyword");
        int pageNum = getIntParameter(req, "pageNum", 1);
        int pageSize = getIntParameter(req, "pageSize", 10);
        Map<String, Object> result = videoService.searchVideos(keyword, pageNum, pageSize);
        out.print(JSON.toJSONString(result));
    }

    private void handleUploadVideo(HttpServletRequest req, PrintWriter out) throws Exception {
        if (!ServletFileUpload.isMultipartContent(req)) {
            out.print(JSON.toJSONString(errorResult(400, "请使用multipart/form-data格式上传")));
            return;
        }

        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) {
            out.print(JSON.toJSONString(errorResult(401, "未登录，请先登录")));
            return;
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
            out.print(JSON.toJSONString(errorResult(400, "视频文件不能为空")));
            return;
        }

        if (video.getTitle() == null) {
            out.print(JSON.toJSONString(errorResult(400, "标题不能为空")));
            return;
        }

        Map<String, Object> result = videoService.uploadVideo(video, videoBytes, videoFileName, coverBytes, coverFileName);
        out.print(JSON.toJSONString(result));
    }

    private void handleDeleteVideo(Long userId, Long id, PrintWriter out) {
        Map<String, Object> result = videoService.deleteVideo(id);
        out.print(JSON.toJSONString(result));
    }

    private void handleLikeVideo(HttpServletRequest req, Long videoId, PrintWriter out) {
        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) {
            out.print(JSON.toJSONString(errorResult(401, "未登录，请先登录")));
            return;
        }
        Map<String, Object> result = videoService.likeVideo(userId, videoId);
        out.print(JSON.toJSONString(result));
    }

    private void handleUnlikeVideo(HttpServletRequest req, Long videoId, PrintWriter out) {
        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) {
            out.print(JSON.toJSONString(errorResult(401, "未登录，请先登录")));
            return;
        }
        Map<String, Object> result = videoService.unlikeVideo(userId, videoId);
        out.print(JSON.toJSONString(result));
    }


    private void handleCacheVideo(Long userId, Long videoId, PrintWriter out) {
        Map<String, Object> result = videoService.cacheVideoToLocal(videoId);
        out.print(JSON.toJSONString(result));
    }

    private void handleGetDownloadUrl(HttpServletRequest req, String pathInfo, PrintWriter out) {
        try {
            String[] parts = pathInfo.split("/");
            Long videoId = Long.parseLong(parts[2]);
            Map<String, Object> result = videoService.getVideoDownloadUrl(videoId);
            out.print(JSON.toJSONString(result));
        } catch (Exception e) {
            out.print(JSON.toJSONString(errorResult(400, "参数错误")));
        }
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

    private Long getLongParameter(HttpServletRequest req, String name, Long defaultValue) {
        String value = req.getParameter(name);
        if (value != null && !value.isEmpty()) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private Map<String, Object> errorResult(int code, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        return result;
    }
}
