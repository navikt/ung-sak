package no.nav.k9.sak.kontrakt.uttak;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class UtenlandsoppholdDto {
    private List<UtenlandsoppholdPeriodeDto> perioder;

    public UtenlandsoppholdDto() {

    }

    public void leggTil(LocalDate fom, LocalDate tom, String landkode, String årsak) {
        perioder.add(new UtenlandsoppholdPeriodeDto(fom, tom, landkode, årsak));
    }
}


class UtenlandsoppholdPeriodeDto {
    private LocalDate fom;
    private LocalDate tom;
    private String landkode;
    private String årsak;

    public UtenlandsoppholdPeriodeDto(LocalDate fom, LocalDate tom, String landkode, String årsak) {
        this.fom = fom;
        this.tom = tom;
        this.landkode = landkode;
        this.årsak = årsak;
    }
}
