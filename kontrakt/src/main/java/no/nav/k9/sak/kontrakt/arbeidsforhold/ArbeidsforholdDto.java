package no.nav.k9.sak.kontrakt.arbeidsforhold;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdKilde;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class ArbeidsforholdDto {

    @JsonProperty(value = "id")
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String id;

    @JsonProperty(value = "navn")
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String navn;

    // For mottak fra GUI (orgnr for virksomhet, og aktørId for person-arbeidsgiver)
    @JsonProperty(value = "arbeidsgiverIdentifikator")
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsgiverIdentifikator;

    // For visning i GUI (orgnr for virksomhet, og fødselsdato formatert dd.MM.yyyy for person-arbeidsgiver)
    @JsonProperty(value = "arbeidsgiverIdentifiktorGUI")
    @JsonAlias(value = "arbeidsgiverIdentifikatorForVisning")
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsgiverIdentifiktorGUI;

    @JsonProperty(value = "arbeidsforholdId")
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsforholdId;

    @JsonProperty(value = "eksternArbeidsforholdId")
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String eksternArbeidsforholdId;

    @JsonProperty(value = "begrunnelse")
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String begrunnelse;

    @JsonProperty(value = "erstatterArbeidsforholdId")
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String erstatterArbeidsforholdId;

    @JsonProperty(value = "handlingType")
    @NotNull
    @Valid
    private ArbeidsforholdHandlingType handlingType;

    @JsonProperty(value = "kilde")
    @Valid
    private ArbeidsforholdKildeDto kilde;

    @JsonProperty(value = "stillingsprosent")
    @DecimalMin(value = "0.00")
    @DecimalMax(value = "500.00")
    private BigDecimal stillingsprosent;

    @JsonProperty(value = "skjaeringstidspunkt")
    private LocalDate skjaeringstidspunkt;

    @JsonProperty(value = "mottattDatoInntektsmelding")
    private LocalDate mottattDatoInntektsmelding;

    @JsonProperty(value = "fomDato")
    private LocalDate fomDato;

    @JsonProperty(value = "tomDato")
    private LocalDate tomDato;

    @JsonProperty(value = "harErstattetEttEllerFlere")
    private Boolean harErstattetEttEllerFlere;

    @JsonProperty(value = "ikkeRegistrertIAaRegister")
    private Boolean ikkeRegistrertIAaRegister;

    @JsonProperty(value = "tilVurdering")
    private Boolean tilVurdering;

    @JsonProperty(value = "vurderOmSkalErstattes")
    private Boolean vurderOmSkalErstattes;

    @JsonProperty(value = "brukArbeidsforholdet")
    private Boolean brukArbeidsforholdet;

    @JsonProperty(value = "fortsetBehandlingUtenInntektsmelding")
    private Boolean fortsettBehandlingUtenInntektsmelding;

    @JsonProperty(value = "erNyttArbeidsforhold")
    private Boolean erNyttArbeidsforhold;

    @JsonProperty(value = "erEndret")
    private Boolean erEndret;

    @JsonProperty(value = "erSlettet")
    private Boolean erSlettet;

    @JsonProperty(value = "brukMedJustertPeriode")
    private boolean brukMedJustertPeriode;

    @JsonProperty(value = "lagtTilAvSaksbehandler")
    private boolean lagtTilAvSaksbehandler;

    @JsonProperty(value = "basertPaInntektsmelding")
    private boolean basertPaInntektsmelding;

    @JsonProperty(value = "brukPermisjon")
    private Boolean brukPermisjon;

    @JsonProperty(value = "inntektMedTilBeregningsgrunnlag")
    private Boolean inntektMedTilBeregningsgrunnlag;

    @JsonProperty(value = "permisjoner")
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

    public void setKilde(ArbeidsforholdKilde kilde) {
        this.kilde = new ArbeidsforholdKildeDto(kilde.getNavn());
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

    public String getArbeidsforholdId() {
        return arbeidsforholdId;
    }

    public void setArbeidsforholdId(String arbeidsforholdId) {
        this.arbeidsforholdId = arbeidsforholdId;
    }

    public void setEksternArbeidsforholdId(String eksternArbeidsforholdId) {
        this.eksternArbeidsforholdId = eksternArbeidsforholdId;
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

    public void setErEndret(Boolean erEndret) {
        this.erEndret = erEndret;
    }

    public void setErSlettet(Boolean erSlettet) {
        this.erSlettet = erSlettet;
    }

    public String getErstatterArbeidsforholdId() {
        return erstatterArbeidsforholdId;
    }

    public void setErstatterArbeidsforholdId(String erstatterArbeidsforholdId) {
        this.erstatterArbeidsforholdId = erstatterArbeidsforholdId;
    }

    public void setHarErstattetEttEllerFlere(Boolean harErstattetEttEllerFlere) {
        this.harErstattetEttEllerFlere = harErstattetEttEllerFlere;
    }

    public void setIkkeRegistrertIAaRegister(Boolean ikkeRegistrertIAaRegister) {
        this.ikkeRegistrertIAaRegister = ikkeRegistrertIAaRegister;
    }

    public void setTilVurdering(Boolean tilVurdering) {
        this.tilVurdering = tilVurdering;
    }

    public void setVurderOmSkalErstattes(boolean vurderOmSkalErstattes) {
        this.vurderOmSkalErstattes = vurderOmSkalErstattes;
    }

    public void setArbeidsgiverIdentifiktorGUI(String arbeidsgiverIdentififaktorGUI) {
        this.arbeidsgiverIdentifiktorGUI = arbeidsgiverIdentififaktorGUI;
    }

    public void setHandlingType(ArbeidsforholdHandlingType handlingType) {
        this.handlingType = handlingType;
    }

    public void setBrukMedJustertPeriode(boolean brukMedJustertPeriode) {
        this.brukMedJustertPeriode = brukMedJustertPeriode;
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

    public void setSkjaeringstidspunkt(LocalDate skjaeringstidspunkt) {
        this.skjaeringstidspunkt = skjaeringstidspunkt;
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
