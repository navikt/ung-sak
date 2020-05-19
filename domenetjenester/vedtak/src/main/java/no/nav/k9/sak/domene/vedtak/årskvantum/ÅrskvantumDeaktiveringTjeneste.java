package no.nav.k9.sak.domene.vedtak.årskvantum;


import no.nav.k9.sak.behandlingslager.behandling.Behandling;


public interface ÅrskvantumDeaktiveringTjeneste {
    void meldIfraOmIverksetting(Behandling behandling);
}
