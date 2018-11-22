package ftp.scan;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class Configuration {
    private RedissonClient redisson;
    private Map<String, List<String>> fieldToAliases = new HashMap<>();

    public Configuration(RedissonClient redisson, @Value("${column.mapping.key}") String key){
        this.redisson = redisson;
        setFieldToAliases((Map<String, List<String>>) redisson.getBucket(key).get());
    }

    public Map<String, List<String>> getFieldToAliases() {
        return fieldToAliases;
    }

    public void setFieldToAliases(Map<String, List<String>> fieldToAliases) {
        this.fieldToAliases = fieldToAliases;
    }
}
