package no.nav.k9.sak.skjæringstidspunkt;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.k9.sak.behandling.BehandlingReferanse;

public interface YtelseOpphørtidspunktTjeneste {
    Optional<LocalDate> getOpphørsdato(BehandlingReferanse ref);

    boolean erOpphør(BehandlingReferanse ref);

    Boolean erOpphørEtterSkjæringstidspunkt(BehandlingReferanse ref);
}
