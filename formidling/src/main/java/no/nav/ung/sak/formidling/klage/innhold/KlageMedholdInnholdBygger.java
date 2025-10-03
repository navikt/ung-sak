package no.nav.ung.sak.formidling.klage.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.klage.KlageVurderingOmgjør;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageRepository;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.template.dto.KlageMedholdDto;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;

@Dependent
public class KlageMedholdInnholdBygger implements VedtaksbrevInnholdBygger {

    private KlageRepository klageRepository;

    @Inject
    public KlageMedholdInnholdBygger(KlageRepository klageRepository) {
        this.klageRepository = klageRepository;
    }

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje) {
        throw new UnsupportedOperationException("Gjelder ikke for klagebrev");
    }

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling) {
        var klageutredning = klageRepository.hentKlageUtredning(behandling.getId());
        var klageVurdering = klageutredning.hentKlagevurdering(KlageVurdertAv.VEDTAKSINSTANS).orElseThrow();
        var omgjørÅrsak = klageVurdering.getKlageresultat().getKlageVurderingOmgjør().orElseThrow();
        var harDelvisMedhold =  KlageVurderingOmgjør.DELVIS_MEDHOLD_I_KLAGE.equals(omgjørÅrsak);

        return new TemplateInnholdResultat(TemplateType.KLAGE_MEDHOLD,
            new KlageMedholdDto(
                harDelvisMedhold,
                true
            ),
            false);
    }
}
