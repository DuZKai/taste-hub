package com.tasteHub.service.impl;

import com.tasteHub.entity.UserInfo;
import com.tasteHub.mapper.UserInfoMapper;
import com.tasteHub.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}
