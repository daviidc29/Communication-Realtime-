package edu.eci.arsw.calls.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Configuración de Redis para la aplicación.
 */
@Configuration
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
public class RedisConfig {

    @Value("${redis.host:localhost}")
    private String host;

    @Value("${redis.port:6379}")
    private int port;

    /**
     * Configura la fábrica de conexiones de Redis.
     * 
     * @return Fábrica de conexiones de Redis.
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(new RedisStandaloneConfiguration(host, port));
    }

    /**
     * Configura el template de Redis para operaciones con cadenas.
     * 
     * @param cf Fábrica de conexiones de Redis.
     * @return Template de Redis para cadenas.
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory cf) {
        return new StringRedisTemplate(cf);
    }

    /**
     * Configura el contenedor de listeners de mensajes de Redis.
     * 
     * @param cf Fábrica de conexiones de Redis.
     * @return Contenedor de listeners de mensajes de Redis.
     */
    @Bean
    public RedisMessageListenerContainer redisContainer(LettuceConnectionFactory cf) {
        RedisMessageListenerContainer c = new RedisMessageListenerContainer();
        c.setConnectionFactory(cf);
        return c;
    }
}
