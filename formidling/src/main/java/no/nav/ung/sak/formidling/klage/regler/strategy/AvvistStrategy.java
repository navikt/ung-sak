package no.nav.ung.sak.formidling.klage.regler.strategy;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.klage.KlageVurderingType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageUtredningEntitet;
import no.nav.ung.sak.formidling.klage.innhold.KlageAvvistInnholdBygger;
import no.nav.ung.sak.formidling.klage.regler.VedtaksbrevKlageInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;

@Dependent
public final class AvvistStrategy implements VedtaksbrevKlageInnholdbyggerStrategy {

    private final KlageAvvistInnholdBygger klageAvvistInnholdBygger;

    @Inject
    public AvvistStrategy(KlageAvvistInnholdBygger klageAvvistInnholdBygger) {
        this.klageAvvistInnholdBygger = klageAvvistInnholdBygger;
    }

    @Override
    public VedtaksbrevStrategyResultat evaluer(Behandling behandling, KlageUtredningEntitet klageUtredning) {
        return VedtaksbrevStrategyResultat.medBrev(
            DokumentMalType.KLAGE_AVVIST_DOK, klageAvvistInnholdBygger, "Brev for avvist klage");
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, KlageVurderingType klageVurderingType) {
        return KlageVurderingType.AVVIS_KLAGE.equals(klageVurderingType);
    }
}
