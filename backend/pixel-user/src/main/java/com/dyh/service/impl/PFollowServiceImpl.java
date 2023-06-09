package com.dyh.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dyh.dao.PFollowDao;
import com.dyh.dao.PUserDao;
import com.dyh.entity.PFollow;
import com.dyh.entity.PPost;
import com.dyh.entity.PUser;
import com.dyh.entity.dto.PUserDTO;
import com.dyh.entity.dto.ScrollResult;
import com.dyh.feign.PPostFeignService;
import com.dyh.service.PFollowService;
import com.dyh.service.PUserService;
import com.dyh.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.*;
import java.util.stream.Collectors;

import static com.dyh.constant.RedisConstants.FEED_KEY;
import static com.dyh.constant.RedisConstants.FOLLOW_KEY;

/**
 * 关注(PFollow)表服务实现类
 *
 * @author makejava
 * @since 2023-02-25 22:40:17
 */
@Service("pFollowService")
public class PFollowServiceImpl extends ServiceImpl<PFollowDao, PFollow> implements PFollowService {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    PUserService userService;

    @Resource
    PPostFeignService pPostFeignService;

    /**
     * 关注service
     *
     * @param followUserId 遵循用户id
     * @param isFollow     是遵循
     * @return {@link R}
     */
    @Override
    public R follow(Long followUserId, Boolean isFollow) {
        // 1.获取登录用户
        Long userId = UserHolder.getUser().getId();
        String key = FOLLOW_KEY + userId;
        // 1.判断到底是关注还是取关
        if (isFollow) {
            // 2.关注，新增数据
            PFollow follow = new PFollow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean isSuccess = save(follow);
            if (isSuccess) {
                // 把关注用户的id，放入redis的set集合 sadd userId followerUserId
                stringRedisTemplate.opsForSet().add(key, followUserId.toString());
            }
        } else {
            // 3.取关，删除 delete from tb_follow where user_id = ? and follow_user_id = ?
            boolean isSuccess = remove(new QueryWrapper<PFollow>()
                    .eq("user_id", userId).eq("follow_user_id", followUserId));
            if (isSuccess) {
                // 把关注用户的id从Redis集合中移除
                stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
                return R.ok("取关成功");
            }else{
                return R.failed("出错");
            }
        }
        return R.ok("关注成功");
    }
    /**
     * 查看是否关注
     *
     * @param followUserId 遵循用户id
     * @return {@link R}
     */
    @Override
    public R isFollow (Long followUserId){
        // 1.获取登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.查询是否关注 select count(*) from tb_follow where user_id = ? and follow_user_id = ?
        Integer count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
        // 3.判断
        return R.ok(count > 0);
    }

    @Override
    public R followCommons(Long id) {
        // 1.获取当前用户
        Long userId = UserHolder.getUser().getId();
        String key = FOLLOW_KEY + userId;
        // 2.求交集
        String key2 = FOLLOW_KEY + id;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key, key2);
        if (intersect == null || intersect.isEmpty()) {
            // 无交集
            return R.ok(Collections.emptyList());
        }
        // 3.解析id集合
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        // 4.查询用户
        List<PUserDTO> users = userService.listByIds(ids)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, PUserDTO.class))
                .collect(Collectors.toList());
        return R.ok(users);
    }

    @Override
    public R<List<PFollow>> queryFansById(Long id) {
        List<PFollow> follows = query().eq("follow_user_id", id).list();
        if(follows==null){
            return R.failed("查询出错");
        }
        return R.ok(follows);
    }

    /**
     * 查询关注博主的所有Feed推文
     * @param max
     * @param offset
     * @return {@link R}
     */
    @Override
    public R queryPostOfFollow(Long max, Integer offset) {
        // 1.获取当前用户
        Long userId = UserHolder.getUser().getId();
        // 2.查询收件箱 ZREVRANGEBYSCORE key Max Min LIMIT offset count
        String key = FEED_KEY + userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 2);
        // 3.非空判断
        if (typedTuples == null || typedTuples.isEmpty()) {
            return R.ok("无新推博文");
        }
        // 4.解析数据：blogId、minTime（时间戳）、offset
        List<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime = 0; // 2
        int os = 1; // 2
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) { // 5 4 4 2 2
            // 4.1.获取id
            ids.add(Long.valueOf(Objects.requireNonNull(tuple.getValue())));
            // 4.2.获取分数(时间戳）
            long time = Objects.requireNonNull(tuple.getScore()).longValue();
            if(time == minTime){
                os++;
            }else{
                minTime = time;
                os = 1;
            }
        }

        // 5.根据id查询blog
        List<PPost> posts = pPostFeignService.selectBatch(ids).getData();

        // 6.封装并返回
        ScrollResult r = new ScrollResult();
        r.setList(posts);
        r.setOffset(os);
        r.setMinTime(minTime);

        return R.ok(r);
    }
}

