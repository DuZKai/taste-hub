package com.tasteHub.service.impl;

import com.tasteHub.entity.BlogComments;
import com.tasteHub.mapper.BlogCommentsMapper;
import com.tasteHub.service.IBlogCommentsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

}
