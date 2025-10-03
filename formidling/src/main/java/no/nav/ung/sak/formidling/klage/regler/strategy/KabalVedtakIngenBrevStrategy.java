package no.nav.ung.sak.formidling.klage.regler.strategy;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.klage.KlageVurderingType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageUtredningEntitet;
import no.nav.ung.sak.formidling.klage.regler.VedtaksbrevKlageInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;

@Dependent
public final class KabalVedtakIngenBrevStrategy implements VedtaksbrevKlageInnholdbyggerStrategy {

    @Inject
    public KabalVedtakIngenBrevStrategy() {}

    @Override
    public VedtaksbrevStrategyResultat evaluer(Behandling behandling, KlageUtredningEntitet klageUtredning) {
        return VedtaksbrevStrategyResultat.utenBrev(IngenBrevÅrsakType.IKKE_RELEVANT, "Sender ikke brev for feilregistreringer og vedtak gjort i kabal");
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, KlageVurderingType klageVurderingType) {
        return KlageVurderingType.FEILREGISTRERT.equals(klageVurderingType) ||
            KlageVurderingType.HJEMSENDE_UTEN_Å_OPPHEVE.equals(klageVurderingType) ||
            KlageVurderingType.OPPHEVE_YTELSESVEDTAK.equals(klageVurderingType);
    }
}
