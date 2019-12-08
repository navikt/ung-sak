package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Grunnbeløp;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatusMedHjemmel;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Dekningsgrad;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Inntektsgrunnlag;

public class Beregningsgrunnlag {
    private final List<AktivitetStatusMedHjemmel> aktivitetStatuser = new ArrayList<>();
    private LocalDate skjæringstidspunkt;
    private Inntektsgrunnlag inntektsgrunnlag;
    @JsonManagedReference
    private final List<BeregningsgrunnlagPeriode> beregningsgrunnlagPerioder = new ArrayList<>();
    private SammenligningsGrunnlag sammenligningsGrunnlag;
    private EnumMap<AktivitetStatus, SammenligningsGrunnlag> sammenligningsGrunnlagPrStatus = new EnumMap<>(AktivitetStatus.class);
    private Dekningsgrad dekningsgrad = Dekningsgrad.DEKNINGSGRAD_100;
    private BigDecimal grunnbeløp;
    private List<Grunnbeløp> grunnbeløpSatser = new ArrayList<>();
    private boolean beregningForSykepenger = false; //Alltid false i FPSAK
    private boolean hattMilitærIOpptjeningsperioden = false;
    private int antallGrunnbeløpMilitærHarKravPå = 3; //TODO : Denne burde mappes for SVP (og eventuelt FP) fra f. eks konfig-verdi

