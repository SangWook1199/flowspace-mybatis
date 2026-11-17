package com.flowspace.common;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DbCheckMapper {

    int countTaskStatuses();
}