package no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;

@RuleDocumentationGrunnlag
public class BeregningsresultatFeriepengerRegelModell {
    private Set<Inntektskategori> inntektskategorier;
    private List<BeregningsresultatPeriode> beregningsresultatPerioder;
    private LocalDateInterval feriepengerPeriode;
    private int antallDagerFeriepenger;


    private BeregningsresultatFeriepengerRegelModell() {
        //tom konstruktør
    }

    public Set<Inntektskategori> getInntektskategorier() {
        return inntektskategorier;
    }

    public List<BeregningsresultatPeriode> getBeregningsresultatPerioder() {
        return beregningsresultatPerioder;
    }

    public LocalDateInterval getFeriepengerPeriode() {
        return feriepengerPeriode;
    }

    public int getAntallDagerFeriepenger() {
        return antallDagerFeriepenger;
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

        public Builder medFeriepengerPeriode(LocalDate feriepengePeriodeFom, LocalDate feriepengePeriodeTom) {
            kladd.feriepengerPeriode = new LocalDateInterval(feriepengePeriodeFom, feriepengePeriodeTom);
            return this;
        }

        public Builder medAntallDagerFeriepenger(int antallDagerFeriepenger) {
            kladd.antallDagerFeriepenger = antallDagerFeriepenger;
            return this;
        }

        public BeregningsresultatFeriepengerRegelModell build() {
            return kladd;
        }
    }
}
