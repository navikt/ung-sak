package no.nav.ung.sak.oppgave.typer.oppgave.inntektsrapportering;

import jakarta.persistence.*;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;

import java.time.LocalDate;

/**
 * Databasestruktur for oppgavedata av type INNTEKTSRAPPORTERING.
 * Lagrer rapporteringsperioden og eventuell inntekt rapportert av bruker.
 */
@Entity(name = "InntektsrapporteringOppgaveData")
@Table(name = "BD_OPPGAVE_DATA_INNTEKTSRAPPORTERING")
public class InntektsrapporteringOppgaveDataEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BD_OPPGAVE_DATA_INNTEKTSRAPPORTERING")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "oppgave_id", nullable = false, updatable = false)
    private BrukerdialogOppgaveEntitet oppgave;

    @Column(name = "fra_og_med", nullable = false, updatable = false)
    private LocalDate fraOgMed;

    @Column(name = "til_og_med", nullable = false, updatable = false)
    private LocalDate tilOgMed;

    @Column(name = "gjelder_deler_av_maaned", nullable = false, updatable = false)
    private boolean gjelderDelerAvMåned;

    /** Null inntil bruker har besvart oppgaven. */
    @Column(name = "rapportert_inntekt")
    private Integer rapportertInntekt;

    protected InntektsrapporteringOppgaveDataEntitet() {
        // For JPA
    }

    public InntektsrapporteringOppgaveDataEntitet(BrukerdialogOppgaveEntitet oppgave,
                                                   LocalDate fraOgMed,
                                                   LocalDate tilOgMed,
                                                   boolean gjelderDelerAvMåned) {
        this.oppgave = oppgave;
        this.fraOgMed = fraOgMed;
        this.tilOgMed = tilOgMed;
        this.gjelderDelerAvMåned = gjelderDelerAvMåned;
    }

    public void setRapportertInntekt(Integer rapportertInntekt) {
        this.rapportertInntekt = rapportertInntekt;
    }

    public Long getId() {
        return id;
    }

    public BrukerdialogOppgaveEntitet getOppgave() {
        return oppgave;
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

    public Integer getRapportertInntekt() {
        return rapportertInntekt;
    }
}

