package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold;

import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.formidling.innhold.ManueltVedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.template.dto.ManuellVedtaksbrevDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManueltVedtaksbrevInnholdByggerTest {

    private final ManueltVedtaksbrevInnholdBygger bygger = new ManueltVedtaksbrevInnholdBygger();

    @Test
    void skalGåOkHvisRiktigHtml() {
        String redigertBrevHtml = "<h1>Dette er en overskrift</h1><p>Dette er innholdet i brevet.</p>";

        TemplateInnholdResultat bygg = bygger.bygg(redigertBrevHtml);

        assertThat(bygg.templateType()).isEqualTo(TemplateType.MANUELT_VEDTAKSBREV);
        assertThat(bygg.templateInnholdDto()).isInstanceOf(ManuellVedtaksbrevDto.class);
        var dto = (ManuellVedtaksbrevDto) bygg.templateInnholdDto();
        assertThat(dto.tekstHtml()).isEqualTo(redigertBrevHtml);

    }

    @Test
    void skalKasteFeilHvisUgyldigBrevHtml() {
        assertThatThrownBy(() -> bygger.bygg("<p>Har bare overskrift</p>"))
            .isInstanceOf(IllegalStateException.class);
    }

}
