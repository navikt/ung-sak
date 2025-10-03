package no.nav.ung.sak.formidling.klage.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.template.dto.KlageOversendtDto;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;

@Dependent
public class KlageOversendtInnholdBygger implements VedtaksbrevInnholdBygger {

    @Inject
    public KlageOversendtInnholdBygger() {
    }

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje) {
        throw new UnsupportedOperationException("Gjelder ikke for klagebrev");
    }

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling) {

        return new TemplateInnholdResultat(TemplateType.KLAGE_OVERSENDT,
            new KlageOversendtDto(
                "NAV Klageinstans Nord"
            ),
            false);
    }
}
