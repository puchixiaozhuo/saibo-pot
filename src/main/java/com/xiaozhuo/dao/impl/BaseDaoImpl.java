package com.xiaozhuo.dao.impl;

import com.xiaozhuo.annotation.Column;
import com.xiaozhuo.annotation.TableName;
import com.xiaozhuo.dao.BaseDao;
import com.xiaozhuo.exception.DatabaseException;
import com.xiaozhuo.util.LogUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 基础 DAO 实现类：使用反射机制实现通用 CRUD 操作
 * @param <T> 实体类型
 */
public class BaseDaoImpl<T> implements BaseDao<T> {

    private static final Logger logger = LogUtil.getLogger(BaseDaoImpl.class);

    private final Class<T> entityClass;
    private final String tableName;
    private final Field primaryKeyField;
    private final String primaryKeyName;

    public BaseDaoImpl(Class<T> entityClass) {
        this.entityClass = entityClass;

        TableName tableNameAnnotation = entityClass.getAnnotation(TableName.class);
        if (tableNameAnnotation == null) {
            throw new RuntimeException("Entity class must have @TableName annotation: " + entityClass.getName());
        }
        this.tableName = tableNameAnnotation.value();

        Field pkField = null;
        String pkName = null;
        for (Field field : entityClass.getDeclaredFields()) {
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation != null && columnAnnotation.isPrimaryKey()) {
                field.setAccessible(true);
                pkField = field;
                pkName = columnAnnotation.value();
                break;
            }
        }
        this.primaryKeyField = pkField;
        this.primaryKeyName = pkName;

