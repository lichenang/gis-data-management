package com.gisplatform.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gisplatform.entity.User;
import com.gisplatform.mapper.RoleMapper;
import com.gisplatform.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义用户详情服务
 * <p>
 * 实现 Spring Security 的 UserDetailsService 接口，
 * 根据用户名从数据库查询用户信息并转换为 UserDetails 对象。
 * 同时查询用户角色信息，构建权限。
 * </p>
 *
 * @author GIS Platform Team
 * @version 1.0.0
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    /**
     * 用户 Mapper
     */
    @Autowired
    private UserMapper userMapper;

    /**
     * 角色 Mapper
     */
    @Autowired
    private RoleMapper roleMapper;

    /**
     * 根据用户名加载用户信息
     *
     * @param username 用户名
     * @return UserDetails 用户详情
     * @throws UsernameNotFoundException 用户不存在异常
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.trim().isEmpty()) {
            throw new UsernameNotFoundException("用户名不能为空");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getDeleted, 0));

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        return buildUserDetails(user);
    }

    /**
     * 构建 UserDetails 对象
     * <p>
     * 查询用户角色，根据角色编码构建权限列表。
     * 如果用户没有角色，则默认分配 ROLE_USER。
     * </p>
     *
     * @param user 用户实体
     * @return UserDetails 对象
     */
    private UserDetails buildUserDetails(User user) {
        List<String> roleCodes = roleMapper.selectRoleCodesByUserId(user.getId());

        List<SimpleGrantedAuthority> authorities;
        if (roleCodes == null || roleCodes.isEmpty()) {
            authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        } else {
            authorities = roleCodes.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .disabled(user.getStatus() == 0)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .build();
    }
}
