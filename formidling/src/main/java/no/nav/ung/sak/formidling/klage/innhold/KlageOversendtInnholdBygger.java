package no.nav.ung.sak.formidling.klage.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.fritekst.FritekstRepository;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.template.dto.KlageOversendtDto;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;

@Dependent
public class KlageOversendtInnholdBygger implements VedtaksbrevInnholdBygger {

    private final FritekstRepository fritekstRepository;

    @Inject
    public KlageOversendtInnholdBygger(FritekstRepository fritekstRepository) {
        this.fritekstRepository = fritekstRepository;
    }

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje) {

        var fritekst = fritekstRepository.hentFritekst(behandling.getId(), KlageVurdertAv.VEDTAKSINSTANS.getKode())
            .orElseThrow(() -> new IllegalStateException("Brev ved oversendelse kan ikke genereres uten fritekst"));

        return new TemplateInnholdResultat(TemplateType.KLAGE_OVERSENDT,
            new KlageOversendtDto(
                "NAV Klageinstans Nord",
                fritekst
            )
        );
    }
}
