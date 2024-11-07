package no.nav.k9.sak.kontrakt.arbeidsforhold;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
    @NotEmpty
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
    private Set<ArbeidsforholdAksjonspunktÅrsak> aksjonspunktÅrsaker = new LinkedHashSet<>();

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
        return kilder == null ? Set.of() : Collections.unmodifiableSet(kilder);
    }

    public void setKilde(Set<ArbeidsforholdKilde> kilder) {
        this.kilder = kilder;
    }

    public void leggTilKilde(ArbeidsforholdKilde kilde) {
        Objects.requireNonNull(kilde);
        if (this.kilder == null) {
            this.kilder = new LinkedHashSet<>();
        }
        this.kilder.add(kilde);
    }

    public List<PermisjonDto> getPermisjoner() {
        return permisjoner == null ? List.of() : Collections.unmodifiableList(permisjoner);
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
        return ansettelsesPerioder == null ? Set.of() : Collections.unmodifiableSet(ansettelsesPerioder);
    }

    public void setAnsettelsesPerioder(Collection<PeriodeDto> ansettelsesPerioder) {
        this.ansettelsesPerioder = ansettelsesPerioder == null ? null : new LinkedHashSet<>(ansettelsesPerioder);
    }

    public Set<ArbeidsforholdAksjonspunktÅrsak> getAksjonspunktÅrsaker() {
        return aksjonspunktÅrsaker == null ? Set.of() : Collections.unmodifiableSet(aksjonspunktÅrsaker);
    }

    public void setAksjonspunktÅrsaker(Collection<ArbeidsforholdAksjonspunktÅrsak> aksjonspunktÅrsaker) {
        this.aksjonspunktÅrsaker = aksjonspunktÅrsaker == null ? null : new LinkedHashSet<>(aksjonspunktÅrsaker);
    }

    public void leggTilAksjonspunktÅrsak(ArbeidsforholdAksjonspunktÅrsak aksjonspunktÅrsak) {
        Objects.requireNonNull(aksjonspunktÅrsak);
        if (this.aksjonspunktÅrsaker == null) {
            this.aksjonspunktÅrsaker = new LinkedHashSet<>();
        }
        this.aksjonspunktÅrsaker.add(aksjonspunktÅrsak);
    }

    public Set<MottattInntektsmeldingDto> getInntektsmeldinger() {
        return inntektsmeldinger == null ? Set.of() : Collections.unmodifiableSet(inntektsmeldinger);
    }

    public void setInntektsmeldinger(Set<MottattInntektsmeldingDto> inntektsmeldinger) {
        this.inntektsmeldinger = new LinkedHashSet<>(inntektsmeldinger);
    }

    public void leggTilInntektsmelding(MottattInntektsmeldingDto inntektsmelding) {
        Objects.requireNonNull(inntektsmelding);
        if (this.inntektsmeldinger == null) {
            this.inntektsmeldinger = new LinkedHashSet<>();
        }

        this.inntektsmeldinger.add(inntektsmelding);
    }

    public String getYrkestittel() {
        return yrkestittel;
    }

    public void setYrkestittel(String yrkestittel) {
        this.yrkestittel = yrkestittel;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
            + "<id=" + id
            + ", arbeidsgiver=" + arbeidsgiver
            + ", arbeidsforhold=" + arbeidsforhold
            + ", ansettelsesPerioder=" + ansettelsesPerioder
            + ", handlingType=" + handlingType
            + ", aksjonspunktÅrsaker=" + aksjonspunktÅrsaker
            + ", inntektsmeldinger=" + inntektsmeldinger
            + ">";
    }

}
