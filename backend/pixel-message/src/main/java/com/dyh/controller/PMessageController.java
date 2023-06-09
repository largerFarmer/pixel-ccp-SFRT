package com.dyh.controller;



import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dyh.entity.PMessage;
import com.dyh.service.PMessageService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.List;

/**
 * 消息通知(PMessage)表控制层
 *
 * @author makejava
 * @since 2022-11-20 13:30:42
 */
@RestController
@RequestMapping("/api/pMessage")
public class PMessageController extends ApiController {
    /**
     * 服务对象
     */
    @Resource
    private PMessageService pMessageService;

    /**
     * 分页查询所有数据
     *
     * @param page 分页对象
     * @param pMessage 查询实体
     * @return 所有数据
     */
    @GetMapping("/selectAll")
    public R selectAll(Page<PMessage> page, PMessage pMessage) {
        return success(this.pMessageService.page(page, new QueryWrapper<>(pMessage)));
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping("/selectOne/{id}")
    public R selectOne(@PathVariable Serializable id) {
        return success(this.pMessageService.getById(id));
    }

    /**
     * 新增数据
     *
     * @param pMessage 实体对象
     * @return 新增结果
     */
    @PostMapping("/insert")
    public R insert(@RequestBody PMessage pMessage) {
        return success(this.pMessageService.save(pMessage));
    }

    /**
     * 修改数据
     *
     * @param pMessage 实体对象
     * @return 修改结果
     */
    @PutMapping("/update")
    public R update(@RequestBody PMessage pMessage) {
        return success(this.pMessageService.updateById(pMessage));
    }

    /**
     * 删除数据
     *
     * @param idList 主键结合
     * @return 删除结果
     */
    @DeleteMapping("/delete")
    public R delete(@RequestParam("idList") List<Long> idList) {
        return success(this.pMessageService.removeByIds(idList));
    }
}

