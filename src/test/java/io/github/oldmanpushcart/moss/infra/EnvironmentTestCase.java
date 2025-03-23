package io.github.oldmanpushcart.moss.infra;

import io.github.oldmanpushcart.moss.SpringBootSupport;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

@AllArgsConstructor(onConstructor_ = @Autowired)
public class EnvironmentTestCase extends SpringBootSupport {

    private final Environment environment;

    @Test
    public void test() {
        String osName = environment.getProperty("os.name");
        String osArch = environment.getProperty("os.arch");

        System.out.println("Operating System: " + osName);
        System.out.println("CPU Architecture: " + osArch);
    }

}
