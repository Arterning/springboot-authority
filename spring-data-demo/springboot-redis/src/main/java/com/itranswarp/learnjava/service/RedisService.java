package com.itranswarp.learnjava.service;

import java.time.Duration;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.support.ConnectionPoolSupport;

@Component
public class RedisService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    RedisClient redisClient;

    private GenericObjectPool<StatefulRedisConnection<String, String>> redisConnectionPool;

    @PostConstruct
    public void init() {
        GenericObjectPoolConfig<StatefulRedisConnection<String, String>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(5);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        this.redisConnectionPool = ConnectionPoolSupport.
        createGenericObjectPool(() -> redisClient.connect(), poolConfig);
    }

    @PreDestroy
    public void shutdown() {
        this.redisConnectionPool.close();
        this.redisClient.shutdown();
    }

    /**
     * 通过调用connection.sync()来获取同步命令接口，最后可以使用命令接口执行Redis命令
     * @param <T>
     * @param callback
     * @return
     */
    public <T> T executeSync(SyncCommandCallback<T> callback) {
        try (StatefulRedisConnection<String, String> connection = redisConnectionPool.borrowObject()) {
            connection.setAutoFlushCommands(true);
            RedisCommands<String, String> commands = connection.sync();
            return callback.doInConnection(commands);
        } catch (Exception e) {
            logger.warn("executeSync redis failed.", e);
            throw new RuntimeException(e);
        }
    }

    public String set(String key, String value) {
        return executeSync(commands -> commands.set(key, value));
    }

    public String set(String key, String value, Duration timeout) {
        return executeSync(commands -> commands.setex(key, timeout.toSeconds(), value));
    }

    public String get(String key) {
        return executeSync(commands -> commands.get(key));
    }

    public boolean hset(String key, String field, String value) {
        return executeSync(commands -> commands.hset(key, field, value));
    }

    public String hget(String key, String field) {
        return executeSync(commands -> commands.hget(key, field));
    }

    public Map<String, String> hgetall(String key) {
        return executeSync(commands -> commands.hgetall(key));
    }
}
