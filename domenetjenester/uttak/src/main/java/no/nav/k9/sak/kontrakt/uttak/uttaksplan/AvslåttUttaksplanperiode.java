package no.nav.k9.sak.kontrakt.uttak.uttaksplan;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import no.nav.k9.kodeverk.uttak.UtfallType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class AvslåttUttaksplanperiode extends Uttaksplanperiode {

    @JsonCreator
    public AvslåttUttaksplanperiode() {
    }

    @Override
    public UtfallType getUtfall() {
        return UtfallType.AVSLÅTT;
    }

}
