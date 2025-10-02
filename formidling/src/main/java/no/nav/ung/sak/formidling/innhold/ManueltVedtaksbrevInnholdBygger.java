package no.nav.ung.sak.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.formidling.template.dto.ManuellVedtaksbrevDto;

@Dependent
public class ManueltVedtaksbrevInnholdBygger {

    @Inject
    public ManueltVedtaksbrevInnholdBygger() {
    }

    public TemplateInnholdResultat bygg(String brevHtml) {
        ManueltVedtaksbrevValidator.valider(brevHtml);

        return new TemplateInnholdResultat(
            TemplateType.MANUELT_VEDTAKSBREV,
            new ManuellVedtaksbrevDto(brevHtml),
            false);
    }


}
