package no.nav.ung.sak.formidling.klage.regler;

import no.nav.ung.kodeverk.klage.KlageVurderingType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageUtredningEntitet;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;


public interface VedtaksbrevKlageInnholdbyggerStrategy {
    VedtaksbrevStrategyResultat evaluer(Behandling behandling, KlageUtredningEntitet klageUtredning);

    boolean skalEvaluere(Behandling behandling, KlageVurderingType klageVurderingType);
}
