package no.nav.ung.sak.formidling.klage.regler.strategy;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.klage.KlageVurderingType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageUtredningEntitet;
import no.nav.ung.sak.formidling.klage.innhold.KlageMedholdInnholdBygger;
import no.nav.ung.sak.formidling.klage.regler.VedtaksbrevKlageInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;

@Dependent
public final class MedholdStrategy implements VedtaksbrevKlageInnholdbyggerStrategy {

    private final KlageMedholdInnholdBygger klageMedholdInnholdBygger;

    @Inject
    public MedholdStrategy(KlageMedholdInnholdBygger klageMedholdInnholdBygger) {
        this.klageMedholdInnholdBygger = klageMedholdInnholdBygger;
    }

    @Override
    public VedtaksbrevStrategyResultat evaluer(Behandling behandling, KlageUtredningEntitet klageUtredning) {
        return VedtaksbrevStrategyResultat.medBrev(
            DokumentMalType.KLAGE_VEDTAK_MEDHOLD, klageMedholdInnholdBygger, "Brev for medhold i klage");
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, KlageVurderingType klageVurderingType) {
        return KlageVurderingType.MEDHOLD_I_KLAGE.equals(klageVurderingType);
    }
}
