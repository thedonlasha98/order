package ge.croco.order.config;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableCaching
@Configuration
public class CacheConfig {

    public static final String ORDER_CACHE = "orders";

    @Bean
    public CacheManager cacheManager() {
        return new HazelcastCacheManager(hazelcastInstance());
    }

    @Bean
    public HazelcastInstance hazelcastInstance() {
        Config config = new Config();

        config.addMapConfig(new MapConfig()
                .setName(ORDER_CACHE)
                .setTimeToLiveSeconds(1800)); // 30 minute TTL

        return Hazelcast.newHazelcastInstance(config);
    }
}