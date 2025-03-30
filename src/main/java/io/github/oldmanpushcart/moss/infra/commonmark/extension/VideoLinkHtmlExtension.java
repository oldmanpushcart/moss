package io.github.oldmanpushcart.moss.infra.commonmark.extension;


import org.commonmark.Extension;
import org.commonmark.node.Image;
import org.commonmark.node.Link;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.CoreHtmlNodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlNodeRendererFactory;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.HashMap;
import java.util.Map;
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
                        if (VIDEO_PATTERN.matcher(link.getDestination()).matches()) {
                            visitVideo(link.getDestination());
                        } else {
                            super.visit(link);
                        }
                    }

                    @Override
                    public void visit(Image image) {
                        if (VIDEO_PATTERN.matcher(image.getDestination()).matches()) {
                            visitVideo(image.getDestination());
                        } else {
                            super.visit(image);
                        }
                    }

                    private void visitVideo(String destination) {
                        final var html = context.getWriter();
                        html.tag("video", new HashMap<>() {{
                            put("controls", null);
                            put("style", "width: 100%; height: auto;");
                        }});
                        html.tag("source", Map.of("src", destination));
                        html.text("Your browser does not support the video tag.");
                        html.tag("/video");
                    }

                };
            }
        });
    }

    public static Extension create() {
        return new VideoLinkHtmlExtension();
    }

}
