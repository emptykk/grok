package com.graduation.procurement;
import org.mybatis.spring.annotation.MapperScan;
import com.graduation.procurement.config.PublicDeployProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.graduation.procurement.mapper")
@EnableScheduling
@EnableConfigurationProperties(PublicDeployProperties.class)
public class ProcurementApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProcurementApplication.class, args);
    }
}
