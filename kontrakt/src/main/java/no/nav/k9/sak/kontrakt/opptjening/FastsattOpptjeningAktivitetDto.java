package no.nav.k9.sak.kontrakt.opptjening;

import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FastsattOpptjeningAktivitetDto {

    @JsonProperty(value = "aktivitetReferanse")
    @Pattern(regexp = "^[\\p{Alnum}:_\\-/\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String aktivitetReferanse;

    @JsonProperty(value = "arbeidsgiverNavn")
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsgiverNavn;

    @JsonProperty(value = "fom")
    @NotNull
    private LocalDate fom;

    @JsonProperty(value = "klasse")
    @Valid
    @NotNull
    private OpptjeningAktivitetKlassifisering klasse;

    @JsonProperty(value = "tom")
    @NotNull
    private LocalDate tom;

    @JsonProperty(value = "type")
    @Valid
    @NotNull
    private OpptjeningAktivitetType type;

    public FastsattOpptjeningAktivitetDto() {
        // trengs for deserialisering av JSON
    }

    public FastsattOpptjeningAktivitetDto(LocalDate fom, LocalDate tom, OpptjeningAktivitetKlassifisering klasse) {
        this.fom = fom;
        this.tom = tom;
        this.klasse = klasse;
    }

    public String getAktivitetReferanse() {
        return aktivitetReferanse;
    }

    public String getArbeidsgiverNavn() {
        return arbeidsgiverNavn;
    }

    public LocalDate getFom() {
        return fom;
    }

    public OpptjeningAktivitetKlassifisering getKlasse() {
        return klasse;
    }

    public LocalDate getTom() {
        return tom;
    }

    public OpptjeningAktivitetType getType() {
        return type;
    }

    public void setAktivitetReferanse(String aktivitetReferanse) {
        this.aktivitetReferanse = aktivitetReferanse;
    }

    public void setArbeidsgiverNavn(String arbeidsgiverNavn) {
        this.arbeidsgiverNavn = arbeidsgiverNavn;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public void setKlasse(OpptjeningAktivitetKlassifisering klasse) {
        this.klasse = klasse;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }

    public void setType(OpptjeningAktivitetType type) {
        this.type = type;
    }

}
