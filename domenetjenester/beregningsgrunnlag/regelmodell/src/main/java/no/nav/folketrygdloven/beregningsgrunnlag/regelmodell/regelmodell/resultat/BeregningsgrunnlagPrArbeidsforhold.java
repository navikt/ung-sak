package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Arbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Inntektskategori;

public class BeregningsgrunnlagPrArbeidsforhold {
    private BigDecimal naturalytelseBortfaltPrÅr;
    private BigDecimal naturalytelseTilkommetPrÅr;
    private BigDecimal beregnetPrÅr;
    private BigDecimal overstyrtPrÅr;
    private BigDecimal fordeltPrÅr;
    private BigDecimal bruttoPrÅr;
    private BigDecimal avkortetPrÅr;
    private BigDecimal redusertPrÅr;
    private Periode beregningsperiode;
    private Arbeidsforhold arbeidsforhold;
    private BigDecimal refusjonskravPrÅr;
    private BigDecimal maksimalRefusjonPrÅr;
    private BigDecimal avkortetRefusjonPrÅr;
    private BigDecimal redusertRefusjonPrÅr;
    private BigDecimal avkortetBrukersAndelPrÅr;
    private BigDecimal redusertBrukersAndelPrÅr;
    private Long dagsatsBruker;
    private Long dagsatsArbeidsgiver;
    private Boolean tidsbegrensetArbeidsforhold;
    private Boolean fastsattAvSaksbehandler;
    private Boolean lagtTilAvSaksbehandler;
    private Long andelNr;
    private Inntektskategori inntektskategori;
    private List<Periode> arbeidsgiverperioder = new ArrayList<>(); //Brukes i beregning for sykepenger
    private boolean erSøktYtelseFor = true; //Default true, brukes i SVP
    private BigDecimal utbetalingsprosentSVP = BigDecimal.valueOf(100);
    private BigDecimal andelsmessigFørGraderingPrAar;

    private BeregningsgrunnlagPrArbeidsforhold() {
    }

