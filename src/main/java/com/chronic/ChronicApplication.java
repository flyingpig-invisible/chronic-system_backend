package com.chronic;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 慢病管理系统 启动类
 *
 * @author chronic
 */
@EnableScheduling
@MapperScan("com.chronic.**.mapper")
@SpringBootApplication
public class ChronicApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChronicApplication.class, args);
        System.out.println("慢病管理系统后端启动成功");
        System.out.println("接口文档: http://localhost:8080/doc.html");
    }
}
