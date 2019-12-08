package no.nav.folketrygdloven.beregningsgrunnlag.gradering;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.jpa.tid.DatoIntervallEntitet;

public class AndelGradering {
    private AktivitetStatus aktivitetStatus;
    private Arbeidsgiver arbeidsgiver;
    private InternArbeidsforholdRef arbeidsforholdRef;
    private List<Gradering> graderinger = new ArrayList<>();
    private Long andelsnr;

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef != null ? arbeidsforholdRef : InternArbeidsforholdRef.nullRef();
    }

    public List<Gradering> getGraderinger() {
        return graderinger;
    }

    public Long getAndelsnr() {
        return andelsnr;
    }

    public boolean matcher(BeregningsgrunnlagPrStatusOgAndel andel) {
        if (!Objects.equals(getAktivitetStatus(), andel.getAktivitetStatus())) {
            return false;
        }
        if (AktivitetStatus.ARBEIDSTAKER.equals(getAktivitetStatus())) {
            Optional<BGAndelArbeidsforhold> bgaOpt = andel.getBgAndelArbeidsforhold();
            Optional<Arbeidsgiver> arbeidsgiverOpt = bgaOpt.map(BGAndelArbeidsforhold::getArbeidsgiver);
            if (!arbeidsgiverOpt.isPresent()) {
                return false;
            }
            BGAndelArbeidsforhold bga = bgaOpt.get();
            return gjelderFor(arbeidsgiverOpt.get(), bga.getArbeidsforholdRef());
        } else {
            return true;
        }
    }

    public boolean gjelderFor(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        return Objects.equals(getArbeidsgiver(), arbeidsgiver)
            && getArbeidsforholdRef().gjelderFor(arbeidsforholdRef);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AndelGradering)) {
            return false;
        }
        AndelGradering that = (AndelGradering) o;
        return Objects.equals(aktivitetStatus, that.aktivitetStatus)
            && Objects.equals(arbeidsgiver, that.arbeidsgiver)
            && Objects.equals(arbeidsforholdRef, that.arbeidsforholdRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktivitetStatus, arbeidsgiver, arbeidsforholdRef);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
            + "<aktivitetStatus=" + aktivitetStatus
            + (arbeidsgiver == null ? "" : ", arbeidsgiver=" + arbeidsgiver)
            + (arbeidsforholdRef == null ? "" : ", arbeidsforholdRef=" + arbeidsforholdRef)
            + ", graderinger=" + graderinger
            + ">";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final AndelGradering kladd;

        private Builder() {
            kladd = new AndelGradering();
        }

        public Builder medStatus(AktivitetStatus aktivitetStatus) {
            kladd.aktivitetStatus = aktivitetStatus;
            return this;
        }

        public Builder medArbeidsgiver(Arbeidsgiver arbeidsgiver) {
            kladd.arbeidsgiver = arbeidsgiver;
            return this;
        }

        public Builder medArbeidsforholdRef(InternArbeidsforholdRef arbeidsforholdRef) {
            kladd.arbeidsforholdRef = arbeidsforholdRef;
            return this;
        }

        public Builder medAndelsnr(Long andelsnr) {
            kladd.andelsnr = andelsnr;
            return this;
        }

        public Builder leggTilGradering(Gradering gradering) {
            kladd.graderinger.add(gradering);
            return this;
        }

        public Builder leggTilGradering(DatoIntervallEntitet periode, BigDecimal arbeidstidsprosent) {
            return leggTilGradering(new Gradering(periode, arbeidstidsprosent));
        }

        public Builder medGradering(LocalDate fom, LocalDate tom, int arbeidstidsprosent) {
            return leggTilGradering(fom, tom, BigDecimal.valueOf(arbeidstidsprosent));
        }

        public Builder leggTilGradering(LocalDate fom, LocalDate tom, BigDecimal arbeidstidsprosent) {
            return leggTilGradering(new Gradering(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), arbeidstidsprosent));
        }

        public AndelGradering build() {
            return kladd;
        }

    }

    public static final class Gradering implements Comparable<Gradering> {

        /**
         * Perioden det gjelder.
         */
        private final DatoIntervallEntitet periode;

        /**
         * En arbeidstaker kan kombinere foreldrepenger med deltidsarbeid.
         *
         * Når arbeidstakeren jobber deltid, utgjør foreldrepengene differansen mellom deltidsarbeidet og en 100 prosent stilling.
         * Det er ingen nedre eller øvre grense for hvor mye eller lite arbeidstakeren kan arbeide.
         *
         * Eksempel
         * Arbeidstaker A har en 100 % stilling og arbeider fem dager i uken. Arbeidstakeren ønsker å arbeide to dager i uken i
         * foreldrepengeperioden.
         * Arbeidstids- prosenten blir da 40 %.
         *
         * Arbeidstaker B har en 80 % stilling og arbeider fire dager i uken. Arbeidstakeren ønsker å arbeide to dager i uken i
         * foreldrepengeperioden.
         * Arbeidstidprosenten blir også her 40 %.
         *
         * @return prosentsats
         */
        private final BigDecimal arbeidstidProsent;

        public Gradering(LocalDate periodeFom, LocalDate periodeTom, BigDecimal arbeidstidProsent) {
            this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(periodeFom, periodeTom);
            this.arbeidstidProsent = Objects.requireNonNull(arbeidstidProsent);
            if (arbeidstidProsent.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Arbeidstidsprosent < 0: " + arbeidstidProsent + ", i periode " + this.periode);
            }
        }

        public Gradering(DatoIntervallEntitet periode, BigDecimal arbeidstidProsent) {
            this(periode.getFomDato(), periode.getTomDato(), arbeidstidProsent);
        }

        public DatoIntervallEntitet getPeriode() {
            return periode;
        }

        public BigDecimal getArbeidstidProsent() {
            return arbeidstidProsent;
        }

        @Override
        public int compareTo(Gradering o) {
            return o == null ? 1 : this.getPeriode().compareTo(o.getPeriode());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj == null || !this.getClass().equals(obj.getClass())) {
                return false;
            }
            var other = (Gradering) obj;
            return Objects.equals(this.getPeriode(), other.getPeriode());
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(getPeriode());
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "<periode=" + periode + ", arbeidstidsprosent=" + arbeidstidProsent + "%>";
        }
    }

}
