package com.tasteHub;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@MapperScan("com.tasteHub.mapper")
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)

public class TasteHubApplication {

    public static void main(String[] args) {
        System.setProperty("zookeeper.sasl.client", "false");
        SpringApplication.run(TasteHubApplication.class, args);
    }

}
