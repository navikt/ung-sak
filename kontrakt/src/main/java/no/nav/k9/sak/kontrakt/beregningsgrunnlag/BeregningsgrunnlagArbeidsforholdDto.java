package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

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

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.organisasjon.Organisasjonstype;
import no.nav.k9.sak.typer.AktørId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BeregningsgrunnlagArbeidsforholdDto {

    @JsonProperty(value = "aktørId")
    @NotNull
    @Valid
    private AktørId aktørId;

    @JsonProperty(value = "arbeidsforholdId")
    @Size(max = 50)
    @Pattern(regexp = "^[\\p{XDigit}\\-]$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsforholdId;

    @JsonProperty(value = "arbeidsforholdType", required = true)
    @NotNull
    @Valid
    private OpptjeningAktivitetType arbeidsforholdType;

    @JsonProperty(value = "arbeidsgiverId")
    @Size(max = 20)
    @Pattern(regexp = "^\\d+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsgiverId;

    @JsonProperty(value = "arbeidsgiverNavn")
    @Size(max = 300)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsgiverNavn;

    @JsonProperty(value = "belopFraInntektsmeldingPrMnd")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal belopFraInntektsmeldingPrMnd;

    @JsonProperty(value = "eksternArbeidsforholdId")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String eksternArbeidsforholdId;

    @JsonProperty(value = "naturalytelsebortfaltPrÅr")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal naturalytelseBortfaltPrÅr;

    @JsonProperty(value = "naturalytelseTilkommetPrÅr")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal naturalytelseTilkommetPrÅr;

    @JsonProperty(value = "opphoersdato")
    private LocalDate opphoersdato;

    @JsonProperty(value = "organisasjonstype")
    @Valid
    private Organisasjonstype organisasjonstype;

    @JsonProperty(value = "refusjonPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal refusjonPrAar;

    @JsonProperty(value = "startdato")
    private LocalDate startdato;

    public BeregningsgrunnlagArbeidsforholdDto() {
        //
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BeregningsgrunnlagArbeidsforholdDto that = (BeregningsgrunnlagArbeidsforholdDto) o;
        return Objects.equals(arbeidsgiverNavn, that.arbeidsgiverNavn) &&
            Objects.equals(arbeidsgiverId, that.arbeidsgiverId) &&
            Objects.equals(startdato, that.startdato) &&
            Objects.equals(opphoersdato, that.opphoersdato) &&
            Objects.equals(arbeidsforholdId, that.arbeidsforholdId) &&
            Objects.equals(eksternArbeidsforholdId, that.eksternArbeidsforholdId) &&
            Objects.equals(arbeidsforholdType, that.arbeidsforholdType) &&
            Objects.equals(aktørId, that.aktørId);
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

    public String getArbeidsgiverId() {
        return arbeidsgiverId;
    }

    public String getArbeidsgiverNavn() {
        return arbeidsgiverNavn;
    }

    public BigDecimal getBelopFraInntektsmeldingPrMnd() {
        return belopFraInntektsmeldingPrMnd;
    }

    public String getEksternArbeidsforholdId() {
        return eksternArbeidsforholdId;
    }

    public BigDecimal getNaturalytelseBortfaltPrÅr() {
        return naturalytelseBortfaltPrÅr;
    }

    public BigDecimal getNaturalytelseTilkommetPrÅr() {
        return naturalytelseTilkommetPrÅr;
    }

    public LocalDate getOpphoersdato() {
        return opphoersdato;
    }

    public Organisasjonstype getOrganisasjonstype() {
        return organisasjonstype;
    }

    public BigDecimal getRefusjonPrAar() {
        return refusjonPrAar;
    }

    public LocalDate getStartdato() {
        return startdato;
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiverNavn, arbeidsgiverId, startdato, opphoersdato, arbeidsforholdId, eksternArbeidsforholdId, arbeidsforholdType, aktørId);
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

    public void setArbeidsgiverId(String arbeidsgiverId) {
        this.arbeidsgiverId = arbeidsgiverId;
    }

    public void setArbeidsgiverNavn(String arbeidsgiverNavn) {
        this.arbeidsgiverNavn = arbeidsgiverNavn;
    }

    public void setBelopFraInntektsmeldingPrMnd(BigDecimal belopFraInntektsmeldingPrMnd) {
        this.belopFraInntektsmeldingPrMnd = belopFraInntektsmeldingPrMnd;
    }

    public void setEksternArbeidsforholdId(String eksternArbeidsforholdId) {
        this.eksternArbeidsforholdId = eksternArbeidsforholdId;
    }

    public void setNaturalytelseBortfaltPrÅr(BigDecimal naturalytelseBortfaltPrÅr) {
        this.naturalytelseBortfaltPrÅr = naturalytelseBortfaltPrÅr;
    }

    public void setNaturalytelseTilkommetPrÅr(BigDecimal naturalytelseTilkommetPrÅr) {
        this.naturalytelseTilkommetPrÅr = naturalytelseTilkommetPrÅr;
    }

    public void setOpphoersdato(LocalDate opphoersdato) {
        this.opphoersdato = opphoersdato;
    }

    public void setOrganisasjonstype(Organisasjonstype organisasjonstype) {
        this.organisasjonstype = organisasjonstype;
    }

    public void setRefusjonPrAar(BigDecimal refusjonPrAar) {
        this.refusjonPrAar = refusjonPrAar;
    }

    public void setStartdato(LocalDate startdato) {
        this.startdato = startdato;
    }
}
