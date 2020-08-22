package cn.springcloud.gray.web.configuration;

import cn.springcloud.gray.client.config.properties.GrayTrackProperties;
import cn.springcloud.gray.client.netflix.hystrix.HystrixLocalStorageCycle;
import cn.springcloud.gray.client.netflix.hystrix.HystrixRequestLocalStorage;
import cn.springcloud.gray.mock.MockManager;
import cn.springcloud.gray.request.LocalStorageLifeCycle;
import cn.springcloud.gray.request.RequestLocalStorage;
import cn.springcloud.gray.request.track.GrayTrackHolder;
import cn.springcloud.gray.web.filter.GrayMockWebFilter;
import cn.springcloud.gray.web.filter.GrayTrackWebFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@ConditionalOnBean(GrayTrackHolder.class)
@ConditionalOnProperty(value = "gray.client.runenv", havingValue = "web", matchIfMissing = true)
public class GrayTrackWebMvcConfiguration {

    @Autowired
    private GrayTrackProperties grayTrackProperties;

    @Bean
    @ConditionalOnMissingBean
    @Order(999)
    public GrayTrackWebFilter grayTrackFilter(
            GrayTrackHolder grayTrackHolder, RequestLocalStorage requestLocalStorage) {
        return new GrayTrackWebFilter(requestLocalStorage, grayTrackHolder);
    }

    @Bean
    @ConditionalOnProperty(value = "gray.mock.enabled", havingValue = "true")
    @Order(1000)
    public GrayMockWebFilter grayMockWebFilter(
            MockManager mockManager, RequestLocalStorage requestLocalStorage) {
        return new GrayMockWebFilter(requestLocalStorage, mockManager);
    }


    //    @Configuration
    @ConditionalOnProperty(value = {"gray.hystrix.threadTransmitStrategy"}, havingValue = "HYSTRIX_REQUEST_LOCAL_STORAGE")
//    @Import(HystrixGrayTrackWebConfiguration.class)
    public static class HystrixRequestLocalStorageConfiguration {
        @Bean
        public RequestLocalStorage requestLocalStorage(LocalStorageLifeCycle localStorageLifeCycle) {
            return new HystrixRequestLocalStorage(localStorageLifeCycle);
        }

        @Bean
        public LocalStorageLifeCycle localStorageLifeCycle() {
            return new HystrixLocalStorageCycle();
        }
    }

}
