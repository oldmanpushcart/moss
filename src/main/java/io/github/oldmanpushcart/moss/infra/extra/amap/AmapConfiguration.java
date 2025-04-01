package io.github.oldmanpushcart.moss.infra.extra.amap;

import okhttp3.OkHttpClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmapConfiguration {

    @Bean(name = "amapHttp")
    public OkHttpClient amapHttp(AmapConfig config) {
        final var httpConfig = config.getHttp();
        return new OkHttpClient.Builder()
                .connectTimeout(httpConfig.getConnectTimeout())
                .readTimeout(httpConfig.getReadTimeout())
                .writeTimeout(httpConfig.getWriteTimeout())
                .build();
    }

    @Bean
    public DisposableBean shutdownAmapHttp(

            @Autowired
            @Qualifier("amapHttp")
            OkHttpClient amapHttp

    ) {
        return ()-> {
            amapHttp.dispatcher().executorService().shutdown();
            amapHttp.connectionPool().evictAll();
        };
    }

}
