package com.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.ManagementCenterConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.core.Hazelcast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.session.hazelcast.config.annotation.web.http.EnableHazelcastHttpSession;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Objects;

/**
 * Created by gz on 2017/10/18
 */
@EnableCaching
@RestController
@EnableHazelcastHttpSession
@SpringBootApplication
public class StartUp {

    private Logger LOGGER = LoggerFactory.getLogger(StartUp.class);

    public static void main(String[] args) {
        SpringApplication.run(StartUp.class, args);
    }

    @Bean
    public Config hazelCastConfig() {
        //如果有集群管理中心，可以配置
        ManagementCenterConfig centerConfig = new ManagementCenterConfig();
        centerConfig.setUrl("http://172.16.135.165:8200/mancenter");
        centerConfig.setEnabled(true);
        return new Config()
                .setInstanceName("hazelcast-instance")
                .setManagementCenterConfig(centerConfig)
                .addMapConfig(
                        new MapConfig()
                                .setName("instruments")
                                .setMaxSizeConfig(new MaxSizeConfig(200, MaxSizeConfig.MaxSizePolicy.FREE_HEAP_SIZE))
                                .setEvictionPolicy(EvictionPolicy.LRU)
                                .setTimeToLiveSeconds(20000));
    }


    @GetMapping("/greet")
    public Object greet() {
        Object value = Hazelcast.getHazelcastInstanceByName("hazelcast-instance").getMap("instruments").get("hello");
        if (Objects.isNull(value)) {
            Hazelcast.getHazelcastInstanceByName("hazelcast-instance").getMap("instruments").put("hello", "world!");

        }
        LOGGER.info("从分布式缓存获取到 key=hello,value={}", value);
        return value;
    }

    @Autowired
    private DemoService demoService;

    @GetMapping("/cache")
    public Object cache() {
        String value = demoService.greet("hello");
        LOGGER.info("从分布式缓存获取到 key=hello,value={}", value);
        return value;
    }

    @GetMapping("/session")
    public Object session(HttpSession session) {
        String sessionId = session.getId();
        LOGGER.info("当前请求的sessionId={}", sessionId);
        return sessionId;
    }
}

@Service
@CacheConfig(cacheNames = "instruments")
class DemoService {

    private Logger LOGGER = LoggerFactory.getLogger(DemoService.class);

    @Cacheable(key = "#key")
    public String greet(String key) {
        LOGGER.info("缓存内没有取到key={}", key);
        return "world！";
    }
}

