package com.tasteHub.controller;


import com.tasteHub.aspect.ApiOperationLog;
import com.tasteHub.dto.Result;
import com.tasteHub.entity.ShopType;
import com.tasteHub.service.IShopTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    @GetMapping("list")
    @ApiOperationLog(description = "查询商铺类型列表")
    public Result queryTypeList() {
        List<ShopType> typeList = typeService
                .query().orderByAsc("sort").list();
        return Result.ok(typeList);
    }
}
