package no.nav.ung.sak.behandling.revurdering;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

public interface RevurderingFeil extends DeklarerteFeil {
    RevurderingFeil FACTORY = FeilFactory.create(RevurderingFeil.class);

    @TekniskFeil(feilkode = "FP-317517", feilmelding = "finner ingen behandling som kan revurderes for fagsak: %s", logLevel = LogLevel.WARN)
    Feil tjenesteFinnerIkkeBehandlingForRevurdering(Long fagsakId);

    @TekniskFeil(feilkode = "FP-186234", feilmelding = "Revurdering med id %s har ikke original behandling", logLevel = LogLevel.ERROR)
    Feil revurderingManglerOriginalBehandling(Long behandlingId);

    @TekniskFeil(feilkode = "FP-818307", feilmelding = "Behandling med id %s mangler beregning", logLevel = LogLevel.ERROR)
    Feil behandlingManglerBeregning(Long behandlingId);

}
