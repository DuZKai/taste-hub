package com.tasteHub.service.impl;

import com.tasteHub.entity.SeckillVoucher;
import com.tasteHub.mapper.SeckillVoucherMapper;
import com.tasteHub.service.ISeckillVoucherService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 秒杀优惠券表，与优惠券是一对一关系 服务实现类
 */
@Service
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher> implements ISeckillVoucherService {

}
