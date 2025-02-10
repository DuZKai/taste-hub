package com.tasteHub.controller;


import com.tasteHub.aspect.ApiOperationLog;
import com.tasteHub.dto.Result;
import com.tasteHub.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private IFollowService followService;

    @PutMapping("/{id}/{isFollow}")
    @ApiOperationLog(description = "关注/取消关注")
    public Result follow(@PathVariable("id") Long followUserId, @PathVariable("isFollow") Boolean isFollow) {
        return followService.follow(followUserId, isFollow);
    }

    @GetMapping("/or/not/{id}")
    @ApiOperationLog(description = "是否关注")
    public Result isFollow(@PathVariable("id") Long followUserId) {
        return followService.isFollow(followUserId);
    }

    @GetMapping("/common/{id}")
    @ApiOperationLog(description = "关注公共")
    public Result followCommons(@PathVariable("id") Long id){
        return followService.followCommons(id);
    }
}
