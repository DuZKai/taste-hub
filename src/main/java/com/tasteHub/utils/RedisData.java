package com.tasteHub.utils;

import lombok.Data;

import java.time.LocalDateTime;

// 逻辑过期使用的数据结构
@Data
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;
}
