package no.nav.k9.sak.kontrakt.arbeidsforhold;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class AvklarArbeidsforholdDto {

    @JsonProperty(value = "arbeidsforholdId")
    @Valid
    private UUID arbeidsforholdId;

    // For mottak fra GUI (orgnr for virksomhet, og aktørId for person-arbeidsgiver)
    @JsonProperty(value = "arbeidsgiverIdentifikator")
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String arbeidsgiverIdentifikator;

    @JsonProperty(value = "basertPaInntektsmelding")
    private boolean basertPaInntektsmelding;

    @JsonProperty(value = "begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String begrunnelse;

    @JsonProperty(value = "brukArbeidsforholdet")
    private Boolean brukArbeidsforholdet;

    @JsonProperty(value = "brukPermisjon")
    private Boolean brukPermisjon;

    @JsonProperty(value = "erNyttArbeidsforhold")
    private Boolean erNyttArbeidsforhold;

    @JsonProperty(value = "erstatterArbeidsforholdId")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String erstatterArbeidsforholdId;

    @JsonProperty(value = "fomDato", required = true)
    @NotNull
    private LocalDate fomDato;

    @JsonProperty(value = "fortsettBehandlingUtenInntektsmelding")
    private Boolean fortsettBehandlingUtenInntektsmelding;

    @JsonProperty(value = "id")
    @Pattern(regexp = "^[\\p{Alnum}\\-\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String id;

    @JsonProperty(value = "inntektMedTilBeregningsgrunnlag")
    private Boolean inntektMedTilBeregningsgrunnlag;

    @JsonProperty(value = "lagtTilAvSaksbehandler")
    private boolean lagtTilAvSaksbehandler;

    @JsonProperty(value = "mottattDatoInntektsmelding")
    private LocalDate mottattDatoInntektsmelding;

    @JsonProperty(value = "navn")
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String navn;

    @JsonProperty(value = "overstyrtTom")
    private LocalDate overstyrtTom;

    @JsonProperty(value = "permisjoner")
    @Size(max = 100)
    @Valid
    private List<PermisjonDto> permisjoner;

    @JsonProperty(value = "stillingsprosent")
    @DecimalMin(value = "0.00")
    @DecimalMax(value = "500.00")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal stillingsprosent;

    @JsonProperty(value = "tomDato")
    private LocalDate tomDato;

    public AvklarArbeidsforholdDto() {
        //
    }

    public UUID getArbeidsforholdId() {
        return arbeidsforholdId;
    }

    public String getArbeidsgiverIdentifikator() {
        return arbeidsgiverIdentifikator;
    }

    public boolean getBasertPaInntektsmelding() {
        return basertPaInntektsmelding;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public Boolean getBrukArbeidsforholdet() {
        return brukArbeidsforholdet;
    }

    public Boolean getBrukPermisjon() {
        return brukPermisjon;
    }

    public Boolean getErNyttArbeidsforhold() {
        return erNyttArbeidsforhold;
    }

    public String getErstatterArbeidsforholdId() {
        return erstatterArbeidsforholdId;
    }

    public LocalDate getFomDato() {
        return fomDato;
    }

    public Boolean getFortsettBehandlingUtenInntektsmelding() {
        return fortsettBehandlingUtenInntektsmelding;
    }

    public String getId() {
        return id;
    }

    public Boolean getInntektMedTilBeregningsgrunnlag() {
        return inntektMedTilBeregningsgrunnlag;
    }

    public boolean getLagtTilAvSaksbehandler() {
        return lagtTilAvSaksbehandler;
    }

    public LocalDate getMottattDatoInntektsmelding() {
        return mottattDatoInntektsmelding;
    }

    public String getNavn() {
        return navn;
    }

    public LocalDate getOverstyrtTom() {
        return overstyrtTom;
    }

    public List<PermisjonDto> getPermisjoner() {
        return permisjoner;
    }

    public BigDecimal getStillingsprosent() {
        return stillingsprosent;
    }

    public LocalDate getTomDato() {
        return tomDato;
    }

    public void setArbeidsforholdId(String arbeidsforholdId) {
        this.arbeidsforholdId = arbeidsforholdId == null ? null : UUID.fromString(arbeidsforholdId);
    }

    public void setArbeidsforholdId(UUID arbeidsforholdId) {
        this.arbeidsforholdId = arbeidsforholdId;
    }

    public void setArbeidsgiverIdentifikator(String arbeidsgiverIdentifikator) {
        this.arbeidsgiverIdentifikator = arbeidsgiverIdentifikator;
    }

    public void setBasertPaInntektsmelding(boolean basertPaInntektsmelding) {
        this.basertPaInntektsmelding = basertPaInntektsmelding;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public void setBrukArbeidsforholdet(Boolean brukArbeidsforholdet) {
        this.brukArbeidsforholdet = brukArbeidsforholdet;
    }

    public void setBrukPermisjon(Boolean brukPermisjon) {
        this.brukPermisjon = brukPermisjon;
    }

    public void setErNyttArbeidsforhold(Boolean erNyttArbeidsforhold) {
        this.erNyttArbeidsforhold = erNyttArbeidsforhold;
    }

    public void setErstatterArbeidsforholdId(String erstatterArbeidsforholdId) {
        this.erstatterArbeidsforholdId = erstatterArbeidsforholdId;
    }

    public void setFomDato(LocalDate fomDato) {
        this.fomDato = fomDato;
    }

    public void setFortsettBehandlingUtenInntektsmelding(Boolean fortsettBehandlingUtenInntektsmelding) {
        this.fortsettBehandlingUtenInntektsmelding = fortsettBehandlingUtenInntektsmelding;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setInntektMedTilBeregningsgrunnlag(Boolean inntektMedTilBeregningsgrunnlag) {
        this.inntektMedTilBeregningsgrunnlag = inntektMedTilBeregningsgrunnlag;
    }

    public void setLagtTilAvSaksbehandler(boolean lagtTilAvSaksbehandler) {
        this.lagtTilAvSaksbehandler = lagtTilAvSaksbehandler;
    }

    public void setMottattDatoInntektsmelding(LocalDate mottattDatoInntektsmelding) {
        this.mottattDatoInntektsmelding = mottattDatoInntektsmelding;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public void setOverstyrtTom(LocalDate overstyrtTom) {
        this.overstyrtTom = overstyrtTom;
    }

    public void setPermisjoner(List<PermisjonDto> permisjoner) {
        this.permisjoner = permisjoner;
    }

    public void setStillingsprosent(BigDecimal stillingsprosent) {
        this.stillingsprosent = stillingsprosent;
    }

    public void setTomDato(LocalDate tomDato) {
        this.tomDato = tomDato;
    }

}
