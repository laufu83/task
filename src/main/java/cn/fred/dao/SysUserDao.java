package cn.fred.dao;


import cn.fred.common.BaseDao;
import cn.fred.entity.SysUser;
import org.springframework.stereotype.Repository;

@Repository
public class SysUserDao extends BaseDao<SysUser> {
    public SysUserDao() {
        super(SysUser.class);
    }
    public long countAll() {
        return count("SELECT COUNT(*) FROM sys_user");
    }
    public SysUser getByUsername(String username) {
        return findById("SELECT * FROM sys_user WHERE username = ?", username);
    }
    public Long insertUser(SysUser user) {
        String sql = "INSERT INTO sys_user(username,password,real_name,status) VALUES (?,?,?,?)";
        return insert(sql, user.getUsername(), user.getPassword(), user.getRealName(), user.getStatus());
    }
}