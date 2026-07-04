package cn.fred.service;

import cn.fred.dao.SysUserDao;
import cn.fred.entity.SysUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SysUserService {
    @Autowired
    private SysUserDao sysUserDao;
    public long count() {
        return sysUserDao.countAll();
    }
    public SysUser getByUsername(String username) {
        return sysUserDao.getByUsername(username);
    }
    public void save(SysUser user) {
        sysUserDao.insertUser(user);
    }
}