package no.nav.k9.sak.kontrakt.uttak;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.uttak.UtenlandsoppholdÅrsak;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class UtenlandsoppholdPeriodeDto {
    @JsonProperty(value = "periode")
    @Valid
    @Size
    private Periode periode;

    @JsonProperty(value = "landkode")
    @Valid
    private Landkoder landkode;

    @JsonProperty(value = "årsak")
    @Valid
    private UtenlandsoppholdÅrsak årsak;

    public UtenlandsoppholdPeriodeDto() {

    }

    public UtenlandsoppholdPeriodeDto(LocalDate fom, LocalDate tom, Landkoder landkode, UtenlandsoppholdÅrsak årsak) {
        this.periode = new Periode(fom, tom);
        this.landkode = landkode;
        this.årsak = årsak;
    }

    public Periode getPeriode() {
        return periode;
    }

    public Landkoder getLandkode() {
        return landkode;
    }

    public UtenlandsoppholdÅrsak getÅrsak() {
        return årsak;
    }
}
