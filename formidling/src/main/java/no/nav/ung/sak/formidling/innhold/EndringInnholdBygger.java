package no.nav.ung.sak.formidling.innhold;

import java.time.LocalDate;

import jakarta.enterprise.context.Dependent;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.template.TemplateType;
import no.nav.ung.sak.formidling.template.dto.EndringDto;
import no.nav.ung.sak.formidling.template.dto.endring.EndringRapportertInntektDto;
import no.nav.ung.sak.formidling.template.dto.felles.PeriodeDto;

@Dependent
public class EndringInnholdBygger implements VedtaksbrevInnholdBygger  {


    @Override
    public TemplateInnholdResultat bygg(Behandling behandlingId) {
        return new TemplateInnholdResultat(DokumentMalType.ENDRING_DOK, TemplateType.ENDRING_INNTEKT, new EndringDto(
            new EndringRapportertInntektDto(
                new PeriodeDto(LocalDate.now(), LocalDate.now()),
                10000,
                7393,
                66,
                6600,
                636,
                336
            )
        ));
    }
}
