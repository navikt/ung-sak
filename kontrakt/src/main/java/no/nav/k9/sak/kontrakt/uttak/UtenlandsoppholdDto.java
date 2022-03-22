package no.nav.k9.sak.kontrakt.uttak;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
public class UtenlandsoppholdDto {
    @JsonProperty(value = "perioder")
    @Valid
    @Size
    private List<UtenlandsoppholdPeriodeDto> perioder = new ArrayList<>();

    public UtenlandsoppholdDto() {

    }

    public void leggTil(LocalDate fom, LocalDate tom, Landkoder landkode, UtenlandsoppholdÅrsak årsak) {
        perioder.add(new UtenlandsoppholdPeriodeDto(fom, tom, landkode, årsak));
    }

    public List<UtenlandsoppholdPeriodeDto> getPerioder() {
        return Collections.unmodifiableList(perioder);
    }
}


