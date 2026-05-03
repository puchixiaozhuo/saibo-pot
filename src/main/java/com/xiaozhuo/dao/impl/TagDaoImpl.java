package com.xiaozhuo.dao.impl;

import com.xiaozhuo.dao.TagDao;
import com.xiaozhuo.entity.VideoTag;
import com.xiaozhuo.entity.VideoTagRelation;
import com.xiaozhuo.util.LogUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class TagDaoImpl implements TagDao {

    private static final Logger logger = LogUtil.getLogger(TagDaoImpl.class);

    @Override
    public int insert(Connection conn, VideoTag tag) {
        String sql = "INSERT INTO video_tag (tag_name, use_count, create_time) VALUES (?, ?, ?)";

        LogUtil.logSql(logger, sql, new Object[]{tag.getTagName(), tag.getUseCount(), tag.getCreateTime()});

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, tag.getTagName());
            pstmt.setLong(2, tag.getUseCount() != null ? tag.getUseCount() : 0);
            pstmt.setTimestamp(3, Timestamp.valueOf(tag.getCreateTime()));

            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    tag.setId(rs.getLong(1));
                }
            }

            LogUtil.logBusiness(logger, "INSERT_TAG", "Affected " + rows + " row(s)");
            return rows;
        } catch (SQLException e) {
            LogUtil.logError(logger, "INSERT_TAG failed", e);
            throw new RuntimeException("添加标签失败", e);
        }
    }

    @Override
    public int update(Connection conn, VideoTag tag) {
        String sql = "UPDATE video_tag SET tag_name = ?, use_count = ? WHERE id = ?";

        LogUtil.logSql(logger, sql, new Object[]{tag.getTagName(), tag.getUseCount(), tag.getId()});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tag.getTagName());
            pstmt.setLong(2, tag.getUseCount());
            pstmt.setLong(3, tag.getId());

            int rows = pstmt.executeUpdate();
            LogUtil.logBusiness(logger, "UPDATE_TAG", "Affected " + rows + " row(s)");
            return rows;
        } catch (SQLException e) {
            LogUtil.logError(logger, "UPDATE_TAG failed", e);
            throw new RuntimeException("更新标签失败", e);
        }
    }

    @Override
    public int delete(Connection conn, Long tagId) {
        String sql = "DELETE FROM video_tag WHERE id = ?";

        LogUtil.logSql(logger, sql, new Object[]{tagId});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, tagId);

            int rows = pstmt.executeUpdate();
            LogUtil.logBusiness(logger, "DELETE_TAG", "Affected " + rows + " row(s)");
            return rows;
        } catch (SQLException e) {
            LogUtil.logError(logger, "DELETE_TAG failed", e);
            throw new RuntimeException("删除标签失败", e);
        }
    }

    @Override
    public VideoTag selectById(Connection conn, Long tagId) {
        String sql = "SELECT * FROM video_tag WHERE id = ?";

        LogUtil.logSql(logger, sql, new Object[]{tagId});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, tagId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    VideoTag tag = new VideoTag();
                    tag.setId(rs.getLong("id"));
                    tag.setTagName(rs.getString("tag_name"));
                    tag.setUseCount(rs.getLong("use_count"));
                    tag.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());

                    LogUtil.logBusiness(logger, "SELECT_TAG_BY_ID", "Found tag");
                    return tag;
                }
            }

            return null;
        } catch (SQLException e) {
            LogUtil.logError(logger, "SELECT_TAG_BY_ID failed", e);
            throw new RuntimeException("查询标签失败", e);
        }
    }

    @Override
    public VideoTag selectByName(Connection conn, String tagName) {
        String sql = "SELECT * FROM video_tag WHERE tag_name = ?";

        LogUtil.logSql(logger, sql, new Object[]{tagName});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tagName);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    VideoTag tag = new VideoTag();
                    tag.setId(rs.getLong("id"));
                    tag.setTagName(rs.getString("tag_name"));
                    tag.setUseCount(rs.getLong("use_count"));
                    tag.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());

                    LogUtil.logBusiness(logger, "SELECT_TAG_BY_NAME", "Found tag");
                    return tag;
                }
            }

            return null;
        } catch (SQLException e) {
            LogUtil.logError(logger, "SELECT_TAG_BY_NAME failed", e);
            throw new RuntimeException("查询标签失败", e);
        }
    }

    @Override
    public List<VideoTag> selectAll(Connection conn) {
        String sql = "SELECT * FROM video_tag ORDER BY use_count DESC, create_time DESC";

        LogUtil.logSql(logger, sql, new Object[]{});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            List<VideoTag> list = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    VideoTag tag = new VideoTag();
                    tag.setId(rs.getLong("id"));
                    tag.setTagName(rs.getString("tag_name"));
                    tag.setUseCount(rs.getLong("use_count"));
                    tag.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                    list.add(tag);
                }
            }

            LogUtil.logBusiness(logger, "SELECT_ALL_TAGS", "Found " + list.size() + " tags");
            return list;
        } catch (SQLException e) {
            LogUtil.logError(logger, "SELECT_ALL_TAGS failed", e);
            throw new RuntimeException("查询标签列表失败", e);
        }
    }

    @Override
    public int incrementUseCount(Connection conn, Long tagId) {
        String sql = "UPDATE video_tag SET use_count = use_count + 1 WHERE id = ?";

        LogUtil.logSql(logger, sql, new Object[]{tagId});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, tagId);

            int rows = pstmt.executeUpdate();
            LogUtil.logBusiness(logger, "INCREMENT_TAG_USE_COUNT", "Affected " + rows + " row(s)");
            return rows;
        } catch (SQLException e) {
            LogUtil.logError(logger, "INCREMENT_TAG_USE_COUNT failed", e);
            throw new RuntimeException("增加标签使用次数失败", e);
        }
    }

    @Override
    public int decrementUseCount(Connection conn, Long tagId) {
        String sql = "UPDATE video_tag SET use_count = GREATEST(use_count - 1, 0) WHERE id = ?";

        LogUtil.logSql(logger, sql, new Object[]{tagId});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, tagId);

            int rows = pstmt.executeUpdate();
            LogUtil.logBusiness(logger, "DECREMENT_TAG_USE_COUNT", "Affected " + rows + " row(s)");
            return rows;
        } catch (SQLException e) {
            LogUtil.logError(logger, "DECREMENT_TAG_USE_COUNT failed", e);
            throw new RuntimeException("减少标签使用次数失败", e);
        }
    }

    @Override
    public int insertRelation(Connection conn, VideoTagRelation relation) {
        String sql = "INSERT INTO video_tag_relation (video_id, tag_id, create_time) VALUES (?, ?, ?)";

        LogUtil.logSql(logger, sql, new Object[]{
            relation.getVideoId(), relation.getTagId(), relation.getCreateTime()
        });

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, relation.getVideoId());
            pstmt.setLong(2, relation.getTagId());
            pstmt.setTimestamp(3, Timestamp.valueOf(relation.getCreateTime()));

            int rows = pstmt.executeUpdate();
            LogUtil.logBusiness(logger, "INSERT_TAG_RELATION", "Affected " + rows + " row(s)");
            return rows;
        } catch (SQLException e) {
            LogUtil.logError(logger, "INSERT_TAG_RELATION failed", e);
            throw new RuntimeException("添加标签关联失败", e);
        }
    }

    @Override
    public int deleteRelation(Connection conn, Long videoId, Long tagId) {
        String sql = "DELETE FROM video_tag_relation WHERE video_id = ? AND tag_id = ?";

        LogUtil.logSql(logger, sql, new Object[]{videoId, tagId});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, videoId);
            pstmt.setLong(2, tagId);

            int rows = pstmt.executeUpdate();
            LogUtil.logBusiness(logger, "DELETE_TAG_RELATION", "Affected " + rows + " row(s)");
            return rows;
        } catch (SQLException e) {
            LogUtil.logError(logger, "DELETE_TAG_RELATION failed", e);
            throw new RuntimeException("删除标签关联失败", e);
        }
    }

    @Override
    public int deleteRelationsByVideoId(Connection conn, Long videoId) {
        String sql = "DELETE FROM video_tag_relation WHERE video_id = ?";

        LogUtil.logSql(logger, sql, new Object[]{videoId});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, videoId);

            int rows = pstmt.executeUpdate();
            LogUtil.logBusiness(logger, "DELETE_TAG_RELATIONS_BY_VIDEO", "Affected " + rows + " row(s)");
            return rows;
        } catch (SQLException e) {
            LogUtil.logError(logger, "DELETE_TAG_RELATIONS_BY_VIDEO failed", e);
            throw new RuntimeException("删除视频标签关联失败", e);
        }
    }

    @Override
    public List<VideoTag> selectTagsByVideoId(Connection conn, Long videoId) {
        String sql = "SELECT t.* FROM video_tag t INNER JOIN video_tag_relation r ON t.id = r.tag_id WHERE r.video_id = ?";

        LogUtil.logSql(logger, sql, new Object[]{videoId});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, videoId);

            List<VideoTag> list = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    VideoTag tag = new VideoTag();
                    tag.setId(rs.getLong("id"));
                    tag.setTagName(rs.getString("tag_name"));
                    tag.setUseCount(rs.getLong("use_count"));
                    tag.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                    list.add(tag);
                }
            }

            LogUtil.logBusiness(logger, "SELECT_TAGS_BY_VIDEO", "Found " + list.size() + " tags");
            return list;
        } catch (SQLException e) {
            LogUtil.logError(logger, "SELECT_TAGS_BY_VIDEO failed", e);
            throw new RuntimeException("查询视频标签失败", e);
        }
    }

    @Override
    public List<Long> selectVideoIdsByTagId(Connection conn, Long tagId) {
        String sql = "SELECT video_id FROM video_tag_relation WHERE tag_id = ?";

        LogUtil.logSql(logger, sql, new Object[]{tagId});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, tagId);

            List<Long> list = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getLong("video_id"));
                }
            }

            LogUtil.logBusiness(logger, "SELECT_VIDEOS_BY_TAG", "Found " + list.size() + " videos");
            return list;
        } catch (SQLException e) {
            LogUtil.logError(logger, "SELECT_VIDEOS_BY_TAG failed", e);
            throw new RuntimeException("查询标签下的视频失败", e);
        }
    }
}