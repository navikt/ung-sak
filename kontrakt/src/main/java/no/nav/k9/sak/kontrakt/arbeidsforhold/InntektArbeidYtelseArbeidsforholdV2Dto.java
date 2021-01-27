package no.nav.k9.sak.kontrakt.arbeidsforhold;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdKilde;
import no.nav.k9.sak.kontrakt.Patterns;
import no.nav.k9.sak.typer.Arbeidsgiver;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class InntektArbeidYtelseArbeidsforholdV2Dto {

    @JsonProperty(value = "id")
    @Pattern(regexp = "^[\\p{Alnum}\\-\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String id;

    @NotNull
    @Valid
    @JsonProperty(value = "arbeidsgiver")
    private Arbeidsgiver arbeidsgiver;

    @Valid
    @JsonProperty(value = "arbeidsforhold")
    private ArbeidsforholdIdDto arbeidsforhold;

    @JsonProperty(value = "yrkestittel")
    @Size(max = 400)
    @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String yrkestittel = "Ukjent";

    @JsonProperty(value = "begrunnelse")
    @Size(max = 400)
    @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String begrunnelse;

    @JsonProperty(value = "perioder")
    private Set<PeriodeDto> ansettelsesPerioder;

    @JsonProperty(value = "handlingType", required = true)
    @NotNull
    @Valid
    private ArbeidsforholdHandlingType handlingType = ArbeidsforholdHandlingType.BRUK;

    @JsonProperty(value = "kilde", required = true)
    @NotNull
    @Valid
    private Set<ArbeidsforholdKilde> kilder;

    @JsonProperty(value = "permisjoner")
    @Size(max = 100)
    @Valid
    private List<PermisjonDto> permisjoner;

    @JsonProperty(value = "stillingsprosent")
    @DecimalMin(value = "0.00")
    @DecimalMax(value = "500.00")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal stillingsprosent;

    @JsonProperty(value = "aksjonspunktÅrsaker")
    private Set<ArbeidsforholdAksjonspunktÅrsak> aksjonspunktÅrsaker = new HashSet<>();

    @JsonProperty(value = "inntektsmeldinger")
    private Set<MottattInntektsmeldingDto> inntektsmeldinger;

    public InntektArbeidYtelseArbeidsforholdV2Dto() {
        //
    }

    public InntektArbeidYtelseArbeidsforholdV2Dto(Arbeidsgiver arbeidsgiver, ArbeidsforholdIdDto arbeidsforhold) {
        this.arbeidsgiver = Objects.requireNonNull(arbeidsgiver, "arbeidsgiver");
        this.arbeidsforhold = arbeidsforhold;
        this.id = arbeidsgiver.getIdentifikator() + "-" + (arbeidsforhold != null && arbeidsforhold.getInternArbeidsforholdId() != null ? arbeidsforhold.getInternArbeidsforholdId().toString() : null);
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public ArbeidsforholdHandlingType getHandlingType() {
        return handlingType;
    }

    public void setHandlingType(ArbeidsforholdHandlingType handlingType) {
        this.handlingType = handlingType;
    }

    public String getId() {
        return id;
    }

    public Set<ArbeidsforholdKilde> getKilde() {
        return kilder;
    }

    public void setKilde(Set<ArbeidsforholdKilde> kilder) {
        this.kilder = kilder;
    }

    public void leggTilKilde(ArbeidsforholdKilde kilde) {
        Objects.requireNonNull(kilde);
        if (this.kilder == null) {
            this.kilder = new HashSet<>();
        }
        this.kilder.add(kilde);
    }

    public List<PermisjonDto> getPermisjoner() {
        return permisjoner;
    }

    public void setPermisjoner(List<PermisjonDto> permisjoner) {
        this.permisjoner = permisjoner;
    }

    public BigDecimal getStillingsprosent() {
        return stillingsprosent;
    }

    public void setStillingsprosent(BigDecimal stillingsprosent) {
        this.stillingsprosent = stillingsprosent;
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

    public Set<PeriodeDto> getAnsettelsesPerioder() {
        return ansettelsesPerioder;
    }

    public void setAnsettelsesPerioder(Set<PeriodeDto> ansettelsesPerioder) {
        this.ansettelsesPerioder = ansettelsesPerioder;
    }

    public Set<ArbeidsforholdAksjonspunktÅrsak> getAksjonspunktÅrsaker() {
        return aksjonspunktÅrsaker;
    }

    public void setAksjonspunktÅrsaker(Set<ArbeidsforholdAksjonspunktÅrsak> aksjonspunktÅrsaker) {
        this.aksjonspunktÅrsaker = aksjonspunktÅrsaker;
    }

    public void leggTilAksjonspunktÅrsak(ArbeidsforholdAksjonspunktÅrsak aksjonspunktÅrsak) {
        Objects.requireNonNull(aksjonspunktÅrsak);
        if (this.aksjonspunktÅrsaker == null) {
            this.aksjonspunktÅrsaker = new HashSet<>();
        }
        this.aksjonspunktÅrsaker.add(aksjonspunktÅrsak);
    }

    public Set<MottattInntektsmeldingDto> getInntektsmeldinger() {
        return inntektsmeldinger;
    }

    public void setInntektsmeldinger(Set<MottattInntektsmeldingDto> inntektsmeldinger) {
        this.inntektsmeldinger = inntektsmeldinger;
    }

    public void leggTilInntektsmelding(MottattInntektsmeldingDto inntektsmelding) {
        Objects.requireNonNull(inntektsmelding);
        if (this.inntektsmeldinger == null) {
            this.inntektsmeldinger = new HashSet<>();
        }
        this.inntektsmeldinger.add(inntektsmelding);
    }

    public String getYrkestittel() {
        return yrkestittel;
    }

    public void setYrkestittel(String yrkestittel) {
        this.yrkestittel = yrkestittel;
    }

}
