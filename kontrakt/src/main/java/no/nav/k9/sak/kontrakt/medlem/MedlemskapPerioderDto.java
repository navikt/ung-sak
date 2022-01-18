package no.nav.k9.sak.kontrakt.medlem;

import java.time.LocalDate;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.medlem.MedlemskapDekningType;
import no.nav.k9.kodeverk.medlem.MedlemskapKildeType;
import no.nav.k9.kodeverk.medlem.MedlemskapType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class MedlemskapPerioderDto {

    @JsonProperty(value = "beslutningsdato")
    private LocalDate beslutningsdato;

    @JsonProperty(value = "dekningType", required = true)
    @NotNull
    @Valid
    private MedlemskapDekningType dekningType;

    @JsonProperty(value = "fom", required = true)
    @NotNull
    private LocalDate fom;

    @JsonProperty(value = "kildeType", required = true)
    @NotNull
    @Valid
    private MedlemskapKildeType kildeType;

    @JsonProperty(value = "medlemskapType", required = true)
    @NotNull
    @Valid
    private MedlemskapType medlemskapType;

    @JsonProperty(value = "tom", required = true)
    @NotNull
    private LocalDate tom;

    public MedlemskapPerioderDto() {
        // trengs for deserialisering av JSON
    }

    public LocalDate getBeslutningsdato() {
        return beslutningsdato;
    }

    public MedlemskapDekningType getDekningType() {
        return dekningType;
    }

    public LocalDate getFom() {
        return fom;
    }

    public MedlemskapKildeType getKildeType() {
        return kildeType;
    }

    public MedlemskapType getMedlemskapType() {
        return medlemskapType;
    }

    public LocalDate getTom() {
        return tom;
    }

    public void setBeslutningsdato(LocalDate beslutningsdato) {
        this.beslutningsdato = beslutningsdato;
    }

    public void setDekningType(MedlemskapDekningType dekningType) {
        this.dekningType = dekningType;
    }

    public void setFom(LocalDate fom) {
        this.fom = Objects.requireNonNull(fom, "fom");
    }

    public void setKildeType(MedlemskapKildeType kildeType) {
        this.kildeType = kildeType;
    }

    public void setMedlemskapType(MedlemskapType medlemskapType) {
        this.medlemskapType = medlemskapType;
    }

    public void setTom(LocalDate tom) {
        this.tom = Objects.requireNonNull(tom, "tom");
    }
}