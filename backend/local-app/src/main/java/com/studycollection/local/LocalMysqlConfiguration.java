package com.studycollection.local;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local-mysql")
@MapperScan("com.studycollection.user.auth")
public class LocalMysqlConfiguration {
}
