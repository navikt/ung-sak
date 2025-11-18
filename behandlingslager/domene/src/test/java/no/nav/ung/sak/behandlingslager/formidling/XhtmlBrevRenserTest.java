package no.nav.ung.sak.behandlingslager.formidling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class XhtmlBrevRenserTest {

    @Test
    void skal_gjøre_om_html_breakline_til_xhtml_breakline() {
        assertThat(rens("<br>")).isEqualTo("<br />");
    }

    @Test
    void skal_avslutte_ulukkede_tagger() {
        assertThat(rens("<p>")).isEqualTo("<p></p>");
        assertThat(rens("</p>")).isEqualTo("<p></p>");
        assertThat(rens("0<i>1</p>2</i>3<p>4"))
            .isEqualToIgnoringWhitespace("0 <i>1 <p></p> 2</i> 3 <p>4</p>");
    }

    @Test
    void skal_funke_med_spesialtegn() {
        assertThat(rens("&")).isEqualTo("&amp;");
        assertThat(rens("<")).isEqualTo("&lt;");
        assertThat(rens(">")).isEqualTo("&gt;");
        assertThat(rens("[")).isEqualTo("[");
        assertThat(rens("]")).isEqualTo("]");
        assertThat(rens("&amp;")).isEqualTo("&amp;");
        assertThat(rens("&nbsp;")).isEqualTo("&#xa0;");
        assertThat(rens(" ")).isEqualTo("&#xa0;");

        rens("!\"#¤%/()=?`§\\;:<>*€¨^ +-;,è");
    }

    @ParameterizedTest
    @ValueSource(strings = {"script", "style"})
    void skal_fjerne_innhold_og_tagg_i_ikke_støttede_kjente_html_tagger(String tag) {
        assertThat(rens("<" + tag + ">tekst inni en tagg</" + tag + ">")).isEqualTo("");
        assertThat(rens("<" + tag + ">")).isEqualTo("");
        assertThat(rens("</" + tag + ">")).isEqualTo("");
    }

    @Test
    void skal_fjerne_tagg_men_beholde_innhold_i_ukjente_tagger() {
        String text = "tekst inni en tagg";
        assertThat(rens("<FINNESIKKE>" + text + "</FINNESIKKE>")).isEqualTo(text);
        assertThat(rens("<![CDATA[" + text + "]]>")).isEqualTo(text);
        assertThat(rens("<FINNESIKKE>")).isEqualTo("");
        assertThat(rens("</FINNESIKKE>")).isEqualTo("");
    }

    @Test
    void skal_fjerne_attributter_i_tagger() {
        assertThat(rens("<h1 class='overskrift' onclick='stealCookies()'>overskrift</h1>"))
            .isEqualTo("<h1>overskrift</h1>");
    }


    @Test
    void skal_ikke_fjerne_href_og_title_attributter_for_lenker() {
        assertThat(rens("<a href=\"https://www.secure.com\" title=\"tittel\" rel=\"noreferrer\">www.secure.com</a>"))
            .isEqualTo("<a href=\"https://www.secure.com\" title=\"tittel\">www.secure.com</a>");
    }

    @ParameterizedTest
    @ValueSource(strings = {"h1", "h2", "p"})
    void skal_ikke_fjerne_støttede_tagger(String tag) {
        String text = "tekst inni en tagg";
        assertThat(rens("<" + tag + ">" + text + "</" + tag + ">"))
            .isEqualTo("<" + tag + ">" + text + "</" + tag + ">");
    }

    private static String rens(String input) {
        return XhtmlBrevRenser.rens(input);
    }
}
