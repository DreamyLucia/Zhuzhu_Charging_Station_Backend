package org.zhuzhu_charging_station_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.zhuzhu_charging_station_backend.entity.ChargingStationSlot;

@Configuration
public class RedisConfig {
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
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(om.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);
        serializer.setObjectMapper(om);

        // 设置value序列化方式
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
}