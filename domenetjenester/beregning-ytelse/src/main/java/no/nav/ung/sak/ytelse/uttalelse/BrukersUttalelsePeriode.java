package no.nav.ung.sak.ytelse.uttalelse;


import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.util.UUID;

public record BrukersUttalelsePeriode(DatoIntervallEntitet periode, Status status, Uttalelse uttalelse, UUID iayGrunnlagUUID) {

}
