package no.nav.k9.sak.domene.uttak.uttaksplan;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.uttak.UtfallType;
import no.nav.k9.sak.kontrakt.uttak.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class Uttaksplan {

    @JsonProperty(value = "perioder", required = true)
    @Valid
    private NavigableMap<Periode, Uttaksplanperiode> perioder;

    @JsonCreator
    public Uttaksplan(@JsonProperty(value = "perioder", required = true) @Valid Map<Periode, Uttaksplanperiode> perioder) {
        this.perioder = perioder == null ? Collections.emptyNavigableMap() : new TreeMap<>(perioder);
    }

    public NavigableMap<Periode, Uttaksplanperiode> getPerioder() {
        return Collections.unmodifiableNavigableMap(perioder);
    }

    /** sjekk om uttaksplanen har noen innvilgede perioder. */
    public boolean harInnvilgetPerioder() {
        return getPerioder().values().stream().anyMatch(info -> UtfallType.INNVILGET.equals(info.getUtfall()));
    }

    public boolean harAvslåttePerioder() {
        return getPerioder().values().stream().anyMatch(info -> UtfallType.AVSLÅTT.equals(info.getUtfall()));
    }

    public NavigableMap<Periode, Uttaksplanperiode> getPerioderReversert() {
        return getPerioder().descendingMap();
    }
}
