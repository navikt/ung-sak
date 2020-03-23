package no.nav.k9.sak.kontrakt.uttak.uttaksplan;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import no.nav.k9.kodeverk.uttak.UtfallType;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "utfall")
@JsonSubTypes(value = {
        @Type(name = "INNVILGET", value = InnvilgetUttaksplanperiode.class),
        @Type(name = "AVSLÅTT", value = AvslåttUttaksplanperiode.class),
})
public abstract class Uttaksplanperiode {

    public abstract UtfallType getUtfall();
}
