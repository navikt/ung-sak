package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.organisasjon.VirksomhetType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class EgenNæringDto {

    @JsonProperty(value = "utenlandskvirksomhetsnavn")
    @Size(max = 200)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String utenlandskvirksomhetsnavn;

    @JsonProperty(value = "orgnr")
    @Size(max = 50)
    @Pattern(regexp = "^[\\p{Alnum}\\-_:.\\s]$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String orgnr;

    @JsonProperty(value = "erVarigEndret")
    private boolean erVarigEndret;

    @JsonProperty(value = "erNyoppstartet")
    private boolean erNyoppstartet;

    @JsonProperty(value = "virksomhetType")
    @Valid
    private VirksomhetType virksomhetType;

    @JsonProperty(value = "begrunnelse")
    @Size(max = 400)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String begrunnelse;

    @JsonProperty(value = "endringsdato")
    private LocalDate endringsdato;

    @JsonProperty(value = "oppstartsdato")
    private LocalDate oppstartsdato;

    @JsonProperty(value = "regnskapsførerNavn")
    @Size(max = 300)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String regnskapsførerNavn;

    @JsonProperty(value = "regnskapsførerTlf")
    @Size(max = 300)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String regnskapsførerTlf;

    @JsonProperty(value = "kanRegnskapsførerKontaktes")
    private boolean kanRegnskapsførerKontaktes;

    @JsonProperty(value = "erNyIArbeidslivet")
    private boolean erNyIArbeidslivet;

    @JsonProperty(value = "oppgittInntekt")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    private BigDecimal oppgittInntekt;

    public String getUtenlandskvirksomhetsnavn() {
        return utenlandskvirksomhetsnavn;
    }

    public void setUtenlandskvirksomhetsnavn(String utenlandskvirksomhetsnavn) {
        this.utenlandskvirksomhetsnavn = utenlandskvirksomhetsnavn;
    }

    public String getOrgnr() {
        return orgnr;
    }

    public void setOrgnr(String orgnr) {
        this.orgnr = orgnr;
    }

    public boolean isErVarigEndret() {
        return erVarigEndret;
    }

    public void setErVarigEndret(boolean erVarigEndret) {
        this.erVarigEndret = erVarigEndret;
    }

    public boolean isErNyoppstartet() {
        return erNyoppstartet;
    }

    public void setErNyoppstartet(boolean erNyoppstartet) {
        this.erNyoppstartet = erNyoppstartet;
    }

    public VirksomhetType getVirksomhetType() {
        return virksomhetType;
    }

    public void setVirksomhetType(VirksomhetType virksomhetType) {
        this.virksomhetType = virksomhetType;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public LocalDate getEndringsdato() {
        return endringsdato;
    }

    public void setEndringsdato(LocalDate endringsdato) {
        this.endringsdato = endringsdato;
    }

    public LocalDate getOppstartsdato() {
        return oppstartsdato;
    }

    public void setOppstartsdato(LocalDate oppstartsdato) {
        this.oppstartsdato = oppstartsdato;
    }

    public String getRegnskapsførerNavn() {
        return regnskapsførerNavn;
    }

    public void setRegnskapsførerNavn(String regnskapsførerNavn) {
        this.regnskapsførerNavn = regnskapsførerNavn;
    }

    public String getRegnskapsførerTlf() {
        return regnskapsførerTlf;
    }

    public void setRegnskapsførerTlf(String regnskapsførerTlf) {
        this.regnskapsførerTlf = regnskapsførerTlf;
    }

    public boolean isKanRegnskapsførerKontaktes() {
        return kanRegnskapsførerKontaktes;
    }

    public void setKanRegnskapsførerKontaktes(boolean kanRegnskapsførerKontaktes) {
        this.kanRegnskapsførerKontaktes = kanRegnskapsførerKontaktes;
    }

    public boolean isErNyIArbeidslivet() {
        return erNyIArbeidslivet;
    }

    public void setErNyIArbeidslivet(boolean erNyIArbeidslivet) {
        this.erNyIArbeidslivet = erNyIArbeidslivet;
    }

    public BigDecimal getOppgittInntekt() {
        return oppgittInntekt;
    }

    public void setOppgittInntekt(BigDecimal oppgittInntekt) {
        this.oppgittInntekt = oppgittInntekt;
    }
}
