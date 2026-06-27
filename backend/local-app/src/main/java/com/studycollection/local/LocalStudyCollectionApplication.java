package com.studycollection.local;

import com.studycollection.user.UserServiceApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(
        basePackages = "com.studycollection",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = UserServiceApplication.class
        )
)
public class LocalStudyCollectionApplication {
    public static void main(String[] args) {
        SpringApplication.run(LocalStudyCollectionApplication.class, args);
    }
}
