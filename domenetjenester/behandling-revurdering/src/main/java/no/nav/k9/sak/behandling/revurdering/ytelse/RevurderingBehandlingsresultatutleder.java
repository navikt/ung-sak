package no.nav.k9.sak.behandling.revurdering.ytelse;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarsel;

public interface RevurderingBehandlingsresultatutleder {
    VedtakVarsel bestemBehandlingsresultatForRevurdering(BehandlingReferanse revurderingRef, VedtakVarsel vedtakVarsel, boolean erVarselOmRevurderingSendt);
}
