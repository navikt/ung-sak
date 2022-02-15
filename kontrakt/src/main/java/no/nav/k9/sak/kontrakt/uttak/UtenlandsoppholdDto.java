package no.nav.k9.sak.kontrakt.uttak;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class UtenlandsoppholdDto {
    // TODO: Lag class
    // private List<UtenlandsoppholdPeriodeDto> perioder;

    public UtenlandsoppholdDto() {

    }

    // TODO
    //public leggTil(UtenlandsoppholdPeriodeDto periode) {
        //perioder.add(periode);
    //}
}

// TODO:
// class UtenlandsoppholdPeriodeDto {
    //private
// }
