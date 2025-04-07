package io.github.oldmanpushcart.moss.backend.extra.caldav;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import okhttp3.*;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CaldavConfiguration {

    @Bean(name = "caldavHttp")
    public OkHttpClient caldavHttp(CaldavConfig config) {
        final var httpConfig = config.getHttp();
        return new OkHttpClient.Builder()
                .authenticator(new Authenticator() {

                    @Nullable
                    @Override
                    public Request authenticate(@Nullable Route route, @Nonnull Response response) {

                        // 如果请求已经包含了授权头信息，则不再尝试重新认证
                        if (response.request().header("Authorization") != null) {
                            return null;
                        }

                        // 创建授权头信息
                        final var credential = Credentials.basic(config.getUsername(), config.getPassword());
                        return response.request().newBuilder()
                                .header("Authorization", credential)
                                .build();
                    }

                })
                .connectTimeout(httpConfig.getConnectTimeout())
                .readTimeout(httpConfig.getReadTimeout())
                .writeTimeout(httpConfig.getWriteTimeout())
                .build();
    }

    @Bean
    public DisposableBean shutdownCaldavHttp(

            @Autowired
            @Qualifier("caldavHttp")
            OkHttpClient caldavHttp

    ) {
        return ()-> {
            caldavHttp.dispatcher().executorService().shutdown();
            caldavHttp.connectionPool().evictAll();
        };
    }

}
