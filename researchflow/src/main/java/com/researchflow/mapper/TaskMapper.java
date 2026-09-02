package com.researchflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.researchflow.entity.Task;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
