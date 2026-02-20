package no.nav.ung.sak.oppgave.typer.varsel.typer.kontrollerregisterinntekt;

import jakarta.persistence.*;
import no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.YtelseType;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Databasestruktur for oppgavedata av type KONTROLLER_REGISTERINNTEKT.
 * Lagrer periode, totalsummer og individuelle inntektsposter.
 */
@Entity(name = "KontrollerRegisterinntektOppgaveData")
@Table(name = "BD_OPPGAVE_DATA_KONTROLLER_REGISTERINNTEKT")
public class KontrollerRegisterinntektOppgaveDataEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BD_OPPGAVE_DATA_KONTROLLER_REG_INNTEKT")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bd_oppgave_id", nullable = false, updatable = false)
    private BrukerdialogOppgaveEntitet oppgave;

    @Column(name = "fra_og_med", nullable = false, updatable = false)
    private LocalDate fraOgMed;

    @Column(name = "til_og_med", nullable = false, updatable = false)
    private LocalDate tilOgMed;

    @Column(name = "gjelder_deler_av_maaned", nullable = false, updatable = false)
    private boolean gjelderDelerAvMåned;

    @Column(name = "total_inntekt_arbeid_frilans", nullable = false, updatable = false)
    private int totalInntektArbeidFrilans;

    @Column(name = "total_inntekt_ytelse", nullable = false, updatable = false)
    private int totalInntektYtelse;

    @Column(name = "total_inntekt", nullable = false, updatable = false)
    private int totalInntekt;

    @OneToMany(mappedBy = "oppgaveData", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ArbeidOgFrilansInntektEntitet> arbeidOgFrilansInntekter = new ArrayList<>();

    @OneToMany(mappedBy = "oppgaveData", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<YtelseInntektEntitet> ytelseInntekter = new ArrayList<>();

    protected KontrollerRegisterinntektOppgaveDataEntitet() {
        // For JPA
    }

    public KontrollerRegisterinntektOppgaveDataEntitet(BrukerdialogOppgaveEntitet oppgave,
                                                        LocalDate fraOgMed,
                                                        LocalDate tilOgMed,
                                                        boolean gjelderDelerAvMåned,
                                                        int totalInntektArbeidFrilans,
                                                        int totalInntektYtelse,
                                                        int totalInntekt) {
        this.oppgave = oppgave;
        this.fraOgMed = fraOgMed;
        this.tilOgMed = tilOgMed;
        this.gjelderDelerAvMåned = gjelderDelerAvMåned;
        this.totalInntektArbeidFrilans = totalInntektArbeidFrilans;
        this.totalInntektYtelse = totalInntektYtelse;
        this.totalInntekt = totalInntekt;
    }

    public void leggTilArbeidOgFrilansInntekt(String arbeidsgiver, int inntekt) {
        arbeidOgFrilansInntekter.add(new ArbeidOgFrilansInntektEntitet(this, arbeidsgiver, inntekt));
    }

    public void leggTilYtelseInntekt(YtelseType ytelsetype, int inntekt) {
        ytelseInntekter.add(new YtelseInntektEntitet(this, ytelsetype, inntekt));
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

    public int getTotalInntektArbeidFrilans() {
        return totalInntektArbeidFrilans;
    }

    public int getTotalInntektYtelse() {
        return totalInntektYtelse;
    }

    public int getTotalInntekt() {
        return totalInntekt;
    }

    public List<ArbeidOgFrilansInntektEntitet> getArbeidOgFrilansInntekter() {
        return List.copyOf(arbeidOgFrilansInntekter);
    }

    public List<YtelseInntektEntitet> getYtelseInntekter() {
        return List.copyOf(ytelseInntekter);
    }
}

