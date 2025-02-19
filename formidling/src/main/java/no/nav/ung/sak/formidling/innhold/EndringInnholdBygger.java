package no.nav.ung.sak.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.template.TemplateType;
import no.nav.ung.sak.formidling.template.dto.EndringDto;
import no.nav.ung.sak.formidling.template.dto.endring.EndringRapportertInntektDto;

@Dependent
public class EndringInnholdBygger implements VedtaksbrevInnholdBygger  {


    @Override
    public TemplateInnholdResultat bygg(Behandling behandlingId) {
        return new TemplateInnholdResultat(DokumentMalType.ENDRING_DOK, TemplateType.ENDRING_INNTEKT, new EndringDto(new EndringRapportertInntektDto(1000)));
    }
}
