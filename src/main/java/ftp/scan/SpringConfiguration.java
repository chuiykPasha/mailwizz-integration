package ftp.scan;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
        ErrorMvcAutoConfiguration.class
})
public class SpringConfiguration {
    @Bean
    public RedissonClient redissonClient(@Value("${redis.connection}") String connection){
        Config config = new Config();
        config.useSingleServer()
                .setAddress(connection);
        return Redisson.create(config);
    }
}
