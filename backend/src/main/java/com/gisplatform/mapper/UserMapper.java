package com.gisplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gisplatform.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper 接口
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，提供用户表的 CRUD 操作。
 * </p>
 *
 * @author GIS Platform Team
 * @version 1.0.0
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}
