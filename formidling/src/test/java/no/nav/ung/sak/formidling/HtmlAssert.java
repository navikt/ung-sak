package no.nav.ung.sak.formidling;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.assertj.core.api.AbstractAssert;

public class HtmlAssert extends AbstractAssert<HtmlAssert, String> {

    public HtmlAssert(String actual) {
        super(actual, HtmlAssert.class);
    }

    public static HtmlAssert assertThatHtml(String actual) {
        return new HtmlAssert(actual);
    }

    public void contains(String expectedSubstring) {
        String processedHtml = actual
            .replaceAll("(?s)<style.*?>.*?</style>", "")
            .replaceAll("(?s)<head.*?>.*?</head>", "")
            .replaceAll("(?s)<!--.*?-->", "")
            .replaceAll("(?s)<img.*?/>", "");

        if (!processedHtml.contains(expectedSubstring)) {
            failWithMessage("Expected HTML to contain <%s> but did not. Simplified HTML:\n%s", expectedSubstring, processedHtml);
        }
    }
}