        logger.info("BaseDaoImpl initialized for entity: " + entityClass.getSimpleName()
                  + ", table: " + tableName
                  + ", primary key: " + pkName);
    }

    @Override
    public int insert(Connection conn, T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();

        for (Field field : entityClass.getDeclaredFields()) {
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation == null) {
                continue;
            }

            if (columnAnnotation.isAutoIncrement()) {
                continue;
            }

            field.setAccessible(true);
            try {
                Object value = field.get(entity);
                if (value != null) {
                    columns.add(columnAnnotation.value());
                    values.add(value);
                    placeholders.add("?");
                }
            } catch (IllegalAccessException e) {
                LogUtil.logError(logger, "Failed to access field: " + field.getName(), e);
            }
        }

        if (columns.isEmpty()) {
            throw new DatabaseException("No valid fields to insert", null);
        }

        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)",
                tableName,
                String.join(", ", columns),
                String.join(", ", placeholders));

        LogUtil.logSql(logger, sql, values.toArray());

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < values.size(); i++) {
                setParameter(pstmt, i + 1, values.get(i));
            }

            int rows = pstmt.executeUpdate();

            if (rows > 0 && primaryKeyField != null) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        Object generatedId = rs.getObject(1);
                        primaryKeyField.setAccessible(true);
                        try {
                            primaryKeyField.set(entity, convertToFieldType(primaryKeyField, generatedId));
                            LogUtil.logConnection(logger, "Generated key set: " + generatedId);
                        } catch (IllegalAccessException e) {
                            LogUtil.logError(logger, "Failed to set generated key to entity", e);
                        }
                    }
                }
            }

            LogUtil.logBusiness(logger, "INSERT", "Affected " + rows + " row(s) in table " + tableName);
            return rows;
        } catch (SQLException e) {
            LogUtil.logError(logger, "INSERT failed for table " + tableName, e);
            throw new DatabaseException("INSERT failed for table " + tableName, e);
        }
    }

    @Override
    public int deleteById(Connection conn, Object id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }

        String sql = String.format("DELETE FROM %s WHERE %s = ?", tableName, primaryKeyName);

        LogUtil.logSql(logger, sql, new Object[]{id});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            setParameter(pstmt, 1, id);
            int rows = pstmt.executeUpdate();
            LogUtil.logBusiness(logger, "DELETE", "Affected " + rows + " row(s) in table " + tableName);
            return rows;
        } catch (SQLException e) {
            LogUtil.logError(logger, "DELETE failed for table " + tableName + " with id=" + id, e);
            throw new DatabaseException("DELETE failed", e);
        }
    }

    @Override
    public int update(Connection conn, T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        if (primaryKeyField == null) {
            throw new DatabaseException("No primary key defined for entity: " + entityClass.getName(), null);
        }

        List<String> setClauses = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        Object primaryKeyValue = null;

        for (Field field : entityClass.getDeclaredFields()) {
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation == null) {
                continue;
            }

            field.setAccessible(true);
            try {
                Object value = field.get(entity);

                if (columnAnnotation.isPrimaryKey()) {
                    primaryKeyValue = value;
                    continue;
                }

                if (value != null) {
                    setClauses.add(columnAnnotation.value() + " = ?");
                    values.add(value);
                }
            } catch (IllegalAccessException e) {
                logger.log(Level.WARNING, "Failed to access field: " + field.getName(), e);
            }
        }

        if (setClauses.isEmpty()) {
            throw new DatabaseException("No valid fields to update", null);
        }

        if (primaryKeyValue == null) {
            throw new DatabaseException("Primary key value cannot be null for update", null);
        }

        values.add(primaryKeyValue);

        String sql = String.format("UPDATE %s SET %s WHERE %s = ?",
                tableName,
                String.join(", ", setClauses),
                primaryKeyName);

        LogUtil.logSql(logger, sql, values.toArray());

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < values.size(); i++) {
                setParameter(pstmt, i + 1, values.get(i));
            }

            int rows = pstmt.executeUpdate();
            LogUtil.logBusiness(logger, "UPDATE", "Affected " + rows + " row(s) in table " + tableName);
            return rows;
        } catch (SQLException e) {
            LogUtil.logError(logger, "UPDATE failed for table " + tableName, e);
            throw new DatabaseException("UPDATE failed", e);
        }
    }

    @Override
    public T findById(Connection conn, Object id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }

        String sql = String.format("SELECT * FROM %s WHERE %s = ?", tableName, primaryKeyName);

        LogUtil.logSql(logger, sql, new Object[]{id});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            setParameter(pstmt, 1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    T entity = mapResultSetToEntity(rs);
                    LogUtil.logBusiness(logger, "FIND_BY_ID", "Found entity in table " + tableName);
                    return entity;
                }
            }

            LogUtil.logBusiness(logger, "FIND_BY_ID", "No entity found in table " + tableName + " with id=" + id);
            return null;
        } catch (SQLException e) {
            LogUtil.logError(logger, "FIND_BY_ID failed for table " + tableName + " with id=" + id, e);
            throw new DatabaseException("FIND_BY_ID failed", e);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            LogUtil.logError(logger, "Failed to create entity instance", e);
            throw new DatabaseException("Failed to create entity instance", e);
        }
    }

    @Override
    public List<T> findAll(Connection conn) {
        String sql = String.format("SELECT * FROM %s", tableName);

        LogUtil.logSql(logger, sql, new Object[]{});

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            List<T> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapResultSetToEntity(rs));
            }

            LogUtil.logBusiness(logger, "FIND_ALL", "Found " + list.size() + " entities in table " + tableName);
            return list;
        } catch (SQLException e) {
            LogUtil.logError(logger, "FIND_ALL failed for table " + tableName, e);
            throw new DatabaseException("FIND_ALL failed", e);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            LogUtil.logError(logger, "Failed to create entity instance", e);
            throw new DatabaseException("Failed to create entity instance", e);
        }
    }

    @Override
    public List<T> findByCondition(Connection conn, Map<String, Object> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return findAll(conn);
        }

        List<String> whereClauses = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            if (entry.getValue() != null) {
                whereClauses.add(entry.getKey() + " = ?");
                values.add(entry.getValue());
            }
        }

        if (whereClauses.isEmpty()) {
            return findAll(conn);
        }

        String sql = String.format("SELECT * FROM %s WHERE %s",
                tableName,
                String.join(" AND ", whereClauses));

        LogUtil.logSql(logger, sql, values.toArray());

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < values.size(); i++) {
                setParameter(pstmt, i + 1, values.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                List<T> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapResultSetToEntity(rs));
                }

                LogUtil.logBusiness(logger, "FIND_BY_CONDITION", "Found " + list.size() + " entities in table " + tableName);
                return list;
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "FIND_BY_CONDITION failed for table " + tableName, e);
            throw new DatabaseException("FIND_BY_CONDITION failed", e);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            LogUtil.logError(logger, "Failed to create entity instance", e);
            throw new DatabaseException("Failed to create entity instance", e);
        }
    }

    @Override
    public List<T> findByPage(Connection conn, int pageNum, int pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 10;

        int offset = (pageNum - 1) * pageSize;
        String sql = String.format("SELECT * FROM %s LIMIT ? OFFSET ?", tableName);

        LogUtil.logSql(logger, sql, new Object[]{pageSize, offset});

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, pageSize);
            pstmt.setInt(2, offset);

            try (ResultSet rs = pstmt.executeQuery()) {
                List<T> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapResultSetToEntity(rs));
                }

                LogUtil.logBusiness(logger, "FIND_BY_PAGE", "Found " + list.size() + " entities in table " + tableName);
                return list;
            }
        } catch (SQLException e) {
            LogUtil.logError(logger, "FIND_BY_PAGE failed for table " + tableName, e);
            throw new DatabaseException("FIND_BY_PAGE failed", e);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            LogUtil.logError(logger, "Failed to create entity instance", e);
            throw new DatabaseException("Failed to create entity instance", e);
        }
    }

    @Override
    public long count(Connection conn) {
        String sql = String.format("SELECT COUNT(*) FROM %s", tableName);

        LogUtil.logSql(logger, sql, new Object[]{});

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                long count = rs.getLong(1);
                LogUtil.logBusiness(logger, "COUNT", "Total " + count + " entities in table " + tableName);
                return count;
            }

            return 0;
        } catch (SQLException e) {
            LogUtil.logError(logger, "COUNT failed for table " + tableName, e);
            throw new DatabaseException("COUNT failed", e);
        }
    }

    /**
     * 将 ResultSet 映射为实体对象
     */
    private T mapResultSetToEntity(ResultSet rs) throws SQLException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        T entity = entityClass.getDeclaredConstructor().newInstance();

        for (Field field : entityClass.getDeclaredFields()) {
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation == null) {
                continue;
            }

            String columnName = columnAnnotation.value();
            field.setAccessible(true);

            try {
                Object value = rs.getObject(columnName);
                if (value != null) {
                    value = convertToFieldType(field, value);
                    field.set(entity, value);
                }
            } catch (SQLException e) {
                logger.log(Level.FINE, "Column not found: " + columnName, e);
            }
        }

        return entity;
    }

    /**
     * 设置 PreparedStatement 参数
     */
    private void setParameter(PreparedStatement pstmt, int index, Object value) throws SQLException {
        if (value instanceof LocalDateTime) {
            pstmt.setTimestamp(index, Timestamp.valueOf((LocalDateTime) value));
        } else if (value instanceof Integer) {
            pstmt.setInt(index, (Integer) value);
        } else if (value instanceof Long) {
            pstmt.setLong(index, (Long) value);
        } else if (value instanceof String) {
            pstmt.setString(index, (String) value);
        } else {
            pstmt.setObject(index, value);
        }
    }

    /**
     * 将数据库值转换为字段类型
     */
    private Object convertToFieldType(Field field, Object value) {
        if (value == null) {
            return null;
        }

        Class<?> fieldType = field.getType();

        if (fieldType == Long.class || fieldType == long.class) {
            if (value instanceof BigInteger) {
                return ((BigInteger) value).longValue();
            } else if (value instanceof Number) {
                return ((Number) value).longValue();
            }
        } else if (fieldType == Integer.class || fieldType == int.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        } else if (fieldType == LocalDateTime.class) {
            if (value instanceof Timestamp) {
                return ((Timestamp) value).toLocalDateTime();
            }
        }

        return value;
    }
}