    private Beregningsgrunnlag() {
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public Inntektsgrunnlag getInntektsgrunnlag() {
        return inntektsgrunnlag;
    }

    public List<AktivitetStatusMedHjemmel> getAktivitetStatuser() {
        return Collections.unmodifiableList(aktivitetStatuser);
    }

    public SammenligningsGrunnlag getSammenligningsGrunnlag() {
        return sammenligningsGrunnlag;
    }

    public List<BeregningsgrunnlagPeriode> getBeregningsgrunnlagPerioder() {
        return beregningsgrunnlagPerioder.stream()
            .sorted(Comparator.comparing(bg -> bg.getBeregningsgrunnlagPeriode().getFom()))
            .collect(Collectors.toUnmodifiableList());
    }

    public Dekningsgrad getDekningsgrad() {
        return dekningsgrad;
    }

    public BigDecimal getGrunnbeløp() {
        return grunnbeløp;
    }

    public BigDecimal getMinsteinntektMilitærHarKravPå() {
        return grunnbeløp.multiply(BigDecimal.valueOf(antallGrunnbeløpMilitærHarKravPå));
    }

    public AktivitetStatusMedHjemmel getAktivitetStatus(AktivitetStatus aktivitetStatus) {
        return aktivitetStatuser.stream().filter(as -> as.inneholder(aktivitetStatus)).findAny()
                .orElseThrow(() -> new IllegalStateException("Beregningsgrunnlaget mangler regel for status " + aktivitetStatus.getBeskrivelse()));
    }

    public long verdiAvG(LocalDate dato) {
        Optional<Grunnbeløp> optional = grunnbeløpSatser.stream()
            .filter(g -> !dato.isBefore(g.getFom()) && !dato.isAfter(g.getTom()))
            .findFirst();

        if (optional.isPresent()) {
            return optional.get().getGVerdi();
        } else {
            throw new IllegalArgumentException("Kjenner ikke G-verdi for året " + dato.getYear());
        }
    }

    public long snittverdiAvG(int år) {
        Optional<Grunnbeløp> optional = grunnbeløpSatser.stream().filter(g -> g.getFom().getYear() == år).findFirst();
        if (optional.isPresent()) {
            return optional.get().getGSnitt();
        } else {
            throw new IllegalArgumentException("Kjenner ikke GSnitt-verdi for året " + år);
        }
    }

    public boolean isBeregningForSykepenger() {
        return beregningForSykepenger;
    }

    public boolean harHattMilitærIOpptjeningsperioden() {
        return hattMilitærIOpptjeningsperioden;
    }

    public int getAntallGrunnbeløpMilitærHarKravPå() {
        return antallGrunnbeløpMilitærHarKravPå;
    }

    public EnumMap<AktivitetStatus, SammenligningsGrunnlag> getSammenligningsGrunnlagPrAktivitetstatus() {
        return sammenligningsGrunnlagPrStatus;
    }

    public SammenligningsGrunnlag getSammenligningsGrunnlagPrAktivitetstatus(AktivitetStatus aktivitetStatus) {
        return sammenligningsGrunnlagPrStatus.get(aktivitetStatus);
    }

    public static Builder builder() {
        return new Builder();
    }

    // FIXME: Dette er en skjult mutator siden den endrer på oppgitt beregningsgrunnlag.  endre metode navn eller pattern?
    public static Builder builder(Beregningsgrunnlag beregningsgrunnlag) {
        return new Builder(beregningsgrunnlag);
    }

    public static class Builder {
        private Beregningsgrunnlag beregningsgrunnlagMal;

        private Builder() {
            beregningsgrunnlagMal = new Beregningsgrunnlag();
        }

        private Builder(Beregningsgrunnlag beregningsgrunnlag) {
            beregningsgrunnlagMal = beregningsgrunnlag;
        }

        public Builder medInntektsgrunnlag(Inntektsgrunnlag inntektsgrunnlag) {
            beregningsgrunnlagMal.inntektsgrunnlag = inntektsgrunnlag;
            return this;
        }

        public Builder medSkjæringstidspunkt(LocalDate skjæringstidspunkt) {
            beregningsgrunnlagMal.skjæringstidspunkt = skjæringstidspunkt;
            return this;
        }

        public Builder medAktivitetStatuser(List<AktivitetStatusMedHjemmel> aktivitetStatusList) {
            beregningsgrunnlagMal.aktivitetStatuser.addAll(aktivitetStatusList);
            return this;
        }

        public Builder medSammenligningsgrunnlag(SammenligningsGrunnlag sammenligningsGrunnlag) {
            beregningsgrunnlagMal.sammenligningsGrunnlag = sammenligningsGrunnlag;
            return this;
        }

        public Builder medBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
            beregningsgrunnlagMal.beregningsgrunnlagPerioder.add(beregningsgrunnlagPeriode);
            beregningsgrunnlagPeriode.setBeregningsgrunnlag(beregningsgrunnlagMal);
            return this;
        }

        public Builder medBeregningsgrunnlagPerioder(List<BeregningsgrunnlagPeriode> beregningsgrunnlagPerioder) {
            beregningsgrunnlagMal.beregningsgrunnlagPerioder.addAll(beregningsgrunnlagPerioder);
            beregningsgrunnlagPerioder.forEach(bgPeriode -> bgPeriode.setBeregningsgrunnlag(beregningsgrunnlagMal));
            return this;
        }

        public Builder medDekningsgrad(Dekningsgrad dekningsgrad) {
            beregningsgrunnlagMal.dekningsgrad = dekningsgrad;
            return this;
        }

        public Builder medGrunnbeløp(BigDecimal grunnbeløp) {
            beregningsgrunnlagMal.grunnbeløp = grunnbeløp;
            return this;
        }

        public Builder medGrunnbeløpSatser(List<Grunnbeløp> grunnbeløpSatser) {
            beregningsgrunnlagMal.grunnbeløpSatser.clear();
            beregningsgrunnlagMal.grunnbeløpSatser.addAll(grunnbeløpSatser);
            return this;
        }

        //Brukes bare i sykepenger og i enhetstest
        public Builder medBeregningForSykepenger(boolean beregningForSykepenger) {
            beregningsgrunnlagMal.beregningForSykepenger = beregningForSykepenger;
            return this;
        }

        public Builder medMilitærIOpptjeningsperioden(boolean hattMilitærIOpptjeningsperioden) {
            beregningsgrunnlagMal.hattMilitærIOpptjeningsperioden = hattMilitærIOpptjeningsperioden;
            return this;
        }

        public Builder medAntallGrunnbeløpMilitærHarKravPå(int antallGrunnbeløpMilitærHarKravPå) {
            beregningsgrunnlagMal.antallGrunnbeløpMilitærHarKravPå = antallGrunnbeløpMilitærHarKravPå;
            return this;
        }

        public Builder medSammenligningsgrunnlagPrStatus(AktivitetStatus aktivitetStatus, SammenligningsGrunnlag sammenligningsGrunnlag) {
            beregningsgrunnlagMal.sammenligningsGrunnlagPrStatus.put(aktivitetStatus, sammenligningsGrunnlag);
            return this;
        }

        public Beregningsgrunnlag build() {
            verifyStateForBuild();
            return beregningsgrunnlagMal;
        }

        private void verifyStateForBuild() {
            Objects.requireNonNull(beregningsgrunnlagMal.inntektsgrunnlag, "inntektsgrunnlag");
            Objects.requireNonNull(beregningsgrunnlagMal.skjæringstidspunkt, "skjæringstidspunkt");
            Objects.requireNonNull(beregningsgrunnlagMal.aktivitetStatuser, "aktivitetStatuser");
            if (beregningsgrunnlagMal.beregningsgrunnlagPerioder.isEmpty()) {
                throw new IllegalStateException("Beregningsgrunnlaget må inneholde minst 1 periode");
            }
            if (beregningsgrunnlagMal.aktivitetStatuser.isEmpty()) {
                throw new IllegalStateException("Beregningsgrunnlaget må inneholde minst 1 status");
            }
            if (beregningsgrunnlagMal.grunnbeløpSatser.isEmpty()) {
                throw new IllegalStateException("Beregningsgrunnlaget må inneholde grunnbeløpsatser");
            }
        }
    }
}
