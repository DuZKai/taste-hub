package com.tasteHub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tasteHub.dto.LoginFormDTO;
import com.tasteHub.dto.Result;
import com.tasteHub.entity.User;
import jakarta.servlet.http.HttpSession;


public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);

    Result sign();

    Result signCount();

}
