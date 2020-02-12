package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.time.LocalDate;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class GraderingEllerRefusjonDto {

    @JsonProperty(value = "erGradering", required = true)
    @NotNull
    private boolean erGradering;

    @JsonProperty(value = "erRefusjon", required = true)
    @NotNull
    private boolean erRefusjon;

    @JsonProperty(value = "fom", required = true)
    @NotNull
    private LocalDate fom;

    @JsonProperty(value = "tom", required = true)
    @NotNull
    private LocalDate tom;

    public GraderingEllerRefusjonDto(boolean erRefusjon, boolean erGradering) {
        if ((erRefusjon && erGradering) || (!erRefusjon && !erGradering)) {
            throw new IllegalArgumentException("Må gjelde enten gradering eller refusjon");
        }
        this.erGradering = erGradering;
        this.erRefusjon = erRefusjon;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public boolean isErGradering() {
        return erGradering;
    }

    public boolean isErRefusjon() {
        return erRefusjon;
    }

    public void setErGradering(boolean erGradering) {
        this.erGradering = erGradering;
    }

    public void setErRefusjon(boolean erRefusjon) {
        this.erRefusjon = erRefusjon;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }
}
