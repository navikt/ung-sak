package no.nav.ung.sak.oppgave.typer.varsel.typer.endretperiode;

import jakarta.persistence.*;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretperiode.PeriodeEndringType;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Databasestruktur for oppgavedata av type ENDRET_PERIODE.
 * Lagrer ny og forrige periode samt hvilke typer endringer som har skjedd.
 * Endringer lagres som en kommaseparert streng av PeriodeEndringType-verdier.
 */
@Entity(name = "EndretPeriodeOppgaveData")
@Table(name = "BD_OPPGAVE_DATA_ENDRET_PERIODE")
public class EndretPeriodeOppgaveDataEntitet extends OppgaveDataEntitet {

    /** Fra-dato for ny periode. Null dersom perioden er fjernet. */
    @Column(name = "ny_periode_fom", updatable = false)
    private LocalDate nyPeriodeFom;

    @Column(name = "ny_periode_tom", updatable = false)
    private LocalDate nyPeriodeTom;

    /** Fra-dato for forrige periode. Null dersom dette er en ny periode. */
    @Column(name = "forrige_periode_fom", updatable = false)
    private LocalDate forrigePeriodeFom;

    @Column(name = "forrige_periode_tom", updatable = false)
    private LocalDate forrigePeriodeTom;

    /**
     * Kommaseparert liste av PeriodeEndringType-verdier.
     * Eksempel: "ENDRET_STARTDATO,ENDRET_SLUTTDATO"
     */
    @Column(name = "endringer", nullable = false, updatable = false)
    private String endringer;

    protected EndretPeriodeOppgaveDataEntitet() {
        // For JPA
    }

    public EndretPeriodeOppgaveDataEntitet(LocalDate nyPeriodeFom,
                                            LocalDate nyPeriodeTom,
                                            LocalDate forrigePeriodeFom,
                                            LocalDate forrigePeriodeTom,
                                            Set<PeriodeEndringType> endringer) {
        this.nyPeriodeFom = nyPeriodeFom;
        this.nyPeriodeTom = nyPeriodeTom;
        this.forrigePeriodeFom = forrigePeriodeFom;
        this.forrigePeriodeTom = forrigePeriodeTom;
        this.endringer = endringer.stream()
            .map(PeriodeEndringType::name)
            .collect(Collectors.joining(","));
    }


    public LocalDate getNyPeriodeFom() {
        return nyPeriodeFom;
    }

    public LocalDate getNyPeriodeTom() {
        return nyPeriodeTom;
    }

    public LocalDate getForrigePeriodeFom() {
        return forrigePeriodeFom;
    }

    public LocalDate getForrigePeriodeTom() {
        return forrigePeriodeTom;
    }

    public Set<PeriodeEndringType> getEndringer() {
        if (endringer == null || endringer.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(endringer.split(","))
            .map(String::trim)
            .map(PeriodeEndringType::valueOf)
            .collect(Collectors.toSet());
    }
}

