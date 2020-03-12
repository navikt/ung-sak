package no.nav.foreldrepenger.domene.uttak.uttaksplan.kontrakt;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.uttak.UtfallType;

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

    public LocalDateTimeline<Uttaksplanperiode> getTimeline() {
        return new LocalDateTimeline<>(getPerioder().entrySet().stream().map(e -> toSegment(e.getKey(), e.getValue())).collect(Collectors.toList()));
    }

    private LocalDateSegment<Uttaksplanperiode> toSegment(Periode periode, Uttaksplanperiode value) {
        return new LocalDateSegment<>(periode.getFom(), periode.getTom(), value);
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
