package no.nav.ung.sak.formidling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.SoftAssertions;

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

        actualHtmlTrimmed = BrevUtils.trimmedHtml(actual);
        actualTextTrimmed = BrevUtils.htmlToPlainText(actual);
    }

    public static HtmlAssert assertThatHtml(String actual) {
        return new HtmlAssert(actual);
    }

    public HtmlAssert asPlainTextIsEqualTo(String text) {
        assertThat(actualTextTrimmed).isEqualTo(text);
        return this;
    }


    public HtmlAssert containsHtmlSubSequenceOnce(String... html) {
        assertThatContainsSubSequenceOnce(actualHtmlTrimmed, html);
        return this;
    }

    /**
     * Sjekker at teksten kommer etterhverandre i rekkefølge én gang. Kan ha annen tekst i mellom.
     */
    private void assertThatContainsSubSequenceOnce(String actual, String... seq) {
        assertThat(actual).containsSubsequence(seq);
        assertThatOnlyOneOccurance(actual, seq);
    }

    private static void assertThatOnlyOneOccurance(String actual, String[] seq) {
        var soft = new SoftAssertions();
        Arrays.stream(seq).forEach(it ->
            soft.assertThat(actual).containsOnlyOnce(it)
        );
        soft.assertAll();
    }

}
