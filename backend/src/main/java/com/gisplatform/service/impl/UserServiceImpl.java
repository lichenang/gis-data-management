package com.gisplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gisplatform.entity.User;
import com.gisplatform.mapper.UserMapper;
import com.gisplatform.service.UserService;
import org.springframework.stereotype.Service;

/**
 * 用户 Service 实现类
 * <p>
 * 实现用户相关业务逻辑。
 * </p>
 *
 * @author GIS Platform Team
 * @version 1.0.0
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体，不存在返回 null
     */
    @Override
    public User getByUsername(String username) {
        return this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
    }

    /**
     * 创建新用户
     *
     * @param user 用户信息
     * @return 创建成功返回 true
     */
    @Override
    public boolean createUser(User user) {
        return this.save(user);
    }

    @Override
    public Page<User> listUsers(int page, int pageSize, String username) {
        Page<User> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isEmpty()) {
            wrapper.like(User::getUsername, username);
        }
        return this.page(pageObj, wrapper);
    }

}
