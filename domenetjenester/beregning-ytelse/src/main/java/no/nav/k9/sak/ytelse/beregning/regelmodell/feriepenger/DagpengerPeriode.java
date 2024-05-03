package no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger;

import java.time.LocalDate;

public record DagpengerPeriode(DagpengerKilde kilde, LocalDate fom, LocalDate tom) {
}
