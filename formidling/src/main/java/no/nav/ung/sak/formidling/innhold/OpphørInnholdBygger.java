package no.nav.ung.sak.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.template.dto.OpphørDto;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Dependent
public class OpphørInnholdBygger implements VedtaksbrevInnholdBygger {

    private static final Logger LOG = LoggerFactory.getLogger(OpphørInnholdBygger.class);

    @Inject
    public OpphørInnholdBygger() {
    }


    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> resultatTidslinje) {
        return new TemplateInnholdResultat(DokumentMalType.ENDRING_DOK, TemplateType.OPPHØR,
            new OpphørDto(
                resultatTidslinje
                    .filterValue(it -> it.resultatInfo().stream()
                        .anyMatch(r -> r.detaljertResultatType() == DetaljertResultatType.ENDRING_SLUTTDATO))
                    .getMinLocalDate()
            ));
    }

}
