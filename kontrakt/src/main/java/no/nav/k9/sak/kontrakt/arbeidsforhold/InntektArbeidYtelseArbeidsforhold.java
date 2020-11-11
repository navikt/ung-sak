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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdKilde;
import no.nav.k9.sak.kontrakt.Patterns;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class InntektArbeidYtelseArbeidsforhold {

    @JsonProperty(value = "arbeidsforholdId")
    @Valid
    private UUID arbeidsforholdId;

    // For mottak fra GUI (orgnr for virksomhet, og aktørId for person-arbeidsgiver)
    @JsonProperty(value = "arbeidsgiverIdentifikator")
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String arbeidsgiverIdentifikator;

    // For visning i GUI (orgnr for virksomhet, og fødselsdato formatert dd.MM.yyyy for person-arbeidsgiver)
    @JsonProperty(value = "arbeidsgiverIdentifiktorGUI")
    @JsonAlias(value = "arbeidsgiverIdentifikatorForVisning")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String arbeidsgiverIdentifikatorGUI;

    @JsonProperty(value = "basertPaInntektsmelding")
    private boolean basertPaInntektsmelding;

    @JsonProperty(value = "begrunnelse")
    @Size(max = 400)
    @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String begrunnelse;

    @JsonProperty(value = "brukArbeidsforholdet")
    private Boolean brukArbeidsforholdet;

    @JsonProperty(value = "brukMedJustertPeriode")
    private boolean brukMedJustertPeriode;

    @JsonProperty(value = "brukPermisjon")
    private Boolean brukPermisjon;

    @JsonProperty(value = "eksternArbeidsforholdId")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String eksternArbeidsforholdId;

    @JsonProperty(value = "erEndret")
    private Boolean erEndret;

    @JsonProperty(value = "erNyttArbeidsforhold")
    private Boolean erNyttArbeidsforhold;

    @JsonProperty(value = "erSlettet")
    private Boolean erSlettet;

    @JsonProperty(value = "fomDato", required = true)
    @NotNull
    private LocalDate fomDato;

    @JsonProperty(value = "fortsetBehandlingUtenInntektsmelding")
    private Boolean fortsettBehandlingUtenInntektsmelding;

    @JsonProperty(value = "handlingType", required = true)
    @NotNull
    @Valid
    private ArbeidsforholdHandlingType handlingType;

    @JsonProperty(value = "harErstattetEttEllerFlere")
    private Boolean harErstattetEttEllerFlere;

    @JsonProperty(value = "id")
    @Pattern(regexp = "^[\\p{Alnum}\\-\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String id;

    @JsonProperty(value = "ikkeRegistrertIAaRegister")
    private Boolean ikkeRegistrertIAaRegister;

    @JsonProperty(value = "inntektMedTilBeregningsgrunnlag")
    private Boolean inntektMedTilBeregningsgrunnlag;

    @JsonProperty(value = "kilde", required = true)
    @NotNull
    @Valid
    private ArbeidsforholdKilde kilde;

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

    @JsonProperty(value = "skjaeringstidspunkt")
    private LocalDate skjaeringstidspunkt;

    @JsonProperty(value = "stillingsprosent")
    @DecimalMin(value = "0.00")
    @DecimalMax(value = "500.00")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal stillingsprosent;

    @JsonProperty(value = "tilVurdering")
    private Boolean tilVurdering;

    @JsonProperty(value = "tomDato")
    private LocalDate tomDato;

    @JsonProperty(value = "vurderOmSkalErstattes")
    private Boolean vurderOmSkalErstattes;

    public InntektArbeidYtelseArbeidsforhold() {
        //
    }

    public UUID getArbeidsforholdId() {
        return arbeidsforholdId;
    }

    public String getArbeidsgiverIdentifikator() {
        return arbeidsgiverIdentifikator;
    }

    public String getArbeidsgiverIdentifikatorGUI() {
        return arbeidsgiverIdentifikatorGUI;
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

    public String getEksternArbeidsforholdId() {
        return eksternArbeidsforholdId;
    }

    public Boolean getErEndret() {
        return erEndret;
    }

    public Boolean getErNyttArbeidsforhold() {
        return erNyttArbeidsforhold;
    }

    public Boolean getErSlettet() {
        return erSlettet;
    }

    public LocalDate getFomDato() {
        return fomDato;
    }

    public Boolean getFortsettBehandlingUtenInntektsmelding() {
        return fortsettBehandlingUtenInntektsmelding;
    }

    public ArbeidsforholdHandlingType getHandlingType() {
        return handlingType;
    }

    public Boolean getHarErstattetEttEllerFlere() {
        return harErstattetEttEllerFlere;
    }

    public String getId() {
        return id;
    }

    public Boolean getIkkeRegistrertIAaRegister() {
        return ikkeRegistrertIAaRegister;
    }

    public Boolean getInntektMedTilBeregningsgrunnlag() {
        return inntektMedTilBeregningsgrunnlag;
    }

    public ArbeidsforholdKilde getKilde() {
        return kilde;
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

    public LocalDate getSkjaeringstidspunkt() {
        return skjaeringstidspunkt;
    }

    public BigDecimal getStillingsprosent() {
        return stillingsprosent;
    }

    public Boolean getTilVurdering() {
        return tilVurdering;
    }

    public LocalDate getTomDato() {
        return tomDato;
    }

    public Boolean getVurderOmSkalErstattes() {
        return vurderOmSkalErstattes;
    }

    public boolean isBrukMedJustertPeriode() {
        return brukMedJustertPeriode;
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

    public void setArbeidsgiverIdentifikatorGUI(String arbeidsgiverIdentifikatorGUI) {
        this.arbeidsgiverIdentifikatorGUI = arbeidsgiverIdentifikatorGUI;
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

    public void setBrukMedJustertPeriode(boolean brukMedJustertPeriode) {
        this.brukMedJustertPeriode = brukMedJustertPeriode;
    }

    public void setBrukPermisjon(Boolean brukPermisjon) {
        this.brukPermisjon = brukPermisjon;
    }

    public void setEksternArbeidsforholdId(String eksternArbeidsforholdId) {
        this.eksternArbeidsforholdId = eksternArbeidsforholdId;
    }

    public void setErEndret(Boolean erEndret) {
        this.erEndret = erEndret;
    }

    public void setErNyttArbeidsforhold(Boolean erNyttArbeidsforhold) {
        this.erNyttArbeidsforhold = erNyttArbeidsforhold;
    }

    public void setErSlettet(Boolean erSlettet) {
        this.erSlettet = erSlettet;
    }

    public void setFomDato(LocalDate fomDato) {
        this.fomDato = fomDato;
    }

    public void setFortsettBehandlingUtenInntektsmelding(Boolean fortsettBehandlingUtenInntektsmelding) {
        this.fortsettBehandlingUtenInntektsmelding = fortsettBehandlingUtenInntektsmelding;
    }

    public void setHandlingType(ArbeidsforholdHandlingType handlingType) {
        this.handlingType = handlingType;
    }

    public void setHarErstattetEttEllerFlere(Boolean harErstattetEttEllerFlere) {
        this.harErstattetEttEllerFlere = harErstattetEttEllerFlere;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setIkkeRegistrertIAaRegister(Boolean ikkeRegistrertIAaRegister) {
        this.ikkeRegistrertIAaRegister = ikkeRegistrertIAaRegister;
    }

    public void setInntektMedTilBeregningsgrunnlag(Boolean inntektMedTilBeregningsgrunnlag) {
        this.inntektMedTilBeregningsgrunnlag = inntektMedTilBeregningsgrunnlag;
    }

    public void setKilde(ArbeidsforholdKilde kilde) {
        this.kilde = kilde;
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

    public void setSkjaeringstidspunkt(LocalDate skjaeringstidspunkt) {
        this.skjaeringstidspunkt = skjaeringstidspunkt;
    }

    public void setStillingsprosent(BigDecimal stillingsprosent) {
        this.stillingsprosent = stillingsprosent;
    }

    public void setTilVurdering(Boolean tilVurdering) {
        this.tilVurdering = tilVurdering;
    }

    public void setTomDato(LocalDate tomDato) {
        this.tomDato = tomDato;
    }

    public void setVurderOmSkalErstattes(boolean vurderOmSkalErstattes) {
        this.vurderOmSkalErstattes = vurderOmSkalErstattes;
    }

    public void setVurderOmSkalErstattes(Boolean vurderOmSkalErstattes) {
        this.vurderOmSkalErstattes = vurderOmSkalErstattes;
    }

}
