package no.nav.foreldrepenger.økonomi.økonomistøtte;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;

@SuppressWarnings("unused")
@ApplicationScoped
public class OppdragskontrollTjeneste {

    @Inject
    public OppdragskontrollTjeneste() {
    }

    public Optional<Oppdragskontroll> opprettOppdrag(Long behandlingId, Long prosessTaskId) {
        // FIXME K9 implementer økonomi oppdrag (her eller i fpoppdrag)
        return Optional.empty();
    }

    public Optional<Oppdragskontroll> finnOppdragForBehandling(Long behandlingId) {
        // FIXME K9 finn økonomi oppdrag
        return Optional.empty();
    }

}
