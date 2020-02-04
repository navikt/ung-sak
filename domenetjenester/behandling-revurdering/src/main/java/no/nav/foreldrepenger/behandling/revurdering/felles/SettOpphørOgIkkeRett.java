package no.nav.foreldrepenger.behandling.revurdering.felles;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.KonsekvensForYtelsen;
import no.nav.k9.kodeverk.vedtak.Vedtaksbrev;

public class SettOpphørOgIkkeRett {
    private SettOpphørOgIkkeRett() {
    }

    public static Behandlingsresultat fastsett(Behandling revurdering, Vedtaksbrev vedtaksbrev) {
        Behandlingsresultat behandlingsresultat = revurdering.getBehandlingsresultat();
        Behandlingsresultat.Builder behandlingsresultatBuilder = Behandlingsresultat.builderEndreEksisterende(behandlingsresultat);
        behandlingsresultatBuilder.medBehandlingResultatType(BehandlingResultatType.OPPHØR);
        behandlingsresultatBuilder.leggTilKonsekvensForYtelsen(KonsekvensForYtelsen.YTELSE_OPPHØRER);
        behandlingsresultatBuilder.medVedtaksbrev(vedtaksbrev);
        return behandlingsresultatBuilder.buildFor(revurdering);
    }
}
