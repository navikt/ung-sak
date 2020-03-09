package no.nav.foreldrepenger.domene.uttak.uttaksplan.kontrakt;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.uttak.PeriodeResultatType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class Uttaksplan {

    @JsonProperty(value = "perioder", required = true)
    @Valid
    private Map<Periode, UttaksperiodeInfo> perioder = new LinkedHashMap<>();

    public Map<Periode, UttaksperiodeInfo> getPerioder() {
        return perioder;
    }

    public void setPerioder(Map<Periode, UttaksperiodeInfo> perioder) {
        this.perioder = perioder;
    }

    public void leggTilPeriode(Periode periode, UttaksperiodeInfo uttaksperiodeInfo) {
        this.perioder.put(periode, uttaksperiodeInfo);
    }

    /** sjekk om uttaksplanen har noen innvilgede perioder. */
    public boolean harInnvilgetPerioder() {
        return getPerioder().values().stream().anyMatch(info -> Objects.equals(PeriodeResultatType.INNVILGET, info.getType()));
    }

    public boolean harAvslåttePerioder() {
        return getPerioder().values().stream().anyMatch(info -> Objects.equals(PeriodeResultatType.AVSLÅTT, info.getType()));
    }

    public NavigableMap<Periode, UttaksperiodeInfo> getPerioderReversert() {
        NavigableMap<Periode, UttaksperiodeInfo> map = new TreeMap<>(Comparator.comparing(Periode::getFom).reversed());
        map.putAll(getPerioder());
        return map;
    }
}
