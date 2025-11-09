package no.nav.ung.sak.formidling.vedtak;

import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.editor.VedtaksbrevSeksjon;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.editor.VedtaksbrevSeksjonType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BrevXhtmlTilSeksjonKonverterTest {

    @Test
    void skal_dele_opp_html_i_fire_seksjoner() {
        String html = """
            <html lang="">
            <head>
                <style>
                    body { font-family: Arial; }
                    p { margin: 10px; }
                </style>
            </head>
            <body>
                <header>
                    <div class="uten-mellomrom">
                        <p>Til: Ung Testesen</p>
                        <p>Fødselsnummer: 01017000299</p>
                    </div>
                </header>
                <section>
                    <div data-editable="true">
                        <h1>Redigerbar overskrift</h1>
                        <p>Dette kan redigeres</p>
                    </div>
                    <h2>Du kan klage</h2>
                    <p>Klagefrist</p>
                    <p>Med vennlig Hilsen</p>
                    <p>Nav</p>
                </section>
            </body>
            </html>
            """;
        // Arrange

        // Act
        List<VedtaksbrevSeksjon> seksjoner = BrevXhtmlTilSeksjonKonverter.konverter(html);

        // Assert
        assertThat(seksjoner).hasSize(4);

        // Del 1: Style
        assertThat(seksjoner.get(0).type()).isEqualTo(VedtaksbrevSeksjonType.STYLE);
        assertThat(seksjoner.get(0).innhold()).contains("<style>");
        assertThat(seksjoner.get(0).innhold()).contains("body { font-family: Arial; }");

        // Del 2: Statisk før editable
        assertThat(seksjoner.get(1).type()).isEqualTo(VedtaksbrevSeksjonType.STATISK);
        assertThat(seksjoner.get(1).innhold()).contains("<p>Til: Ung Testesen</p>");
        assertThat(seksjoner.get(1).innhold()).doesNotContain("overskrift");

        // Del 3: Redigerbar
        assertThat(seksjoner.get(2).type()).isEqualTo(VedtaksbrevSeksjonType.REDIGERBAR);
        assertThat(seksjoner.get(2).innhold()).contains("<h1>Redigerbar overskrift</h1>");
        assertThat(seksjoner.get(2).innhold()).contains("Dette kan redigeres");
        assertThat(seksjoner.get(2).innhold()).doesNotContain("Du kan klage");
        // Skal ikke inneholde selve div taggen, bare innholdet
        assertThat(seksjoner.get(2).innhold()).doesNotContain("data-editable");

        // Del 4: Statisk etter editable
        assertThat(seksjoner.get(3).type()).isEqualTo(VedtaksbrevSeksjonType.STATISK);
        assertThat(seksjoner.get(3).innhold()).contains("<h2>Du kan klage</h2>");
        assertThat(seksjoner.get(3).innhold()).contains("Klagefrist");
    }


}

