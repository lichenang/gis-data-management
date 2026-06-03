package com.gisplatform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gisplatform.common.R;
import com.gisplatform.entity.User;
import com.gisplatform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户 Controller
 * <p>
 * 处理用户相关的 HTTP 请求。
 * 接口前缀：/api/v1/users
 * </p>
 *
 * @author GIS Platform Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "用户管理", description = "用户相关接口")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 分页查询用户列表
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param username 用户名搜索关键字
     * @return 用户列表分页数据
     */
    @GetMapping
    @Operation(summary = "获取用户列表", description = "分页查询用户列表，支持用户名搜索")
    public R<Page<User>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String username) {
        Page<User> result = userService.listUsers(page, pageSize, username);
        return R.ok(result);
    }

    /**
     * 根据ID获取用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取用户信息", description = "根据用户ID获取用户详细信息")
    @Parameter(name = "id", description = "用户ID")
    public R<User> getById(@PathVariable Long id) {
        User user = userService.getById(id);
        return R.ok(user);
    }

    /**
     * 根据用户名获取用户信息
     *
     * @param username 用户名
     * @return 用户信息
     */
    @GetMapping("/username/{username}")
    @Operation(summary = "根据用户名查询", description = "根据用户名获取用户信息")
    @Parameter(name = "username", description = "用户名")
    public R<User> getByUsername(@PathVariable String username) {
        User user = userService.getByUsername(username);
        return R.ok(user);
    }

    /**
     * 创建用户
     *
     * @param user 用户信息
     * @return 创建结果
     */
    @PostMapping
    @Operation(summary = "创建用户", description = "创建新用户")
    public R<Boolean> create(@RequestBody User user) {
        boolean result = userService.createUser(user);
        return R.ok(result);
    }

}