    public String getArbeidsgiverId() {
        if (arbeidsforhold.getAktørId() != null) {
            return arbeidsforhold.getAktørId();
        } else if (arbeidsforhold.getOrgnr() != null) {
            return arbeidsforhold.getOrgnr();
        }
        return null;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public boolean erFrilanser() {
        return arbeidsforhold.erFrilanser();
    }

    public Optional<BigDecimal> getNaturalytelseBortfaltPrÅr() {
        return Optional.ofNullable(naturalytelseBortfaltPrÅr);
    }

    public Optional<BigDecimal> getNaturalytelseTilkommetPrÅr() {
        return Optional.ofNullable(naturalytelseTilkommetPrÅr);
    }

    public BigDecimal getBeregnetPrÅr() {
        return beregnetPrÅr;
    }

    public String getBeskrivelse() {
        return (erFrilanser() ? "FL:" : "AT:") + getArbeidsgiverId();
    }

    public BigDecimal getOverstyrtPrÅr() {
        return overstyrtPrÅr;
    }

    public BigDecimal getBruttoPrÅr() {
        return bruttoPrÅr;
    }

    public BigDecimal getFordeltPrÅr() {
        return fordeltPrÅr;
    }

    public Optional<BigDecimal> getBruttoInkludertNaturalytelsePrÅr() {
        if (bruttoPrÅr == null) {
            return Optional.empty();
        }
        BigDecimal bortfaltNaturalytelse = naturalytelseBortfaltPrÅr != null ? naturalytelseBortfaltPrÅr : BigDecimal.ZERO;
        BigDecimal tilkommetNaturalytelse = naturalytelseTilkommetPrÅr != null ? naturalytelseTilkommetPrÅr : BigDecimal.ZERO;
        return Optional.of(bruttoPrÅr.add(bortfaltNaturalytelse).subtract(tilkommetNaturalytelse));
    }

    public BigDecimal getAvkortetPrÅr() {
        return avkortetPrÅr;
    }

    public BigDecimal getRedusertPrÅr() {
        return redusertPrÅr;
    }

    public Periode getBeregningsperiode() {
        return beregningsperiode;
    }

    public Arbeidsforhold getArbeidsforhold() {
        return arbeidsforhold;
    }

    public Optional<BigDecimal> getRefusjonskravPrÅr() {
        return Optional.ofNullable(refusjonskravPrÅr);
    }

    public Optional<BigDecimal> getGradertRefusjonskravPrÅr() {
        return Optional.ofNullable(finnGradert(refusjonskravPrÅr));
    }

    public BigDecimal getMaksimalRefusjonPrÅr() {
        return maksimalRefusjonPrÅr;
    }

    public Long getDagsats() {
        if (dagsatsBruker == null) {
            return dagsatsArbeidsgiver;
        }
        if (dagsatsArbeidsgiver == null) {
            return dagsatsBruker;
        }
        return dagsatsBruker + dagsatsArbeidsgiver;
    }

    public BigDecimal getAvkortetRefusjonPrÅr() {
        return avkortetRefusjonPrÅr;
    }

    public BigDecimal getRedusertRefusjonPrÅr() {
        return redusertRefusjonPrÅr;
    }

    public BigDecimal getAvkortetBrukersAndelPrÅr() {
        return avkortetBrukersAndelPrÅr;
    }

    public BigDecimal getRedusertBrukersAndelPrÅr() {
        return redusertBrukersAndelPrÅr;
    }

    public Long getDagsatsBruker() {
        return dagsatsBruker;
    }

    public Long getDagsatsArbeidsgiver() {
        return dagsatsArbeidsgiver;
    }

    public Boolean getTidsbegrensetArbeidsforhold() {
        return tidsbegrensetArbeidsforhold;
    }

    public Boolean getFastsattAvSaksbehandler() {
        return fastsattAvSaksbehandler;
    }

    public Boolean getLagtTilAvSaksbehandler() {
        return lagtTilAvSaksbehandler;
    }

    public Long getAndelNr() {
        return andelNr;
    }

    public List<Periode> getArbeidsgiverperioder() {
        return Collections.unmodifiableList(arbeidsgiverperioder);
    }

    public BigDecimal getUtbetalingsprosentSVP() {
        return utbetalingsprosentSVP;
    }

    public boolean getErSøktYtelseFor() {
        return erSøktYtelseFor;
    }

    public void setErSøktYtelseFor(boolean erSøktYtelseFor) {
        this.erSøktYtelseFor = erSøktYtelseFor;
    }

    public BigDecimal getAndelsmessigFørGraderingPrAar() {
        return andelsmessigFørGraderingPrAar;
    }

    @Override
    public String toString() {
        return getBeskrivelse();
    }

    private BigDecimal finnGradert(BigDecimal verdi) {
        return verdi == null ? null : verdi.multiply(utbetalingsprosentSVP.scaleByPowerOfTen(-2));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(BeregningsgrunnlagPrArbeidsforhold af) {
        return new Builder(af);
    }
    public static class Builder {

        private BeregningsgrunnlagPrArbeidsforhold mal;

        public Builder() {
            mal = new BeregningsgrunnlagPrArbeidsforhold();
        }

        public Builder(BeregningsgrunnlagPrArbeidsforhold af) {
            mal = af;
        }

        public Builder medArbeidsforhold(Arbeidsforhold arbeidsforhold) {
            mal.arbeidsforhold = arbeidsforhold;
            return this;
        }


        public Builder medAndelsmessigFørGraderingPrAar(BigDecimal andelsmessigFørGraderingPrAar) {
            mal.andelsmessigFørGraderingPrAar = andelsmessigFørGraderingPrAar;
            return this;
        }

        public Builder medBeregnetPrÅr(BigDecimal beregnetPrÅr) {
            mal.beregnetPrÅr = beregnetPrÅr;
            if (mal.overstyrtPrÅr == null && mal.fordeltPrÅr == null) {
                mal.bruttoPrÅr = beregnetPrÅr;
            }
            return this;
        }

        public Builder medOverstyrtPrÅr(BigDecimal overstyrtPrÅr) {
            mal.overstyrtPrÅr = overstyrtPrÅr;
            if (overstyrtPrÅr != null && mal.fordeltPrÅr == null) {
                mal.bruttoPrÅr = overstyrtPrÅr;
            }
            return this;
        }

        public Builder medFordeltPrÅr(BigDecimal fordeltPrÅr) {
            mal.fordeltPrÅr = fordeltPrÅr;
            if (fordeltPrÅr != null) {
                mal.bruttoPrÅr = fordeltPrÅr;
            }
            return this;
        }

        public Builder medAvkortetPrÅr(BigDecimal avkortetPrÅr) {
            mal.avkortetPrÅr = avkortetPrÅr;
            return this;
        }

        public Builder medRedusertPrÅr(BigDecimal redusertPrÅr) {
            mal.redusertPrÅr = redusertPrÅr;
            return this;
        }

        public Builder medNaturalytelseBortfaltPrÅr(BigDecimal naturalytelseBortfaltPrÅr) {
            mal.naturalytelseBortfaltPrÅr = naturalytelseBortfaltPrÅr;
            return this;
        }

        public Builder medNaturalytelseTilkommetPrÅr(BigDecimal naturalytelseTilkommetPrÅr) {
            mal.naturalytelseTilkommetPrÅr = naturalytelseTilkommetPrÅr;
            return this;
        }

        public Builder medBeregningsperiode(Periode beregningsperiode) {
            mal.beregningsperiode = beregningsperiode;
            return this;
        }

        public Builder medRefusjonskravPrÅr(BigDecimal refusjonskravPrÅr) {
            mal.refusjonskravPrÅr = refusjonskravPrÅr;
            return this;
        }

        public Builder medMaksimalRefusjonPrÅr(BigDecimal maksimalRefusjonPrÅr) {
            mal.maksimalRefusjonPrÅr = maksimalRefusjonPrÅr;
            return this;
        }

        public Builder medAvkortetRefusjonPrÅr(BigDecimal avkortetRefusjonPrÅr) {
            mal.avkortetRefusjonPrÅr = avkortetRefusjonPrÅr;
            return this;
        }

        public Builder medRedusertRefusjonPrÅr(BigDecimal redusertRefusjonPrÅr) {
            mal.redusertRefusjonPrÅr = redusertRefusjonPrÅr;
            mal.dagsatsArbeidsgiver = redusertRefusjonPrÅr == null ? null : Math.round(redusertRefusjonPrÅr.doubleValue() / 260);
            return this;
        }

        public Builder medAvkortetBrukersAndelPrÅr(BigDecimal avkortetBrukersAndelPrÅr) {
            mal.avkortetBrukersAndelPrÅr = avkortetBrukersAndelPrÅr;
            return this;
        }

        public Builder medRedusertBrukersAndelPrÅr(BigDecimal redusertBrukersAndelPrÅr) {
            mal.redusertBrukersAndelPrÅr = redusertBrukersAndelPrÅr;
            mal.dagsatsBruker = redusertBrukersAndelPrÅr == null ? null : Math.round(redusertBrukersAndelPrÅr.doubleValue() / 260);
            return this;
        }

        public Builder medErTidsbegrensetArbeidsforhold(Boolean tidsbegrensetArbeidsforhold) {
            mal.tidsbegrensetArbeidsforhold = tidsbegrensetArbeidsforhold;
            return this;
        }

        public Builder medAndelNr(long andelNr) {
            mal.andelNr = andelNr;
            return this;
        }

        public Builder medInntektskategori(Inntektskategori inntektskategori) {
            mal.inntektskategori = inntektskategori;
            return this;
        }

        public Builder medFastsattAvSaksbehandler(Boolean fastsattAvSaksbehandler) {
            mal.fastsattAvSaksbehandler = fastsattAvSaksbehandler;
            return this;
        }

        public Builder medLagtTilAvSaksbehandler(Boolean lagtTilAvSaksbehandler) {
            mal.lagtTilAvSaksbehandler = lagtTilAvSaksbehandler;
            return this;
        }

        //Brukes bare i sykepenger og enhetstest
        public Builder medArbeidsgiverperioder(List<Periode> arbeidsgiverperioder) {
            mal.arbeidsgiverperioder.addAll(arbeidsgiverperioder);
            return this;
        }

        public Builder medUtbetalingsprosentSVP(BigDecimal utbetalingsprosentSVP) {
            mal.utbetalingsprosentSVP = utbetalingsprosentSVP;
            return this;
        }

        public BeregningsgrunnlagPrArbeidsforhold build() {
            verifyStateForBuild();
            return mal;
        }

        private void verifyStateForBuild() {
            Objects.requireNonNull(mal.arbeidsforhold, "arbeidsforhold");
            Objects.requireNonNull(mal.andelNr, "andelNr");
        }
    }
}
