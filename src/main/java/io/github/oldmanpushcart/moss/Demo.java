package io.github.oldmanpushcart.moss;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class Demo {

    public static void main(String... args) {

        final var file = new File("C:\\Users\\vlinux\\OneDrive\\文档\\个人简历-李夏驰-2024.1.pdf");
        final var encoded = file.toURI().toString();
        System.out.println(encoded);

        final var decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8);
        System.out.println(decoded);

    }

}
