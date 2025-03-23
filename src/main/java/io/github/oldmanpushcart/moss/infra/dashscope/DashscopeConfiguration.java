package io.github.oldmanpushcart.moss.infra.dashscope;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.github.oldmanpushcart.moss.util.CommonUtils.acceptIfNonNull;

@Configuration
public class DashscopeConfiguration {

    @Bean
    public DashscopeClient dashscope(DashscopeConfig config) {
        return DashscopeClient.newBuilder()
                .ak(config.getApiKey())
                .customizeOkHttpClient(okHttpBuilder -> {
                    if (null != config.getHttp()) {
                        final var httpConfig = config.getHttp();
                        acceptIfNonNull(httpConfig.getConnectTimeout(), okHttpBuilder::connectTimeout);
                        acceptIfNonNull(httpConfig.getReadTimeout(), okHttpBuilder::readTimeout);
                        acceptIfNonNull(httpConfig.getWriteTimeout(), okHttpBuilder::writeTimeout);
                        acceptIfNonNull(httpConfig.getPingInterval(), okHttpBuilder::pingInterval);
                    }
                })
                .build();
    }

}
