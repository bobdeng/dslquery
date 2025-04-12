package cn.beagile.dslquery;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;

@Configuration
public class TestDataSourceConfig {

    @Bean
    public DataSource dataSource() {
        DockerImageName dockerImageName = DockerImageName.parse("mysql:8.0.32");
        MySQLContainer mySQLContainer = new MySQLContainer(dockerImageName);
        mySQLContainer.withReuse(false).start();
        return DataSourceBuilder.create()
                .driverClassName(mySQLContainer.getDriverClassName())
                .username(mySQLContainer.getUsername())
                .password(mySQLContainer.getPassword())
                .url(mySQLContainer.getJdbcUrl() + "?characterEncoding=utf-8")
                .build();
    }

}
