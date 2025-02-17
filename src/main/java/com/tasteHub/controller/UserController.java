package com.tasteHub.controller;


import cn.hutool.core.bean.BeanUtil;
import com.tasteHub.aspect.ApiOperationLog;
import com.tasteHub.dto.LoginFormDTO;
import com.tasteHub.dto.Result;
import com.tasteHub.dto.UserDTO;
import com.tasteHub.entity.User;
import com.tasteHub.entity.UserInfo;
import com.tasteHub.service.IUserInfoService;
import com.tasteHub.service.IUserService;
import com.tasteHub.utils.UserHolder;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    @ApiOperationLog(description = "发送手机验证码")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        // 发送短信验证码并保存验证码
        return userService.sendCode(phone, session);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    @ApiOperationLog(description = "登录")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){
        // 实现登录功能
        return userService.login(loginForm, session);
    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    @ApiOperationLog(description = "登出")
    public Result logout(){
        // TODO 实现登出功能
        return Result.fail("功能未完成");
    }

    @GetMapping("/me")
    @ApiOperationLog(description = "查询我的信息")
    public Result me(){
        // 获取当前登录的用户并返回
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    @GetMapping("/info/{id}")
    @ApiOperationLog(description = "查询用户信息")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }

    @GetMapping("/{id}")
    @ApiOperationLog(description = "查询用户详情")
    public Result queryUserById(@PathVariable("id") Long userId){
        // 查询详情
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // 返回
        return Result.ok(userDTO);
    }

    @PostMapping("/sign")
    @ApiOperationLog(description = "签到")
    public Result sign(){
        return userService.sign();
    }

    @GetMapping("/sign/count")
    @ApiOperationLog(description = "查询签到次数")
    public Result signCount(){
        return userService.signCount();
    }

    @GetMapping("/generatetokentoredis")
    public Result generateTokenToRedis() {
        userService.generateToken();
        System.out.println("生成token成功");
        return Result.ok();
    }
}