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

    @JsonProperty(value = "fom", required = true)
    @NotNull
    private LocalDate fom;

    @JsonProperty(value = "tom", required = true)
    @NotNull
    private LocalDate tom;

    @JsonProperty(value = "medlemskapType", required = true)
    @NotNull
    @Valid
    private MedlemskapType medlemskapType;

    @JsonProperty(value = "dekningType", required = true)
    @NotNull
    @Valid
    private MedlemskapDekningType dekningType;

    @JsonProperty(value = "kildeType", required = true)
    @NotNull
    @Valid
    private MedlemskapKildeType kildeType;

    @JsonProperty(value = "beslutningsdato")
    private LocalDate beslutningsdato;

    public MedlemskapPerioderDto() {
        // trengs for deserialisering av JSON
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public MedlemskapType getMedlemskapType() {
        return medlemskapType;
    }

    public MedlemskapDekningType getDekningType() {
        return dekningType;
    }

    public MedlemskapKildeType getKildeType() {
        return kildeType;
    }

    public LocalDate getBeslutningsdato() {
        return beslutningsdato;
    }

    public void setFom(LocalDate fom) {
        this.fom = Objects.requireNonNull(fom, "fom");
    }

    public void setTom(LocalDate tom) {
        this.tom = Objects.requireNonNull(tom, "tom");
    }

    public void setMedlemskapType(MedlemskapType medlemskapType) {
        this.medlemskapType = medlemskapType;
    }

    public void setDekningType(MedlemskapDekningType dekningType) {
        this.dekningType = dekningType;
    }

    public void setKildeType(MedlemskapKildeType kildeType) {
        this.kildeType = kildeType;
    }

    public void setBeslutningsdato(LocalDate beslutningsdato) {
        this.beslutningsdato = beslutningsdato;
    }
}