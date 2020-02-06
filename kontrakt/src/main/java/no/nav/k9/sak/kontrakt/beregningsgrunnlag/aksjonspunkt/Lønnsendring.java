package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class Lønnsendring {

    @JsonProperty(value = "gammelArbeidsinntekt")
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer gammelArbeidsinntekt;

    @JsonProperty(value = "nyArbeidsinntekt")
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer nyArbeidsinntekt;

    @JsonProperty(value = "nyArbeidsinntektPrÅr")
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer nyArbeidsinntektPrÅr;

    @JsonProperty(value = "gammelInntektskategori")
    @Valid
    private Inntektskategori gammelInntektskategori;

    @JsonProperty(value = "nyInntektskategori")
    @Valid
    private Inntektskategori nyInntektskategori;

    @JsonProperty(value = "gammelRefusjonPrÅr")
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer gammelRefusjonPrÅr;

    @JsonProperty(value = "nyRefusjonPrÅr")
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer nyRefusjonPrÅr;

    @JsonProperty(value = "nyTotalRefusjonPrÅr")
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer nyTotalRefusjonPrÅr;

    @JsonProperty(value = "nyAndel")
    private boolean nyAndel;

    @JsonProperty(value = "gammelArbeidsinntektPrÅr")
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer gammelArbeidsinntektPrÅr;

    @JsonProperty(value = "arbeidsgiver")
    @Valid
    private Arbeidsgiver arbeidsgiver;

    @JsonProperty(value = "arbeidsforholdRef")
    @Valid
    private InternArbeidsforholdRef arbeidsforholdRef;

    @JsonProperty(value = "aktivitetStatus")
    @Valid
    private AktivitetStatus aktivitetStatus;

    private Lønnsendring() {
    }

    public void setNyAndel(boolean nyAndel) {
        this.nyAndel = nyAndel;
    }

    public boolean isNyAndel() {
        return nyAndel;
    }

    public Integer getGammelArbeidsinntekt() {
        return gammelArbeidsinntekt;
    }

    public Integer getNyArbeidsinntekt() {
        return nyArbeidsinntekt;
    }

    public Integer getNyArbeidsinntektPrÅr() {
        return nyArbeidsinntektPrÅr;
    }

    public Integer getGammelArbeidsinntektPrÅr() {
        return gammelArbeidsinntektPrÅr;
    }

    public Inntektskategori getGammelInntektskategori() {
        return gammelInntektskategori;
    }

    public Inntektskategori getNyInntektskategori() {
        return nyInntektskategori;
    }

    public Integer getGammelRefusjonPrÅr() {
        return gammelRefusjonPrÅr;
    }

    public Integer getNyRefusjonPrÅr() {
        return nyRefusjonPrÅr;
    }

    public Integer getNyTotalRefusjonPrÅr() {
        return nyTotalRefusjonPrÅr;
    }

    public Optional<Arbeidsgiver> getArbeidsgiver() {
        return Optional.ofNullable(arbeidsgiver);
    }

    public Optional<InternArbeidsforholdRef> getArbeidsforholdRef() {
        return Optional.ofNullable(arbeidsforholdRef);
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public static class Builder {
        private Lønnsendring lønnsendringMal;

        public Builder() {
            lønnsendringMal = new Lønnsendring();
        }

        Builder(Lønnsendring lønnsendring) {
            lønnsendringMal = lønnsendring;
        }

        public static Builder ny() {
            return new Builder();
        }

        public Builder medNyAndel(boolean nyAndel) {
            lønnsendringMal.nyAndel = nyAndel;
            return this;
        }

        public Builder medGammelArbeidsinntekt(Integer gammelArbeidsinntekt) {
            lønnsendringMal.gammelArbeidsinntekt = gammelArbeidsinntekt;
            return this;
        }

        public Builder medGammelArbeidsinntektPrÅr(Integer gammelArbeidsinntektPrÅr) {
            lønnsendringMal.gammelArbeidsinntektPrÅr = gammelArbeidsinntektPrÅr;
            return this;
        }

        public Builder medNyArbeidsinntekt(Integer nyArbeidsinntekt) {
            lønnsendringMal.nyArbeidsinntekt = nyArbeidsinntekt;
            return this;
        }

        public Builder medNyArbeidsinntektPrÅr(Integer nyArbeidsinntektPrÅr) {
            lønnsendringMal.nyArbeidsinntektPrÅr = nyArbeidsinntektPrÅr;
            return this;
        }

        public Builder medNyInntektskategori(Inntektskategori nyInntektskategori) {
            lønnsendringMal.nyInntektskategori = nyInntektskategori;
            return this;
        }

        public Builder medGammelInntektskategori(Inntektskategori inntektskategori) {
            lønnsendringMal.gammelInntektskategori = inntektskategori;
            return this;
        }

        public Builder medGammelRefusjonPrÅr(Integer refusjon) {
            lønnsendringMal.gammelRefusjonPrÅr = refusjon;
            return this;
        }

        public Builder medNyRefusjonPrÅr(Integer refusjon) {
            lønnsendringMal.nyRefusjonPrÅr = refusjon;
            return this;
        }

        public Builder medNyTotalRefusjonPrÅr(Integer refusjon) {
            lønnsendringMal.nyTotalRefusjonPrÅr = refusjon;
            return this;
        }

        public Builder medArbeidsforholdRef(InternArbeidsforholdRef arbeidsforholdRef) {
            lønnsendringMal.arbeidsforholdRef = arbeidsforholdRef;
            return this;
        }

        public Builder medArbeidsgiver(Arbeidsgiver arbeidsgiver) {
            lønnsendringMal.arbeidsgiver = arbeidsgiver;
            return this;
        }

        public Builder medAktivitetStatus(AktivitetStatus aktivitetStatus) {
            lønnsendringMal.aktivitetStatus = aktivitetStatus;
            return this;
        }

        public Lønnsendring build() {
            return lønnsendringMal;
        }

    }

}
