package io.github.oldmanpushcart.moss.frontend.commonmark.extension;


import org.commonmark.Extension;
import org.commonmark.node.Image;
import org.commonmark.node.Link;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.CoreHtmlNodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlNodeRendererFactory;
import org.commonmark.renderer.html.HtmlRenderer;

import java.net.URI;
import java.util.HashMap;
import java.util.regex.Pattern;

public class VideoLinkHtmlExtension implements HtmlRenderer.HtmlRendererExtension {

    private static final Pattern VIDEO_PATTERN = Pattern.compile(".*\\.(mp4|webm|ogg)$");

    @Override
    public void extend(HtmlRenderer.Builder rendererBuilder) {
        rendererBuilder.nodeRendererFactory(new HtmlNodeRendererFactory() {
            @Override
            public NodeRenderer create(HtmlNodeRendererContext context) {
                return new CoreHtmlNodeRenderer(context) {

                    @Override
                    public void visit(Link link) {
                        final var resource = URI.create(link.getDestination());
                        if (VIDEO_PATTERN.matcher(resource.getPath()).matches()) {
                            visitVideo(resource);
                        } else {
                            super.visit(link);
                        }
                    }

                    @Override
                    public void visit(Image image) {
                        final var resource = URI.create(image.getDestination());
                        if (VIDEO_PATTERN.matcher(resource.getPath()).matches()) {
                            visitVideo(resource);
                        } else {
                            super.visit(image);
                        }
                    }

                    private void visitVideo(URI resource) {
                        final var html = context.getWriter();
                        html.tag("video", new HashMap<>() {{
                            put("controls", null);
                            put("style", "width: 100%; height: auto;");
                        }});
                        html.tag("source", new HashMap<>() {{
                            put("src", resource.toString());
                            final var type = parseType(resource);
                            if (null != type) {
                                put("type", "video/mp4");
                            }
                        }});
                        html.text("Your browser does not support the video tag.");
                        html.tag("/video");
                    }

                    // 解析视频类型
                    private String parseType(URI resource) {
                        final var suffix = resource.getPath()
                                .substring(resource.getPath().lastIndexOf(".") + 1)
                                .toLowerCase();
                        return switch (suffix) {
                            case "mp4" -> "video/mp4";
                            case "webm" -> "video/webm";
                            case "ogg" -> "video/ogg";
                            default -> null;
                        };
                    }

                };
            }
        });
    }

    public static Extension create() {
        return new VideoLinkHtmlExtension();
    }

}
