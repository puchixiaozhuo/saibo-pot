package com.xiaozhuo.service.Impl;

import com.xiaozhuo.annotation.Bean;
import com.xiaozhuo.annotation.Transactional;
import com.xiaozhuo.dao.TagDao;
import com.xiaozhuo.dao.impl.TagDaoImpl;
import com.xiaozhuo.entity.VideoTag;
import com.xiaozhuo.entity.VideoTagRelation;
import com.xiaozhuo.result.Result;
import com.xiaozhuo.service.TagService;
import com.xiaozhuo.util.ConnectionPool;
import com.xiaozhuo.util.LogUtil;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Bean
public class TagServiceImpl implements TagService {

    private static final Logger logger = LogUtil.getLogger(TagServiceImpl.class);

    private TagDao tagDao = new TagDaoImpl();

    @Override
    @Transactional
    public Result<Long> createTag(VideoTag tag) {
        Connection conn = null;
        try {
            if (tag == null || tag.getTagName() == null || tag.getTagName().trim().isEmpty()) {
                return Result.fail(400, "标签名称不能为空");
            }

            conn = ConnectionPool.getConnection();

            VideoTag existingTag = tagDao.selectByName(conn, tag.getTagName());
            if (existingTag != null) {
                return Result.fail(400, "标签名称已存在");
            }

            if (tag.getUseCount() == null) {
                tag.setUseCount(0L);
            }
            tag.setCreateTime(LocalDateTime.now());

            int rows = tagDao.insert(conn, tag);
            if (rows <= 0) {
                return Result.fail(500, "创建标签失败");
            }

            System.out.println("✅ 标签创建成功 - TagID: " + tag.getId()
                    + ", Name: " + tag.getTagName());

            return Result.success("标签创建成功", tag.getId());

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.logError(logger, "创建标签失败", e);
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    @Transactional
    public Result<Boolean> updateTag(VideoTag tag) {
        Connection conn = null;
        try {
            if (tag == null || tag.getId() == null) {
                return Result.fail(400, "标签ID不能为空");
            }

            conn = ConnectionPool.getConnection();

            VideoTag existingTag = tagDao.selectById(conn, tag.getId());
            if (existingTag == null) {
                return Result.fail(404, "标签不存在");
            }

            if (tag.getTagName() != null) {
                VideoTag duplicateTag = tagDao.selectByName(conn, tag.getTagName());
                if (duplicateTag != null && !duplicateTag.getId().equals(tag.getId())) {
                    return Result.fail(400, "标签名称已存在");
                }
                existingTag.setTagName(tag.getTagName());
            }

            if (tag.getUseCount() != null) {
                existingTag.setUseCount(tag.getUseCount());
            }

            int rows = tagDao.update(conn, existingTag);
            if (rows <= 0) {
                return Result.fail(500, "更新标签失败");
            }

            System.out.println("✅ 标签更新成功 - TagID: " + tag.getId());

            return Result.success("标签更新成功", true);

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.logError(logger, "更新标签失败", e);
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    @Transactional
    public Result<Boolean> deleteTag(Long tagId) {
        Connection conn = null;
        try {
            if (tagId == null) {
                return Result.fail(400, "标签ID不能为空");
            }

            conn = ConnectionPool.getConnection();

            VideoTag tag = tagDao.selectById(conn, tagId);
            if (tag == null) {
                return Result.fail(404, "标签不存在");
            }

            int rows = tagDao.delete(conn, tagId);
            if (rows <= 0) {
                return Result.fail(500, "删除标签失败");
            }

            System.out.println("✅ 标签删除成功 - TagID: " + tagId);

            return Result.success("标签删除成功", true);

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.logError(logger, "删除标签失败", e);
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public Result<List<VideoTag>> getAllTags() {
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();

            List<VideoTag> tags = tagDao.selectAll(conn);

            return Result.success("查询成功", tags);

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.logError(logger, "查询标签列表失败", e);
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public Result<VideoTag> getTagById(Long tagId) {
        Connection conn = null;
        try {
            if (tagId == null) {
                return Result.fail(400, "标签ID不能为空");
            }

            conn = ConnectionPool.getConnection();

            VideoTag tag = tagDao.selectById(conn, tagId);
            if (tag == null) {
                return Result.fail(404, "标签不存在");
            }

            return Result.success("查询成功", tag);

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.logError(logger, "查询标签详情失败", e);
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    @Transactional
    public Result<Boolean> addTagsToVideo(Long videoId, List<Long> tagIds) {
        Connection conn = null;
        try {
            if (videoId == null || tagIds == null || tagIds.isEmpty()) {
                return Result.fail(400, "参数不能为空");
            }

            conn = ConnectionPool.getConnection();

            for (Long tagId : tagIds) {
                VideoTag tag = tagDao.selectById(conn, tagId);
                if (tag == null) {
                    return Result.fail(404, "标签不存在: " + tagId);
                }

                VideoTagRelation relation = new VideoTagRelation();
                relation.setVideoId(videoId);
                relation.setTagId(tagId);
                relation.setCreateTime(LocalDateTime.now());

                tagDao.insertRelation(conn, relation);
                tagDao.incrementUseCount(conn, tagId);
            }

            System.out.println("✅ 视频标签添加成功 - VideoID: " + videoId
                    + ", Tags: " + tagIds.size());

            return Result.success("标签添加成功", true);

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.logError(logger, "添加视频标签失败", e);
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    @Transactional
    public Result<Boolean> removeTagFromVideo(Long videoId, Long tagId) {
        Connection conn = null;
        try {
            if (videoId == null || tagId == null) {
                return Result.fail(400, "参数不能为空");
            }

            conn = ConnectionPool.getConnection();

            tagDao.deleteRelation(conn, videoId, tagId);
            tagDao.decrementUseCount(conn, tagId);

            System.out.println("✅ 视频标签移除成功 - VideoID: " + videoId
                    + ", TagID: " + tagId);

            return Result.success("标签移除成功", true);

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.logError(logger, "移除视频标签失败", e);
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public Result<List<VideoTag>> getTagsByVideoId(Long videoId) {
        Connection conn = null;
        try {
            if (videoId == null) {
                return Result.fail(400, "视频ID不能为空");
            }

            conn = ConnectionPool.getConnection();

            List<VideoTag> tags = tagDao.selectTagsByVideoId(conn, videoId);

            return Result.success("查询成功", tags);

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.logError(logger, "查询视频标签失败", e);
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }

    @Override
    public Result<List<Long>> getVideosByTagId(Long tagId) {
        Connection conn = null;
        try {
            if (tagId == null) {
                return Result.fail(400, "标签ID不能为空");
            }

            conn = ConnectionPool.getConnection();

            List<Long> videoIds = tagDao.selectVideoIdsByTagId(conn, tagId);

            return Result.success("查询成功", videoIds);

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.logError(logger, "查询标签下的视频失败", e);
            return Result.error();
        } finally {
            if (conn != null) {
                ConnectionPool.returnConnection(conn);
            }
        }
    }
}