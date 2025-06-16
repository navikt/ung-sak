package no.nav.ung.sak.formidling.informasjonsbrev.innhold;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public final class MarkdownParser {
    private static final Parser parser = Parser.builder().build();

    private static final HtmlRenderer htmlRenderer = HtmlRenderer.builder()
        .escapeHtml(true)
        .softbreak("<br />")
        .build();

    public static String markdownTilHtml(String input) {
        var node = parser.parse(input);
        return htmlRenderer.render(node);
    }

}
