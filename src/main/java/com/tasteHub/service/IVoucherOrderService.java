package com.tasteHub.service;

import com.tasteHub.dto.Result;
import com.tasteHub.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;


public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);

    Result createVoucherOrder(Long voucherId);
    // void createVoucherOrder(VoucherOrder voucherId);

}
