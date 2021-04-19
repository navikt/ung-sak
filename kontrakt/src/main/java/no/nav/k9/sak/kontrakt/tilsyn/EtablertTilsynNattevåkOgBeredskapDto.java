package no.nav.k9.sak.kontrakt.tilsyn;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class EtablertTilsynNattevåkOgBeredskapDto {



    private List<EtablertTilsynPeriodeDto> etablertTilsynPerioder;
    private List<NattevåkPeriodeDto> nattevåkPerioder;
    private List<BeredskapPeriodeDto> beredskapPerioder;



}
