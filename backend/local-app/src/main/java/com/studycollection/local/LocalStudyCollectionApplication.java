package com.studycollection.local;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.studycollection.user.UserServiceApplication;
import com.studycollection.user.auth.MyBatisUserRepository;
import com.studycollection.user.auth.UserMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication(
        exclude = {
                DataSourceAutoConfiguration.class,
                MybatisPlusAutoConfiguration.class
        }
)
@ComponentScan(
        basePackages = "com.studycollection",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        UserServiceApplication.class,
                        MyBatisUserRepository.class,
                        UserMapper.class
                }
        )
)
public class LocalStudyCollectionApplication {
    public static void main(String[] args) {
        SpringApplication.run(LocalStudyCollectionApplication.class, args);
    }
}
