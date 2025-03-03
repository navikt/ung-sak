package no.nav.ung.sak.formidling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

/**
 * Gjør det enklere å assert tekster i html. Fjerner urelevante tagger og linjeskift
 */
public class HtmlAssert extends AbstractAssert<HtmlAssert, String> {

    private final String actual;
    private final String actualHtmlTrimmed;
    private final String actualTextTrimmed;

    public HtmlAssert(String actual) {
        super(actual, HtmlAssert.class);
        this.actual = actual;

        // Use Jsoup to parse and clean the HTML
        Document document = Jsoup.parse(actual);

        // Remove undesired elements
        document.select("style, head, img").remove();
        removeComments(document);

        // Extract cleaned HTML as a string
        actualHtmlTrimmed = document.body().html().replaceAll("(?m)^\\s*$\\n", ""); // Remove empty lines
        actualTextTrimmed = Jsoup.parse(actual).text().replaceAll("(?m)^\\s*$\\n", "");
    }

    public static HtmlAssert assertThatHtml(String actual) {
        return new HtmlAssert(actual);
    }

    public HtmlAssert containsText(String text) {
        assertThat(actualTextTrimmed).contains(text);
        return this;
    }

    public HtmlAssert containsTextsOnceInSequence(String... text) {
        assertThatContainsOnceInSequence(actualTextTrimmed, text);
        return this;
    }

    /**
     * Sjekker om alle text er setninger med punktum. Legger også på space etter siste punktum for å fange f.eks.
     * feilaktig dobbel punktum
     */
    public HtmlAssert containsSentencesOnceInSequence(String... text) {
        List<String> list = new ArrayList<>();
        for (var linje : text) {
            if (!linje.endsWith(".")) {
                throw new IllegalArgumentException("Alle setninger må slutte med punktum. Setningen: \"" + linje + "\" mangler punktum.");
            }
            String medSpace = linje + " ";
            list.add(medSpace);
        }
        containsTextsOnceInSequence(list.toArray(new String[0]));
        return this;
    }

    public HtmlAssert containsHtmlOnceInSequence(CharSequence... html) {
        assertThatContainsOnceInSequence(actualHtmlTrimmed, html);
        return this;
    }

    public HtmlAssert doesNotContainText(CharSequence... texts) {
        assertThat(actualTextTrimmed).doesNotContain(texts);
        return this;
    }

    public HtmlAssert doesNotContainHtml(CharSequence... texts) {
        assertThat(actualHtmlTrimmed).doesNotContain(texts);
        return this;
    }

    private void assertThatContainsOnceInSequence(String actual, CharSequence... seq) {
        assertThat(actual).containsSubsequence(seq);
        var soft = new SoftAssertions();
        Arrays.stream(seq).forEach(it ->
            soft.assertThat(actual).containsOnlyOnce(it)
        );
        soft.assertAll();
    }

    private void removeComments(Document document) {
        document.traverse(new NodeVisitor() {

            @Override
            public void head(@NotNull Node node, int depth) {
                if (node.nodeName().equals("#comment")) {
                    node.remove();
                }
            }

            @Override
            public void tail(@NotNull Node node, int depth) {
                // Do nothing on tail visit
            }
        });
    }
}
