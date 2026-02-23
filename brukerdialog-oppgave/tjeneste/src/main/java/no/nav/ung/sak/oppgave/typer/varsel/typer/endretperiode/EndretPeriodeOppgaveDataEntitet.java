package no.nav.ung.sak.oppgave.typer.varsel.typer.endretperiode;

import jakarta.persistence.*;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretperiode.PeriodeEndringType;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Databasestruktur for oppgavedata av type ENDRET_PERIODE.
 * Lagrer ny og forrige periode samt hvilke typer endringer som har skjedd.
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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "endret_periode_data_id", nullable = false, updatable = false)
    private List<PeriodeEndring> endringer = new ArrayList<>();

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
        endringer.forEach(type -> this.endringer.add(new PeriodeEndring(type)));
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
        return endringer.stream()
            .map(PeriodeEndring::getEndringType)
            .collect(Collectors.toSet());
    }
}
