package no.nav.k9.sak.kontrakt.medlem;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class MedlemV2Dto {

    @JsonProperty(value="inntekt")
    @Size(max=200)
    @Valid
    private List<InntektDto> inntekt;
    
    @JsonProperty(value="medlemskapPerioder")
    @Size(max=200)
    @Valid
    private List<MedlemskapPerioderDto> medlemskapPerioder;
    
    @JsonProperty(value="perioder")
    @Size(max=200)
    @Valid
    private Set<MedlemPeriodeDto> perioder;

    public MedlemV2Dto() {
        // trengs for deserialisering av JSON
    }

    public List<InntektDto> getInntekt() {
        return Collections.unmodifiableList(inntekt);
    }

    public void setInntekt(List<InntektDto> inntekt) {
        this.inntekt = List.copyOf(inntekt);
    }

    public List<MedlemskapPerioderDto> getMedlemskapPerioder() {
        return Collections.unmodifiableList(medlemskapPerioder);
    }

    public void setMedlemskapPerioder(List<MedlemskapPerioderDto> medlemskapPerioder) {
        this.medlemskapPerioder = List.copyOf(medlemskapPerioder);
    }

    public Set<MedlemPeriodeDto> getPerioder() {
        return Collections.unmodifiableSet(perioder);
    }

    public void setPerioder(Set<MedlemPeriodeDto> perioder) {
        this.perioder = Set.copyOf(perioder);
    }
}
