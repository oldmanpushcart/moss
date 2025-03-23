package io.github.oldmanpushcart.moss.infra.ibatis;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

public class PreDatabaseInitializationListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(@NotNull ApplicationEnvironmentPreparedEvent event) {
        System.out.println("==========================");
    }

    @Override
    public boolean supportsAsyncExecution() {
        return false;
    }

}
