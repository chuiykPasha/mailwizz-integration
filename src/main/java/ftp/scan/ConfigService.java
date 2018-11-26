package ftp.scan;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ConfigService {
    @Autowired
    private RedissonClient redisson;
    @Value("${column.mapping.key}")
    private String columnMappingKey;

    public Map<String, List<String>> getHeaders(){
        return (Map<String, List<String>>) redisson.getBucket(columnMappingKey).get();
    }

    public void saveHeaders(Map<String, List<String>> headers){
        RBucket<Map<String, List<String>>> bucket = redisson.getBucket(columnMappingKey);
        bucket.set(headers);
    }
}
