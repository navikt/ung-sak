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
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;

@Dependent
public final class StadfesteStrategy implements VedtaksbrevKlageInnholdbyggerStrategy {

    private final KlageOversendtInnholdBygger klageOversendtInnholdBygger;
    private final KlageRepository klageRepository;

    @Inject
    public StadfesteStrategy(
        KlageRepository klageRepository,
        KlageOversendtInnholdBygger klageOversendtInnholdBygger) {
        this.klageRepository = klageRepository;
        this.klageOversendtInnholdBygger = klageOversendtInnholdBygger;
    }

    @Override
    public VedtaksbrevStrategyResultat evaluer(Behandling behandling, KlageUtredningEntitet klageUtredning) {
        var klageutredning = klageRepository.hentKlageUtredning(behandling.getId());
        var stadfestetAvKlageinstans = klageutredning.hentKlagevurdering(KlageVurdertAv.KLAGEINSTANS).isPresent();
        if (!stadfestetAvKlageinstans) {
            return VedtaksbrevStrategyResultat.medBrev(
                DokumentMalType.KLAGE_OVERSENDT_KLAGEINSTANS, klageOversendtInnholdBygger, "Brev for oversendelse av klage til klageinstans");
        } else {
            return VedtaksbrevStrategyResultat.utenBrev(IngenBrevÅrsakType.IKKE_RELEVANT, "Sender ikke brev når klage er stadfestet i klageinstansen");
        }
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, KlageVurderingType klageVurderingType) {
        return KlageVurderingType.STADFESTE_YTELSESVEDTAK.equals(klageVurderingType);
    }
}
