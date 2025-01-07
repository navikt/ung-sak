package no.nav.ung.sak.kontrakt.beregningsresultat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.ung.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.ung.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.ung.sak.kontrakt.arbeidsforhold.ArbeidsgiverDto;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.OrgNummer;
import no.nav.ung.sak.typer.PersonIdent;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BeregningsresultatPeriodeAndelDto {

    @Deprecated(forRemoval = true)
    @JsonProperty(value = "aktivitetStatus")
    @Valid
    private AktivitetStatus aktivitetStatus = AktivitetStatus.ARBEIDSTAKER;

    @Deprecated(forRemoval = true)
    @JsonProperty(value = "inntektskategori", required = true)
    @Valid
    private Inntektskategori inntektskategori = Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER;

    @Deprecated(forRemoval = true)
    @JsonProperty(value = "aktørId")
    @Valid
    private AktørId aktørId;

    @Deprecated(forRemoval = true)
    @JsonProperty(value = "arbeidsforholdId")
    @Size(max = 50)
    @Pattern(regexp = "^[\\p{Graph}\\-\\p{P}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String arbeidsforholdId;

    @Deprecated(forRemoval = true)
    @JsonProperty(value = "arbeidsgiver")
    @Valid
    private ArbeidsgiverDto arbeidsgiver;

    @Deprecated(forRemoval = true)
    @JsonProperty(value = "arbeidsforholdType")
    @Valid
    private OpptjeningAktivitetType arbeidsforholdType;

    @Deprecated(forRemoval = true)
    @JsonProperty(value = "arbeidsgiverNavn")
    @Size(max = 200)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{P}\\p{L}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String arbeidsgiverNavn;

    @Deprecated(forRemoval = true)
    @JsonAlias("orgNummer")
    @JsonProperty(value = "arbeidsgiverOrgnr")
    @Valid
    private OrgNummer arbeidsgiverOrgnr;

    @Deprecated(forRemoval = true)
    @JsonProperty(value = "arbeidsgiverPersonIdent")
    @Valid
    private PersonIdent arbeidsgiverPersonIdent;

    @Deprecated(forRemoval = true)
    @JsonProperty(value = "eksternArbeidsforholdId")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{P}\\p{L}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String eksternArbeidsforholdId;

    @Deprecated(forRemoval = true)
    @JsonProperty(value = "refusjon")
    @Min(0)
    @Max(1000000)
    private Integer refusjon;

    @JsonProperty(value = "sisteUtbetalingsdato")
    private LocalDate sisteUtbetalingsdato;

    @Deprecated(forRemoval = true)
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

    @Deprecated(forRemoval = true)
    @JsonProperty(value = "utbetalingsgradOppdragForBruker")
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal utbetalingsgradOppdragForBruker;

    @Deprecated(forRemoval = true)
    @JsonProperty(value = "utbetalingsgradOppdragForRefusjon")
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal utbetalingsgradOppdragForRefusjon = BigDecimal.ZERO;

    @JsonProperty(value = "uttak")
    @Size(max = 100)
    @Valid
    private List<UttakDto> uttak;

    public BeregningsresultatPeriodeAndelDto() {
        // Deserialisering av JSON
    }

    private BeregningsresultatPeriodeAndelDto(Builder builder) {
        this.tilSoker = builder.tilSøker;
        this.uttak = builder.uttak;
        this.utbetalingsgrad = builder.utbetalingsgrad;
        this.utbetalingsgradOppdragForBruker = builder.utbetalingsgrad;
        this.sisteUtbetalingsdato = builder.sisteUtbetalingsdato;
    }

    public static Builder build() {
        return new Builder();
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public void setAktivitetStatus(AktivitetStatus aktivitetStatus) {
        this.aktivitetStatus = aktivitetStatus;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public void setAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    public String getArbeidsforholdId() {
        return arbeidsforholdId;
    }

    public void setArbeidsforholdId(String arbeidsforholdId) {
        this.arbeidsforholdId = arbeidsforholdId;
    }

    public OpptjeningAktivitetType getArbeidsforholdType() {
        return arbeidsforholdType;
    }

    public void setArbeidsforholdType(OpptjeningAktivitetType arbeidsforholdType) {
        this.arbeidsforholdType = arbeidsforholdType;
    }

    public ArbeidsgiverDto getArbeidsgiver() {
        return arbeidsgiver;
    }

    public void setArbeidsgiver(ArbeidsgiverDto arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

    public String getArbeidsgiverNavn() {
        return arbeidsgiverNavn;
    }

    public void setArbeidsgiverNavn(String arbeidsgiverNavn) {
        this.arbeidsgiverNavn = arbeidsgiverNavn;
    }

    public OrgNummer getArbeidsgiverOrgnr() {
        return arbeidsgiverOrgnr;
    }

    public void setArbeidsgiverOrgnr(OrgNummer arbeidsgiverOrgnr) {
        this.arbeidsgiverOrgnr = arbeidsgiverOrgnr;
    }

    public String getEksternArbeidsforholdId() {
        return eksternArbeidsforholdId;
    }

    public void setEksternArbeidsforholdId(String eksternArbeidsforholdId) {
        this.eksternArbeidsforholdId = eksternArbeidsforholdId;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public void setInntektskategori(Inntektskategori inntektskategori) {
        this.inntektskategori = inntektskategori;
    }

    public Integer getRefusjon() {
        return refusjon;
    }

    public void setRefusjon(Integer refusjon) {
        this.refusjon = refusjon;
    }

    public LocalDate getSisteUtbetalingsdato() {
        return sisteUtbetalingsdato;
    }

    public void setSisteUtbetalingsdato(LocalDate sisteUtbetalingsdato) {
        this.sisteUtbetalingsdato = sisteUtbetalingsdato;
    }

    public BigDecimal getStillingsprosent() {
        return stillingsprosent;
    }

    public void setStillingsprosent(BigDecimal stillingsprosent) {
        this.stillingsprosent = stillingsprosent;
    }

    public Integer getTilSoker() {
        return tilSoker;
    }

    public void setTilSoker(Integer tilSoker) {
        this.tilSoker = tilSoker;
    }

    public BigDecimal getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public void setUtbetalingsgrad(BigDecimal utbetalingsgrad) {
        this.utbetalingsgrad = utbetalingsgrad;
    }

    public BigDecimal getUtbetalingsgradOppdragForBruker() {
        return utbetalingsgradOppdragForBruker;
    }

    public void setUtbetalingsgradOppdragForBruker(BigDecimal utbetalingsgradOppdragForBruker) {
        this.utbetalingsgradOppdragForBruker = utbetalingsgradOppdragForBruker;
    }

    public BigDecimal getUtbetalingsgradOppdragForRefusjon() {
        return utbetalingsgradOppdragForRefusjon;
    }

    public void setUtbetalingsgradOppdragForRefusjon(BigDecimal utbetalingsgradOppdragForRefusjon) {
        this.utbetalingsgradOppdragForRefusjon = utbetalingsgradOppdragForRefusjon;
    }

    public List<UttakDto> getUttak() {
        return uttak;
    }

    public void setUttak(List<UttakDto> uttak) {
        this.uttak = uttak;
    }

    public static class Builder {

        private LocalDate sisteUtbetalingsdato;
        private Integer tilSøker;
        private BigDecimal utbetalingsgrad;
        private List<UttakDto> uttak;

        private Builder() {
        }

        public BeregningsresultatPeriodeAndelDto create() {
            return new BeregningsresultatPeriodeAndelDto(this);
        }



        public Builder medSisteUtbetalingsdato(LocalDate sisteUtbetalingsdato) {
            this.sisteUtbetalingsdato = sisteUtbetalingsdato;
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

        public Builder medUttak(List<UttakDto> uttak) {
            this.uttak = uttak;
            return this;
        }

    }
}
