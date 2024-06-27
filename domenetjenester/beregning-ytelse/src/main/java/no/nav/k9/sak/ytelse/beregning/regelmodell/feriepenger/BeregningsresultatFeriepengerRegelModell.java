package no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.k9.sak.ytelse.beregning.regler.feriepenger.SaksnummerOgSisteBehandling;

@RuleDocumentationGrunnlag
public class BeregningsresultatFeriepengerRegelModell {
    private Set<Inntektskategori> inntektskategorier;
    private List<BeregningsresultatPeriode> beregningsresultatPerioder;
    private Set<LocalDate> skjæringstidspunkter;
    private List<FeriepengekorrigeringInfotrygd> feriepengekorrigeringInfotrygd;
    private List<DagpengerPeriode> perioderMedDagpenger;
    private int antallDagerFeriepenger;
    private boolean feriepengeopptjeningForHelg;
    private boolean ubegrensetFeriepengedagerVedRefusjon;
    private List<PeriodeMedSakOgBehandling> andelerSomKanGiFeriepengerForRelevaneSaker;

    private InfotrygdFeriepengegrunnlag infotrygdFeriepengegrunnlag;

    private BeregningsresultatFeriepengerRegelModell() {
        //tom konstruktør
    }

    public Set<Inntektskategori> getInntektskategorier() {
        return inntektskategorier;
    }

    public List<BeregningsresultatPeriode> getBeregningsresultatPerioder() {
        return beregningsresultatPerioder;
    }

    public Set<LocalDate> getSkjæringstidspunkter() {
        return skjæringstidspunkter;
    }

    public List<FeriepengekorrigeringInfotrygd> getFeriepengekorrigeringInfotrygd() {
        return feriepengekorrigeringInfotrygd;
    }

    public List<DagpengerPeriode> getPerioderMedDagpenger() {
        return perioderMedDagpenger;
    }

    public void setFeriepengekorrigeringInfotrygd(List<FeriepengekorrigeringInfotrygd> feriepengekorrigeringInfotrygd) {
        this.feriepengekorrigeringInfotrygd = feriepengekorrigeringInfotrygd;
    }

    @JsonIgnore
    public LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> getAndelerSomKanGiFeriepengerForRelevaneSaker() {
        return new LocalDateTimeline<>(
            getFeriepengerForRelevanteSaker().stream()
                .map(m -> new LocalDateSegment<>(m.periode(), m.sak()))
                .toList());
    }

    public List<PeriodeMedSakOgBehandling> getFeriepengerForRelevanteSaker() {
        return andelerSomKanGiFeriepengerForRelevaneSaker;
    }

    public InfotrygdFeriepengegrunnlag getInfotrygdFeriepengegrunnlag() {
        return infotrygdFeriepengegrunnlag;
    }

    public record PeriodeMedSakOgBehandling(LocalDateInterval periode, Set<SaksnummerOgSisteBehandling> sak) {
    }


    public int getAntallDagerFeriepenger() {
        return antallDagerFeriepenger;
    }

    public boolean harFeriepengeopptjeningForHelg() {
        return feriepengeopptjeningForHelg;
    }

    public boolean harUbegrensetFeriepengedagerVedRefusjon() {
        return ubegrensetFeriepengedagerVedRefusjon;
    }

    public static Builder builder(BeregningsresultatFeriepengerRegelModell regelModell) {
        return new Builder(regelModell);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final BeregningsresultatFeriepengerRegelModell kladd;

        private Builder(BeregningsresultatFeriepengerRegelModell regelModell) {
            kladd = regelModell;
        }

        public Builder() {
            this.kladd = new BeregningsresultatFeriepengerRegelModell();
        }

        public Builder medInntektskategorier(Set<Inntektskategori> inntektskategorier) {
            kladd.inntektskategorier = inntektskategorier;
            return this;
        }

        public Builder medBeregningsresultatPerioder(List<BeregningsresultatPeriode> beregningsresultatPerioder) {
            kladd.beregningsresultatPerioder = beregningsresultatPerioder;
            return this;
        }

        public Builder medSkjæringstidspunkt(Set<LocalDate> skjæringstidspunkt) {
            kladd.skjæringstidspunkter = skjæringstidspunkt;
            return this;
        }

        public Builder medAndelerSomKanGiFeriepengerForRelevaneSaker(LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> feriepengerTilkjentForRelevanteSaker) {
            kladd.andelerSomKanGiFeriepengerForRelevaneSaker = feriepengerTilkjentForRelevanteSaker.stream()
                .map(segment -> new PeriodeMedSakOgBehandling(segment.getLocalDateInterval(), segment.getValue()))
                .toList();
            return this;
        }

        public Builder medInfotrygdFeriepengegrunnlag(InfotrygdFeriepengegrunnlag infotrygdFeriepengegrunnlag) {
            kladd.infotrygdFeriepengegrunnlag = infotrygdFeriepengegrunnlag;
            return this;
        }

        public Builder medPerioderMedDagpenger(List<DagpengerPeriode> perioderMedDagpenger) {
            kladd.perioderMedDagpenger = perioderMedDagpenger;
            return this;
        }

        public Builder medAntallDagerFeriepenger(int antallDagerFeriepenger) {
            kladd.antallDagerFeriepenger = antallDagerFeriepenger;
            return this;
        }

        public Builder medUbegrensetFeriepengedagerVedRefusjon(boolean ubegrensetFeriepengedagerVedRefusjon) {
            kladd.ubegrensetFeriepengedagerVedRefusjon = ubegrensetFeriepengedagerVedRefusjon;
            return this;
        }

        public Builder medFeriepengeopptjeningForHelg(boolean feriepengeopptjeningForHelg) {
            kladd.feriepengeopptjeningForHelg = feriepengeopptjeningForHelg;
            return this;
        }

        public BeregningsresultatFeriepengerRegelModell build() {
            return kladd;
        }
    }
}
