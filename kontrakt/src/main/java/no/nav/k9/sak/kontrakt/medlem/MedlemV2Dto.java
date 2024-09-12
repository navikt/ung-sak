package no.nav.k9.sak.kontrakt.medlem;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import no.nav.k9.sak.kontrakt.person.PersonopplysningDto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class MedlemV2Dto {

    @JsonProperty(value = "medlemskapPerioder")
    @Size(max = 200)
    @Valid
    private List<MedlemskapPerioderDto> medlemskapPerioder;

    @JsonProperty(value = "perioder")
    @Size(max = 200)
    @Valid
    private Set<MedlemPeriodeDto> perioder;

    @JsonProperty(value = "personopplysninger")
    @Size(max = 200)
    @Valid
    private Map<LocalDate, PersonopplysningDto> personopplysninger;

    public MedlemV2Dto() {
        // trengs for deserialisering av JSON
    }

    public List<MedlemskapPerioderDto> getMedlemskapPerioder() {
        if (medlemskapPerioder == null) {
            return List.of();
        }
        return Collections.unmodifiableList(medlemskapPerioder);
    }

    public void setMedlemskapPerioder(List<MedlemskapPerioderDto> medlemskapPerioder) {
        this.medlemskapPerioder = List.copyOf(medlemskapPerioder);
    }

    public Set<MedlemPeriodeDto> getPerioder() {
        if (perioder == null) {
            return Set.of();
        }
        return Collections.unmodifiableSet(perioder);
    }

    public void setPerioder(Set<MedlemPeriodeDto> perioder) {
        this.perioder = Set.copyOf(perioder);
    }

    public Map<LocalDate, PersonopplysningDto> getPersonopplysninger() {
        if (medlemskapPerioder == null) {
            return Map.of();
        }
        return Collections.unmodifiableMap(personopplysninger);
    }

    public void setPersonopplysninger(Map<LocalDate, PersonopplysningDto> personopplysninger) {
        this.personopplysninger = Map.copyOf(personopplysninger);
    }
}
