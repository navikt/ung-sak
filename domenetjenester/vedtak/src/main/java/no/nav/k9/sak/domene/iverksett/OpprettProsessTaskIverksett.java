package no.nav.k9.sak.domene.iverksett;

import java.util.Optional;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;

public interface OpprettProsessTaskIverksett {
    void opprettIverksettingstasker(Behandling behandling);

    default void opprettIverksettingstasker(Behandling behandling, @SuppressWarnings("unused") Optional<String> initiellTaskNavn) {
        opprettIverksettingstasker(behandling);
    }

    default void meldIfraOmIverksettingTilÅrskvantum(Behandling behandling) { }
}
