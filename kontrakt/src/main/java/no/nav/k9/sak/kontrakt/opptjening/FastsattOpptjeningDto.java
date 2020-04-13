package no.nav.k9.sak.kontrakt.opptjening;

import java.time.LocalDate;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FastsattOpptjeningDto {

    @JsonProperty(value = "fastsattOpptjeningAktivitetList")
    @Valid
    @Size(max = 100)
    private List<FastsattOpptjeningAktivitetDto> fastsattOpptjeningAktivitetList;

    @JsonProperty(value = "opptjeningFom")
    @NotNull
    private LocalDate opptjeningFom;
    @JsonProperty(value = "opptjeningTom")
    @NotNull
    private LocalDate opptjeningTom;

    @JsonProperty(value = "opptjeningperiode")
    @Valid
    @NotNull
    private OpptjeningPeriodeDto opptjeningperiode;

    public FastsattOpptjeningDto() {
        // trengs for deserialisering av JSON
    }

    public FastsattOpptjeningDto(LocalDate fom, LocalDate tom, OpptjeningPeriodeDto opptjeningperiode,
                                 List<FastsattOpptjeningAktivitetDto> fastsattOpptjeningAktivitetList) {
        this.opptjeningFom = fom;
        this.opptjeningTom = tom;
        this.opptjeningperiode = opptjeningperiode;
        this.fastsattOpptjeningAktivitetList = fastsattOpptjeningAktivitetList;
    }

    public List<FastsattOpptjeningAktivitetDto> getFastsattOpptjeningAktivitetList() {
        return fastsattOpptjeningAktivitetList;
    }

    public LocalDate getOpptjeningFom() {
        return opptjeningFom;
    }

    public OpptjeningPeriodeDto getOpptjeningperiode() {
        return opptjeningperiode;
    }

    public LocalDate getOpptjeningTom() {
        return opptjeningTom;
    }

    public void setFastsattOpptjeningAktivitetList(List<FastsattOpptjeningAktivitetDto> fastsattOpptjeningAktivitetList) {
        this.fastsattOpptjeningAktivitetList = fastsattOpptjeningAktivitetList;
    }

    public void setOpptjeningFom(LocalDate opptjeningFom) {
        this.opptjeningFom = opptjeningFom;
    }

    public void setOpptjeningperiode(OpptjeningPeriodeDto opptjeningperiode) {
        this.opptjeningperiode = opptjeningperiode;
    }

    public void setOpptjeningTom(LocalDate opptjeningTom) {
        this.opptjeningTom = opptjeningTom;
    }
}
