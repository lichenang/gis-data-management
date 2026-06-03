package com.gisplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gisplatform.entity.Dataset;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DatasetMapper extends BaseMapper<Dataset> {
}
