package com.dyh.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dyh.entity.PPostCollection;
import org.apache.ibatis.annotations.Mapper;

/**
 * 冒泡/文章收藏(PPostCollection)表数据库访问层
 *
 * @author makejava
 * @since 2022-11-20 12:36:42
 */
@Mapper
public interface PPostCollectionDao extends BaseMapper<PPostCollection> {

}

