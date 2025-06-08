package org.zhuzhu_charging_station_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.zhuzhu_charging_station_backend.entity.ChargingStationSlot;
import org.zhuzhu_charging_station_backend.entity.Order;

@Configuration
@EnableCaching
public class RedisConfig {
    public ObjectMapper redisObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("org.zhuzhu_charging_station_backend")
                        .allowIfSubType("java.util")                           // 允许反序列化各类List/Map
                        .allowIfSubType("java.lang")                           // 允许最常用JDK类型
                        .allowIfSubType("java.math")
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        return om;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(Object.class);

        jackson2JsonRedisSerializer.setObjectMapper(redisObjectMapper());

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .build();
    }

    @Bean
    public RedisTemplate<String, ChargingStationSlot> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, ChargingStationSlot> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 设置key序列化
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // JSON序列化器
        Jackson2JsonRedisSerializer<ChargingStationSlot> serializer =
                new Jackson2JsonRedisSerializer<>(ChargingStationSlot.class);
        serializer.setObjectMapper(redisObjectMapper());

        // 设置value序列化方式
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisTemplate<String, Order> orderRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Order> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 设置 key 序列化
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // JSON序列化器
        Jackson2JsonRedisSerializer<Order> serializer =
                new Jackson2JsonRedisSerializer<>(Order.class);
        serializer.setObjectMapper(redisObjectMapper());

        // 设置value序列化方式
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
}