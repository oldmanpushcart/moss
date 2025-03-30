package io.github.oldmanpushcart.moss;

import io.github.oldmanpushcart.moss.infra.commonmark.extension.VideoLinkHtmlExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class Demo {

    private static final Parser markdownParser = Parser.builder()
            .extensions(List.of(
                    TablesExtension.create()
            ))
            .build();
    private static final HtmlRenderer htmlRenderer = HtmlRenderer.builder()
            .extensions(List.of(
                    TablesExtension.create(),
                    VideoLinkHtmlExtension.create()
            ))
            .build();

    public static void main(String... args) {

        final var location = Path.of("./data/downloads");
        final var now = LocalDateTime.now();
        final var target = location
                .resolve(String.format("%02d", now.getHour()))
                .resolve(String.format("%02d", now.getMinute()))
                .resolve("test.txt");
        System.out.println(URI.create(target.toFile().toString()));

    }

}
