package no.nav.k9.sak.kontrakt.beregningsresultat;

import java.math.BigDecimal;
import java.time.LocalDate;

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

import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.OrgNummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BeregningsresultatPeriodeAndelDto {

    public static class Builder {
        private AktivitetStatus aktivitetStatus;
        private AktørId aktørId;
        private String arbeidsforholdId;
        private OpptjeningAktivitetType arbeidsforholdType;
        private String arbeidsgiverNavn;
        private OrgNummer arbeidsgiverOrgnr;
        private String eksternArbeidsforholdId;
        private Integer refusjon;
        private LocalDate sisteUtbetalingsdato;
        private BigDecimal stillingsprosent;
        private Integer tilSøker;
        private BigDecimal utbetalingsgrad;
        private UttakDto uttak;

        private Builder() {
        }

        public BeregningsresultatPeriodeAndelDto create() {
            return new BeregningsresultatPeriodeAndelDto(this);
        }

        public Builder medAktivitetstatus(AktivitetStatus aktivitetStatus) {
            this.aktivitetStatus = aktivitetStatus;
            return this;
        }

        public Builder medAktørId(AktørId aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public Builder medArbeidsforholdId(String arbeidsforholdId) {
            this.arbeidsforholdId = arbeidsforholdId;
            return this;
        }

        public Builder medArbeidsforholdType(OpptjeningAktivitetType arbeidsforholdType) {
            this.arbeidsforholdType = arbeidsforholdType;
            return this;
        }

        public Builder medArbeidsgiverNavn(String arbeidsgiverNavn) {
            this.arbeidsgiverNavn = arbeidsgiverNavn;
            return this;
        }

        public Builder medArbeidsgiverOrgnr(OrgNummer arbeidsgiverOrgnr) {
            this.arbeidsgiverOrgnr = arbeidsgiverOrgnr;
            return this;
        }

        public Builder medEksternArbeidsforholdId(String eksternArbeidsforholdId) {
            this.eksternArbeidsforholdId = eksternArbeidsforholdId;
            return this;
        }

        public Builder medRefusjon(Integer refusjon) {
            this.refusjon = refusjon;
            return this;
        }

        public Builder medSisteUtbetalingsdato(LocalDate sisteUtbetalingsdato) {
            this.sisteUtbetalingsdato = sisteUtbetalingsdato;
            return this;
        }

        public Builder medStillingsprosent(BigDecimal stillingsprosent) {
            this.stillingsprosent = stillingsprosent;
            return this;
        }

        public Builder medTilSøker(Integer tilSøker) {
            this.tilSøker = tilSøker;
            return this;
        }

        public Builder medUtbetalingsgrad(BigDecimal utbetalingsgrad) {
            this.utbetalingsgrad = utbetalingsgrad;
            return this;
        }

        public Builder medUttak(UttakDto uttak) {
            this.uttak = uttak;
            return this;
        }
    }

    @JsonProperty(value = "aktivitetStatus")
    @Valid
    private AktivitetStatus aktivitetStatus;

    @JsonProperty(value = "aktørId")
    @Valid
    private AktørId aktørId;

    @JsonProperty(value = "arbeidsforholdId")
    @Size(max = 50)
    @Pattern(regexp = "^[\\p{Graph}\\-\\p{P}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsforholdId;

    @JsonProperty(value = "arbeidsforholdType")
    @Valid
    private OpptjeningAktivitetType arbeidsforholdType;

    @JsonProperty(value = "arbeidsgiverNavn")
    @Size(max = 200)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{P}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsgiverNavn;

    @JsonAlias("orgNummer")
    @JsonProperty(value = "arbeidsgiverOrgnr")
    @Valid
    private OrgNummer arbeidsgiverOrgnr;

    @JsonProperty(value = "eksternArbeidsforholdId")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{P}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String eksternArbeidsforholdId;

    @JsonProperty(value = "refusjon")
    @Min(0)
    @Max(1000000)
    private Integer refusjon;

    @JsonProperty(value = "sisteUtbetalingsdato")
    private LocalDate sisteUtbetalingsdato;

    @JsonProperty(value = "stillingsprosent")
    @DecimalMin("0.00")
    @DecimalMax("500.00")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal stillingsprosent;

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
    @Valid
    private UttakDto uttak;

    private BeregningsresultatPeriodeAndelDto(Builder builder) {
        this.arbeidsgiverNavn = builder.arbeidsgiverNavn;
        this.arbeidsgiverOrgnr = builder.arbeidsgiverOrgnr;
        this.refusjon = builder.refusjon;
        this.tilSoker = builder.tilSøker;
        this.uttak = builder.uttak;
        this.utbetalingsgrad = builder.utbetalingsgrad;
        this.sisteUtbetalingsdato = builder.sisteUtbetalingsdato;
        this.aktivitetStatus = builder.aktivitetStatus;
        this.arbeidsforholdId = builder.arbeidsforholdId;
        this.eksternArbeidsforholdId = builder.eksternArbeidsforholdId;
        this.aktørId = builder.aktørId;
        this.arbeidsforholdType = builder.arbeidsforholdType;
        this.stillingsprosent = builder.stillingsprosent;
    }

    public static Builder build() {
        return new Builder();
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
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

    public String getArbeidsgiverNavn() {
        return arbeidsgiverNavn;
    }

    public OrgNummer getArbeidsgiverOrgnr() {
        return arbeidsgiverOrgnr;
    }

    public String getEksternArbeidsforholdId() {
        return eksternArbeidsforholdId;
    }

    public Integer getRefusjon() {
        return refusjon;
    }

    public LocalDate getSisteUtbetalingsdato() {
        return sisteUtbetalingsdato;
    }

    public BigDecimal getStillingsprosent() {
        return stillingsprosent;
    }

    public Integer getTilSoker() {
        return tilSoker;
    }

    public BigDecimal getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public UttakDto getUttak() {
        return uttak;
    }

    public void setAktivitetStatus(AktivitetStatus aktivitetStatus) {
        this.aktivitetStatus = aktivitetStatus;
    }

    public void setAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    public void setArbeidsforholdId(String arbeidsforholdId) {
        this.arbeidsforholdId = arbeidsforholdId;
    }

    public void setArbeidsforholdType(OpptjeningAktivitetType arbeidsforholdType) {
        this.arbeidsforholdType = arbeidsforholdType;
    }

    public void setArbeidsgiverNavn(String arbeidsgiverNavn) {
        this.arbeidsgiverNavn = arbeidsgiverNavn;
    }

    public void setArbeidsgiverOrgnr(OrgNummer arbeidsgiverOrgnr) {
        this.arbeidsgiverOrgnr = arbeidsgiverOrgnr;
    }

    public void setEksternArbeidsforholdId(String eksternArbeidsforholdId) {
        this.eksternArbeidsforholdId = eksternArbeidsforholdId;
    }

    public void setRefusjon(Integer refusjon) {
        this.refusjon = refusjon;
    }

    public void setSisteUtbetalingsdato(LocalDate sisteUtbetalingsdato) {
        this.sisteUtbetalingsdato = sisteUtbetalingsdato;
    }

    public void setStillingsprosent(BigDecimal stillingsprosent) {
        this.stillingsprosent = stillingsprosent;
    }

    public void setTilSoker(Integer tilSoker) {
        this.tilSoker = tilSoker;
    }

    public void setUtbetalingsgrad(BigDecimal utbetalingsgrad) {
        this.utbetalingsgrad = utbetalingsgrad;
    }

    public void setUttak(UttakDto uttak) {
        this.uttak = uttak;
    }
}
