package no.nav.ung.sak.oppgave.typer.varsel.typer.kontrollerregisterinntekt;

import jakarta.persistence.*;
import no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.YtelseType;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Databasestruktur for oppgavedata av type KONTROLLER_REGISTERINNTEKT.
 * Lagrer periode, totalsummer og individuelle inntektsposter.
 */
@Entity(name = "KontrollerRegisterinntektOppgaveData")
@Table(name = "BD_OPPGAVE_DATA_KONTROLLER_REGISTERINNTEKT")
@Access(AccessType.FIELD)
public class KontrollerRegisterinntektOppgaveDataEntitet extends OppgaveDataEntitet {

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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "kontroller_data_id", nullable = false, updatable = false)
    private List<ArbeidOgFrilansInntektEntitet> arbeidOgFrilansInntekter = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "kontroller_data_id", nullable = false, updatable = false)
    private List<YtelseInntektEntitet> ytelseInntekter = new ArrayList<>();

    protected KontrollerRegisterinntektOppgaveDataEntitet() {
        // For JPA
    }

    public KontrollerRegisterinntektOppgaveDataEntitet(LocalDate fraOgMed,
                                                        LocalDate tilOgMed,
                                                        boolean gjelderDelerAvMåned,
                                                        int totalInntektArbeidFrilans,
                                                        int totalInntektYtelse,
                                                        int totalInntekt) {
        this.fraOgMed = fraOgMed;
        this.tilOgMed = tilOgMed;
        this.gjelderDelerAvMåned = gjelderDelerAvMåned;
        this.totalInntektArbeidFrilans = totalInntektArbeidFrilans;
        this.totalInntektYtelse = totalInntektYtelse;
        this.totalInntekt = totalInntekt;
    }


    public void leggTilArbeidOgFrilansInntekt(String arbeidsgiver, int inntekt) {
        arbeidOgFrilansInntekter.add(new ArbeidOgFrilansInntektEntitet(arbeidsgiver, inntekt));
    }

    public void leggTilYtelseInntekt(YtelseType ytelsetype, int inntekt) {
        ytelseInntekter.add(new YtelseInntektEntitet(ytelsetype, inntekt));
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
