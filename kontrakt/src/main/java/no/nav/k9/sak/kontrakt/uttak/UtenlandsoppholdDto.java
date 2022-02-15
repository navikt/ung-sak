package no.nav.k9.sak.kontrakt.uttak;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class UtenlandsoppholdDto {
    @JsonProperty(value = "perioder")
    @Valid
    @Size
    private List<UtenlandsoppholdPeriodeDto> perioder;

    public UtenlandsoppholdDto() {

    }

    public void leggTil(LocalDate fom, LocalDate tom, String landkode, String årsak) {
        perioder.add(new UtenlandsoppholdPeriodeDto(fom, tom, landkode, årsak));
    }
}


class UtenlandsoppholdPeriodeDto {
    @JsonProperty(value = "periode")
    @Valid
    @Size
    private Periode periode;

    @JsonProperty(value = "landkode")
    @Valid
    private String landkode;

    @JsonProperty(value = "årsak")
    @Valid
    private String årsak;

    public UtenlandsoppholdPeriodeDto(LocalDate fom, LocalDate tom, String landkode, String årsak) {
        this.periode = new Periode(fom, tom);
        this.landkode = landkode;
        this.årsak = årsak;
    }
}
