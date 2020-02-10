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

    @JsonProperty(value = "id")
    @Pattern(regexp = "^[\\p{Alnum}\\-\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String id;

    @JsonProperty(value = "navn")
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String navn;

    // For mottak fra GUI (orgnr for virksomhet, og aktørId for person-arbeidsgiver)
    @JsonProperty(value = "arbeidsgiverIdentifikator")
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsgiverIdentifikator;

    @JsonProperty(value = "arbeidsforholdId")
    @Valid
    private UUID arbeidsforholdId;

    @JsonProperty(value = "begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String begrunnelse;

    @JsonProperty(value = "erstatterArbeidsforholdId")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String erstatterArbeidsforholdId;

    @JsonProperty(value = "stillingsprosent")
    @DecimalMin(value = "0.00")
    @DecimalMax(value = "500.00")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal stillingsprosent;

    @JsonProperty(value = "mottattDatoInntektsmelding")
    private LocalDate mottattDatoInntektsmelding;

    @JsonProperty(value = "fomDato", required = true)
    @NotNull
    private LocalDate fomDato;

    @JsonProperty(value = "tomDato")
    private LocalDate tomDato;

    @JsonProperty(value = "brukArbeidsforholdet")
    private Boolean brukArbeidsforholdet;

    @JsonProperty(value = "fortsetBehandlingUtenInntektsmelding")
    private Boolean fortsettBehandlingUtenInntektsmelding;

    @JsonProperty(value = "erNyttArbeidsforhold")
    private Boolean erNyttArbeidsforhold;

    @JsonProperty(value = "lagtTilAvSaksbehandler")
    private boolean lagtTilAvSaksbehandler;

    @JsonProperty(value = "basertPaInntektsmelding")
    private boolean basertPaInntektsmelding;

    @JsonProperty(value = "brukPermisjon")
    private Boolean brukPermisjon;

    @JsonProperty(value = "inntektMedTilBeregningsgrunnlag")
    private Boolean inntektMedTilBeregningsgrunnlag;

    @JsonProperty(value = "permisjoner")
    @Size(max = 100)
    @Valid
    private List<PermisjonDto> permisjoner;

    @JsonProperty(value = "overstyrtTom")
    private LocalDate overstyrtTom;

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public String getArbeidsgiverIdentifikator() {
        return arbeidsgiverIdentifikator;
    }

    public void setArbeidsgiverIdentifikator(String arbeidsgiverIdentifikator) {
        this.arbeidsgiverIdentifikator = arbeidsgiverIdentifikator;
    }

    public LocalDate getFomDato() {
        return fomDato;
    }

    public void setFomDato(LocalDate fomDato) {
        this.fomDato = fomDato;
    }

    public LocalDate getTomDato() {
        return tomDato;
    }

    public void setTomDato(LocalDate tomDato) {
        this.tomDato = tomDato;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public BigDecimal getStillingsprosent() {
        return stillingsprosent;
    }

    public void setStillingsprosent(BigDecimal stillingsprosent) {
        this.stillingsprosent = stillingsprosent;
    }

    public Boolean getBrukArbeidsforholdet() {
        return brukArbeidsforholdet;
    }

    public void setBrukArbeidsforholdet(Boolean brukArbeidsforholdet) {
        this.brukArbeidsforholdet = brukArbeidsforholdet;
    }

    public Boolean getFortsettBehandlingUtenInntektsmelding() {
        return fortsettBehandlingUtenInntektsmelding;
    }

    public void setFortsettBehandlingUtenInntektsmelding(Boolean fortsettBehandlingUtenInntektsmelding) {
        this.fortsettBehandlingUtenInntektsmelding = fortsettBehandlingUtenInntektsmelding;
    }

    public LocalDate getMottattDatoInntektsmelding() {
        return mottattDatoInntektsmelding;
    }

    public void setMottattDatoInntektsmelding(LocalDate mottattDatoInntektsmelding) {
        this.mottattDatoInntektsmelding = mottattDatoInntektsmelding;
    }

    public UUID getArbeidsforholdId() {
        return arbeidsforholdId;
    }

    public void setArbeidsforholdId(String arbeidsforholdId) {
        this.arbeidsforholdId = UUID.fromString(arbeidsforholdId);
    }

    public boolean getBasertPaInntektsmelding() {
        return basertPaInntektsmelding;
    }

    public void setBasertPaInntektsmelding(boolean basertPaInntektsmelding) {
        this.basertPaInntektsmelding = basertPaInntektsmelding;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getErNyttArbeidsforhold() {
        return erNyttArbeidsforhold;
    }

    public void setErNyttArbeidsforhold(Boolean erNyttArbeidsforhold) {
        this.erNyttArbeidsforhold = erNyttArbeidsforhold;
    }

    public String getErstatterArbeidsforholdId() {
        return erstatterArbeidsforholdId;
    }

    public void setErstatterArbeidsforholdId(String erstatterArbeidsforholdId) {
        this.erstatterArbeidsforholdId = erstatterArbeidsforholdId;
    }

    public void setLagtTilAvSaksbehandler(boolean lagtTilAvSaksbehandler) {
        this.lagtTilAvSaksbehandler = lagtTilAvSaksbehandler;
    }

    public boolean getLagtTilAvSaksbehandler() {
        return lagtTilAvSaksbehandler;
    }

    public Boolean getInntektMedTilBeregningsgrunnlag() {
        return inntektMedTilBeregningsgrunnlag;
    }

    public void setInntektMedTilBeregningsgrunnlag(Boolean inntektMedTilBeregningsgrunnlag) {
        this.inntektMedTilBeregningsgrunnlag = inntektMedTilBeregningsgrunnlag;
    }

    public Boolean getBrukPermisjon() {
        return brukPermisjon;
    }

    public void setBrukPermisjon(Boolean brukPermisjon) {
        this.brukPermisjon = brukPermisjon;
    }

    public void setPermisjoner(List<PermisjonDto> permisjoner) {
        this.permisjoner = permisjoner;
    }

    public List<PermisjonDto> getPermisjoner() {
        return permisjoner;
    }

    public LocalDate getOverstyrtTom() {
        return overstyrtTom;
    }

    public void setOverstyrtTom(LocalDate overstyrtTom) {
        this.overstyrtTom = overstyrtTom;
    }

}
