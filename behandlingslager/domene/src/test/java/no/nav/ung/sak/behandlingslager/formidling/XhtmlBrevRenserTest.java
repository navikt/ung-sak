package no.nav.ung.sak.behandlingslager.formidling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class XhtmlBrevRenserTest {

    private final XhtmlBrevRenser sanitizer = new XhtmlBrevRenser();

    @Test
    void skal_gjøre_om_html_breakline_til_xhtml_breakline() {
        assertThat(sanitizer.rens("<br>")).isEqualTo("<br />");
    }

    @Test
    void skal_avslutte_ulukkede_tagger() {
        assertThat(sanitizer.rens("<p>")).isEqualTo("<p></p>");
        assertThat(sanitizer.rens("</p>")).isEqualTo("<p></p>");
        assertThat(sanitizer.rens("0<i>1</p>2</i>3<p>4"))
            .isEqualToIgnoringWhitespace("0 <i>1 <p></p> 2</i> 3 <p>4</p>");
    }

    @Test
    void skal_funke_med_spesialtegn() {
        assertThat(sanitizer.rens("&")).isEqualTo("&amp;");
        assertThat(sanitizer.rens("<")).isEqualTo("&lt;");
        assertThat(sanitizer.rens(">")).isEqualTo("&gt;");
        assertThat(sanitizer.rens("[")).isEqualTo("[");
        assertThat(sanitizer.rens("]")).isEqualTo("]");
        assertThat(sanitizer.rens("&amp;")).isEqualTo("&amp;");
        assertThat(sanitizer.rens("&nbsp;")).isEqualTo("&#xa0;");
        assertThat(sanitizer.rens(" ")).isEqualTo("&#xa0;");

        sanitizer.rens("!\"#¤%/()=?`§\\;:<>*€¨^ +-;,è");
    }

    @ParameterizedTest
    @ValueSource(strings = {"script", "style"})
    void skal_fjerne_innhold_og_tagg_i_ikke_støttede_kjente_html_tagger(String tag) {
        assertThat(sanitizer.rens("<" + tag + ">tekst inni en tagg</" + tag + ">")).isEqualTo("");
        assertThat(sanitizer.rens("<" + tag + ">")).isEqualTo("");
        assertThat(sanitizer.rens("</" + tag + ">")).isEqualTo("");
    }

    @Test
    void skal_fjerne_tagg_men_beholde_innhold_i_ukjente_tagger() {
        String text = "tekst inni en tagg";
        assertThat(sanitizer.rens("<FINNESIKKE>" + text + "</FINNESIKKE>")).isEqualTo(text);
        assertThat(sanitizer.rens("<![CDATA[" + text + "]]>")).isEqualTo(text);
        assertThat(sanitizer.rens("<FINNESIKKE>")).isEqualTo("");
        assertThat(sanitizer.rens("</FINNESIKKE>")).isEqualTo("");
    }

    @Test
    void skal_fjerne_attributter_i_tagger() {
        assertThat(sanitizer.rens("<h1 class='overskrift' onclick='stealCookies()'>overskrift</h1>"))
            .isEqualTo("<h1>overskrift</h1>");
        assertThat(sanitizer.rens("<a href=\"https://www.unsecure.com\">www.secure.com</a>"))
            .isEqualTo("<a>www.secure.com</a>");
    }

    @ParameterizedTest
    @ValueSource(strings = {"h1", "h2", "p"})
    void skal_ikke_fjerne_støttede_tagger(String tag) {
        String text = "tekst inni en tagg";
        assertThat(sanitizer.rens("<" + tag + ">" + text + "</" + tag + ">"))
            .isEqualTo("<" + tag + ">" + text + "</" + tag + ">");
    }
}
