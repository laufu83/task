package cn.fred.common;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import javax.annotation.Resource;
import java.sql.PreparedStatement;
import java.util.List;

public abstract class BaseDao<T> {

    @Resource
    protected JdbcTemplate jdbcTemplate;

    protected final Class<T> entityClass;

    protected BaseDao(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    /**
     * 插入数据，返回自增主键
     */
    protected Long insert(String sql, Object... args) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : null;
    }

    /**
     * 更新数据，返回影响行数
     */
    protected int update(String sql, Object... args) {
        return jdbcTemplate.update(sql, args);
    }

    /**
     * 删除数据，返回影响行数
     */
    protected int delete(String sql, Object... args) {
        return jdbcTemplate.update(sql, args);
    }

    /**
     * 查询单条记录，返回实体对象，不存在返回 null
     */
    protected T findById(String sql, Object... args) {
        try {
            return jdbcTemplate.queryForObject(sql, args, new BeanPropertyRowMapper<>(entityClass));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 查询多条记录，返回实体列表
     */
    protected List<T> list(String sql, Object... args) {
        return jdbcTemplate.query(sql, args, new BeanPropertyRowMapper<>(entityClass));
    }

    /**
     * 统计总数
     */
    protected long count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }

    /**
     * 分页查询
     */
    protected PageResult<T> page(long pageNum, long pageSize,
                                 String countSql, String listSql,
                                 Object... args) {
        long total = count(countSql, args);
        if (total == 0) {
            return new PageResult<>(0, null, pageNum, pageSize);
        }
        long offset = (pageNum - 1) * pageSize;
        Object[] pageArgs = new Object[args.length + 2];
        System.arraycopy(args, 0, pageArgs, 0, args.length);
        pageArgs[args.length] = offset;
        pageArgs[args.length + 1] = pageSize;
        List<T> records = jdbcTemplate.query(listSql, pageArgs, new BeanPropertyRowMapper<>(entityClass));
        return new PageResult<>(total, records, pageNum, pageSize);
    }
}