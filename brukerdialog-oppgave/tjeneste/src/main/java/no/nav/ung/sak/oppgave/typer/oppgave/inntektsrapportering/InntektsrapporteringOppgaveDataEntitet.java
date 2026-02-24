package no.nav.ung.sak.oppgave.typer.oppgave.inntektsrapportering;

import jakarta.persistence.*;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;

import java.time.LocalDate;

/**
 * Databasestruktur for oppgavedata av type INNTEKTSRAPPORTERING.
 * Lagrer rapporteringsperioden og eventuell inntekt rapportert av bruker.
 */
@Entity(name = "InntektsrapporteringOppgaveData")
@Table(name = "BD_OPPGAVE_DATA_INNTEKTSRAPPORTERING")
@Access(AccessType.FIELD)
public class InntektsrapporteringOppgaveDataEntitet extends OppgaveDataEntitet {

    @Column(name = "fra_og_med", nullable = false, updatable = false)
    private LocalDate fraOgMed;

    @Column(name = "til_og_med", nullable = false, updatable = false)
    private LocalDate tilOgMed;

    @Column(name = "gjelder_deler_av_maaned", nullable = false, updatable = false)
    private boolean gjelderDelerAvMåned;

    protected InntektsrapporteringOppgaveDataEntitet() {
        // For JPA
    }

    public InntektsrapporteringOppgaveDataEntitet(LocalDate fraOgMed,
                                                   LocalDate tilOgMed,
                                                   boolean gjelderDelerAvMåned) {
        this.fraOgMed = fraOgMed;
        this.tilOgMed = tilOgMed;
        this.gjelderDelerAvMåned = gjelderDelerAvMåned;
    }

    public LocalDate getFraOgMed() {
        return fraOgMed;
    }

    public LocalDate getTilOgMed() {
        return tilOgMed;
    }

    public boolean isGjelderDelerAvMåned() {
        return gjelderDelerAvMåned;
    }

}
