package no.nav.ung.sak.etterlysning;

import no.nav.ung.brukerdialog.kontrakt.oppgaver.OppgaveYtelsetype;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;

public final class OppgaveYtelsetypeMapper {

    private OppgaveYtelsetypeMapper() {
    }

    public static OppgaveYtelsetype mapTilOppgaveYtelsetype(FagsakYtelseType ytelseType) {
        return switch (ytelseType) {
            case UNGDOMSYTELSE -> OppgaveYtelsetype.UNGDOMSYTELSE;
            case AKTIVITETSPENGER -> OppgaveYtelsetype.AKTIVITETSPENGER;
            default -> throw new IllegalArgumentException("Ukjent ytelsetype for oppgave: " + ytelseType);
        };
    }
}
