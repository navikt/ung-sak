package no.nav.k9.sak.kontrakt.beregningsresultat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.kontrakt.arbeidsforhold.ArbeidsgiverDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.OrgNummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class TilkjentYtelseAndelDto {

    @JsonProperty(value = "erBrukerMottaker")
    @Valid
    private Boolean erBrukerMottaker;
    @JsonProperty(value = "inntektskategori", required = true)
    @Valid
    private Inntektskategori inntektskategori;
    @JsonProperty(value = "aktørId")
    @Valid
    private AktørId aktørId;
    @JsonProperty(value = "arbeidsforholdId")
    @Size(max = 50)
    @Pattern(regexp = "^[\\p{Graph}\\-\\p{P}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String arbeidsforholdId;
    @JsonProperty(value = "arbeidsgiver")
    @Valid
    private ArbeidsgiverDto arbeidsgiver;
    @JsonProperty(value = "arbeidsforholdType")
    @Valid
    private OpptjeningAktivitetType arbeidsforholdType;
    @JsonAlias("orgNummer")
    @JsonProperty(value = "arbeidsgiverOrgnr")
    @Valid
    private OrgNummer arbeidsgiverOrgnr;
    @JsonProperty(value = "stillingsprosent")
    @DecimalMin("0.00")
    @DecimalMax("500.00")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal stillingsprosent;
    @JsonProperty(value = "refusjon")
    @Min(0)
    @Max(1000000)
    private Integer refusjon;
    @JsonProperty(value = "tilSoker")
    @Min(0)
    @Max(1000000)
    private Integer tilSoker;
    @JsonProperty(value = "utbetalingsgrad")
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal utbetalingsgrad;
    @JsonProperty(value = "uttak")
    @Size(max = 100)
    @Valid
    private List<UttakDto> uttak;

    public TilkjentYtelseAndelDto() {
        // Deserialisering av JSON
    }

    private TilkjentYtelseAndelDto(Builder builder) {
        this.arbeidsgiverOrgnr = builder.arbeidsgiverOrgnr;
        this.tilSoker = builder.tilSoker;
        this.refusjon = builder.refusjon;
        this.uttak = builder.uttak;
        this.utbetalingsgrad = builder.utbetalingsgrad;
        this.arbeidsforholdId = builder.arbeidsforholdId;
        this.aktørId = builder.aktørId;
        this.arbeidsforholdType = builder.arbeidsforholdType;
        this.stillingsprosent = builder.stillingsprosent;
        this.arbeidsgiver = builder.arbeidsgiver;
        this.inntektskategori = Objects.requireNonNull(builder.inntektskategori, "inntektskategori");
        this.erBrukerMottaker = Objects.requireNonNull(builder.erBrukerMottaker, "erBrukerMottaker");
    }

    public static Builder build() {
        return new Builder();
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public String getArbeidsforholdId() {
        return arbeidsforholdId;
    }

    public OpptjeningAktivitetType getArbeidsforholdType() {
        return arbeidsforholdType;
    }

    public ArbeidsgiverDto getArbeidsgiver() {
        return arbeidsgiver;
    }

    public OrgNummer getArbeidsgiverOrgnr() {
        return arbeidsgiverOrgnr;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public BigDecimal getStillingsprosent() {
        return stillingsprosent;
    }

    public Integer getRefusjon() {
        return refusjon;
    }

    public Integer getTilSoker() {
        return tilSoker;
    }

    public BigDecimal getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public List<UttakDto> getUttak() {
        return uttak;
    }

    public Boolean getErBrukerMottaker() {
        return erBrukerMottaker;
    }

    public static class Builder {
        private AktørId aktørId;
        private String arbeidsforholdId;
        private OpptjeningAktivitetType arbeidsforholdType;
        private ArbeidsgiverDto arbeidsgiver;
        private OrgNummer arbeidsgiverOrgnr;
        private BigDecimal stillingsprosent;
        private Integer refusjon;
        private Integer tilSoker;
        private BigDecimal utbetalingsgrad;
        private List<UttakDto> uttak;
        private Inntektskategori inntektskategori;
        private boolean erBrukerMottaker;

        private Builder() {
        }

        public TilkjentYtelseAndelDto create() {
            return new TilkjentYtelseAndelDto(this);
        }

        public Builder medAktørId(AktørId aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public Builder medArbeidsforholdId(String arbeidsforholdId) {
            this.arbeidsforholdId = arbeidsforholdId;
            return this;
        }

        public Builder medArbeidsgiver(ArbeidsgiverDto arbeidsgiver) {
            this.arbeidsgiver = arbeidsgiver;
            return this;
        }

        public Builder medArbeidsforholdType(OpptjeningAktivitetType arbeidsforholdType) {
            this.arbeidsforholdType = arbeidsforholdType;
            return this;
        }

        public Builder medArbeidsgiverOrgnr(OrgNummer arbeidsgiverOrgnr) {
            this.arbeidsgiverOrgnr = arbeidsgiverOrgnr;
            return this;
        }

        public Builder medStillingsprosent(BigDecimal stillingsprosent) {
            this.stillingsprosent = stillingsprosent;
            return this;
        }

        public Builder medRefusjon(Integer refusjon) {
            this.refusjon = refusjon;
            return this;
        }

        public Builder medTilSoker(Integer tilSoker) {
            this.tilSoker = tilSoker;
            return this;
        }

        public Builder medUtbetalingsgrad(BigDecimal utbetalingsgrad) {
            this.utbetalingsgrad = utbetalingsgrad;
            return this;
        }

        public Builder medUttak(List<UttakDto> uttak) {
            this.uttak = uttak;
            return this;
        }

        public Builder medInntektskategori(Inntektskategori inntektskategori) {
            this.inntektskategori = inntektskategori;
            return this;
        }

        public Builder medErBrukerMottaker(boolean erBrukerMottaker) {
            this.erBrukerMottaker = erBrukerMottaker;
            return this;
        }
    }
}
