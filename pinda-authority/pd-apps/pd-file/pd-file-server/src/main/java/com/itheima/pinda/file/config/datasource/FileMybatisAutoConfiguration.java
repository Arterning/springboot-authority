package com.itheima.pinda.file.config.datasource;


//import com.itheima.pinda.authority.api.UserApi;

import com.itheima.pinda.database.datasource.BaseMybatisConfiguration;
import com.itheima.pinda.database.properties.DatabaseProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

//import com.itheima.pinda.database.mybatis.auth.DataScopeInterceptor;

/**
 * 配置一些拦截器
 *
 */
@Configuration
@Slf4j
public class FileMybatisAutoConfiguration extends BaseMybatisConfiguration {
    public FileMybatisAutoConfiguration(DatabaseProperties databaseProperties) {
        super(databaseProperties);

    }
}
