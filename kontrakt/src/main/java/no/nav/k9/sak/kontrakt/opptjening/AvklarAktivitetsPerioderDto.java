package no.nav.k9.sak.kontrakt.opptjening;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.sak.kontrakt.Patterns;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class AvklarAktivitetsPerioderDto {

    @Valid
    @NotNull
    @JsonProperty(value = "opptjeningFom", required = true)
    private LocalDate opptjeningFom;

    @Valid
    @NotNull
    @JsonProperty(value = "opptjeningTom", required = true)
    private LocalDate opptjeningTom;

    @JsonProperty("begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String begrunnelse;

    @JsonProperty(value = "opptjeningAktivitetList")
    @Valid
    @Size(max = 100)
    private List<AvklarOpptjeningAktivitetDto> opptjeningAktivitetList;

    public AvklarAktivitetsPerioderDto() {
        // For Jackson
    }

    public AvklarAktivitetsPerioderDto(String begrunnelse, List<AvklarOpptjeningAktivitetDto> opptjeningAktivitetList) {
        this.begrunnelse = begrunnelse;
        this.opptjeningAktivitetList = opptjeningAktivitetList;
    }

    public List<AvklarOpptjeningAktivitetDto> getOpptjeningAktivitetList() {
        return opptjeningAktivitetList;
    }

    public void setOpptjeningAktivitetList(List<AvklarOpptjeningAktivitetDto> opptjeningAktivitetList) {
        this.opptjeningAktivitetList = opptjeningAktivitetList;
    }

    public LocalDate getOpptjeningFom() {
        return opptjeningFom;
    }

    public void setOpptjeningFom(LocalDate opptjeningFom) {
        this.opptjeningFom = opptjeningFom;
    }

    public LocalDate getOpptjeningTom() {
        return opptjeningTom;
    }

    public void setOpptjeningTom(LocalDate opptjeningTom) {
        this.opptjeningTom = opptjeningTom;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }
}
