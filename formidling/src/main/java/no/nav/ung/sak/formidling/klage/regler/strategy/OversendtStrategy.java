package no.nav.ung.sak.formidling.klage.regler.strategy;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.klage.KlageVurderingType;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageRepository;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageUtredningEntitet;
import no.nav.ung.sak.formidling.klage.innhold.KlageOversendtInnholdBygger;
import no.nav.ung.sak.formidling.klage.regler.VedtaksbrevKlageInnholdbyggerStrategy;

@Dependent
public final class OversendtStrategy implements VedtaksbrevKlageInnholdbyggerStrategy {

    private final KlageOversendtInnholdBygger klageOversendtInnholdBygger;
    private final KlageRepository klageRepository;

    @Inject
    public OversendtStrategy(
        KlageRepository klageRepository,
        KlageOversendtInnholdBygger klageOversendtInnholdBygger) {
        this.klageRepository = klageRepository;
        this.klageOversendtInnholdBygger = klageOversendtInnholdBygger;
    }

    @Override
    public VedtaksbrevStrategyResultat evaluer(Behandling behandling, KlageUtredningEntitet klageUtredning) {
        return VedtaksbrevStrategyResultat.medBrev(
            DokumentMalType.KLAGE_OVERSENDT_KLAGEINSTANS, klageOversendtInnholdBygger);
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, KlageVurderingType klageVurderingType) {
        if (KlageVurderingType.STADFESTE_YTELSESVEDTAK.equals(klageVurderingType)) {
            var klageutredning = klageRepository.hentKlageUtredning(behandling.getId());
            var stadfestetAvKlageinstans = klageutredning.hentKlagevurdering(KlageVurdertAv.NK_KABAL).isPresent();
            return !stadfestetAvKlageinstans;
        }
        return false;
    }
}
