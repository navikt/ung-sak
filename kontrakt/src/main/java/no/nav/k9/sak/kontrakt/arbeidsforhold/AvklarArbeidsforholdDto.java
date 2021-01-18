package no.nav.k9.sak.kontrakt.arbeidsforhold;

import java.math.BigDecimal;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.sak.kontrakt.Patterns;
import no.nav.k9.sak.typer.Arbeidsgiver;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class AvklarArbeidsforholdDto {

    @Valid
    @NotNull
    @JsonProperty(value = "id", required = true)
    @Pattern(regexp = "^[\\p{Alnum}\\-\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String id;

    @Valid
    @NotNull
    @JsonProperty(value = "arbeidsgiver", required = true)
    private Arbeidsgiver arbeidsgiver;

    @Valid
    @NotNull
    @JsonProperty(value = "arbeidsforhold", required = true)
    private ArbeidsforholdIdDto arbeidsforhold;

    @JsonProperty(value = "handlingType", required = true)
    @NotNull
    @Valid
    private ArbeidsforholdHandlingType handlingType = ArbeidsforholdHandlingType.BRUK;

    @JsonProperty(value = "navn")
    @Size(max = 100)
    @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String navn;

    @JsonProperty(value = "perioder")
    @Size
    private Set<PeriodeDto> ansettelsesPerioder;

    @JsonProperty(value = "stillingsprosent")
    @DecimalMin(value = "0.00")
    @DecimalMax(value = "500.00")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal stillingsprosent;

    @JsonProperty(value = "yrkestittel")
    @Size(max = 400)
    @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String yrkestittel = "Ukjent";

    @JsonProperty(value = "begrunnelse", required = true)
    @Size(max = 4000)
    @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String begrunnelse;

    public AvklarArbeidsforholdDto() {
        //
    }

    @JsonCreator
    public AvklarArbeidsforholdDto(@JsonProperty(value = "id", required = true) @Valid @NotNull @Pattern(regexp = "^[\\p{Alnum}\\-\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String id,
                                   @JsonProperty(value = "arbeidsgiver", required = true) @Valid @NotNull Arbeidsgiver arbeidsgiver,
                                   @JsonProperty(value = "arbeidsforhold", required = true) @Valid @NotNull ArbeidsforholdIdDto arbeidsforhold,
                                   @JsonProperty(value = "handlingType", required = true) @Valid @NotNull ArbeidsforholdHandlingType handlingType,
                                   @JsonProperty(value = "perioder") @Valid @Size Set<PeriodeDto> ansettelsesPerioder,
                                   @JsonProperty(value = "navn") @Valid @Size(max = 100) @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String navn,
                                   @JsonProperty(value = "stillingsprosent") @DecimalMin(value = "0.00") @DecimalMax(value = "500.00") @Digits(integer = 3, fraction = 2) BigDecimal stillingsprosent,
                                   @JsonProperty(value = "yrkestittel") @Valid @Size(max = 400) @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String yrkestittel,
                                   @JsonProperty(value = "begrunnelse", required = true) @Valid @Size(max = 4000) @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String begrunnelse) {
        this.id = id;
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforhold = arbeidsforhold;
        this.handlingType = handlingType;
        this.ansettelsesPerioder = ansettelsesPerioder;
        this.navn = navn;
        this.stillingsprosent = stillingsprosent;
        this.yrkestittel = yrkestittel;
        this.begrunnelse = begrunnelse;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public void setArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

    public ArbeidsforholdIdDto getArbeidsforhold() {
        return arbeidsforhold;
    }

    public void setArbeidsforhold(ArbeidsforholdIdDto arbeidsforhold) {
        this.arbeidsforhold = arbeidsforhold;
    }

    public ArbeidsforholdHandlingType getHandlingType() {
        return handlingType;
    }

    public void setHandlingType(ArbeidsforholdHandlingType handlingType) {
        this.handlingType = handlingType;
    }

    public Set<PeriodeDto> getAnsettelsesPerioder() {
        return ansettelsesPerioder;
    }

    public void setAnsettelsesPerioder(Set<PeriodeDto> ansettelsesPerioder) {
        this.ansettelsesPerioder = ansettelsesPerioder;
    }

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public BigDecimal getStillingsprosent() {
        return stillingsprosent;
    }

    public void setStillingsprosent(BigDecimal stillingsprosent) {
        this.stillingsprosent = stillingsprosent;
    }

    public String getYrkestittel() {
        return yrkestittel;
    }

    public void setYrkestittel(String yrkestittel) {
        this.yrkestittel = yrkestittel;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }
}
