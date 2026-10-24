package com.shim369.reciperev.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 結合テスト用に本物の MySQL 8.4 を Docker で起動する。
 * {@code @ServiceConnection} が付いていると、spring.datasource.url などをコンテナの接続先に自動で書き換えてくれる。
 * H2 ではなく MySQL を使うのは、生成列・CHECK制約・FOR UPDATE・{@code <=>} など MySQL 固有の動作を確かめたいため。
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
                .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_0900_ai_ci");
    }
}
