package com.gisplatform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gisplatform.entity.User;

/**
 * 用户 Service 接口
 * <p>
 * 定义用户相关的业务操作接口。
 * </p>
 *
 * @author GIS Platform Team
 * @version 1.0.0
 */
public interface UserService extends IService<User> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体，不存在返回 null
     */
    User getByUsername(String username);

    /**
     * 创建新用户
     *
     * @param user 用户信息
     * @return 创建成功返回 true
     */
    boolean createUser(User user);

    /**
     * 分页查询用户列表
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param username 用户名搜索关键字
     * @return 分页结果
     */
    Page<User> listUsers(int page, int pageSize, String username);

}
