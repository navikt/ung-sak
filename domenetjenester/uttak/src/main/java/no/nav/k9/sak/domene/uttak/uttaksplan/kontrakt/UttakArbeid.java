package no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.kontrakt.uttak.UttakArbeidsforhold;
import no.nav.k9.sak.kontrakt.uttak.UttakArbeidsforholdPeriodeInfo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class UttakArbeid implements Comparable<UttakArbeid> {

    @JsonProperty(value = "arbeidsforhold", required = true)
    @Valid
    @NotNull
    private UttakArbeidsforhold arbeidsforhold;
    
    @JsonProperty(value = "perioder", required = true)
    @Valid
    @NotNull
    private NavigableMap<Periode, UttakArbeidsforholdPeriodeInfo> perioder = Collections.emptyNavigableMap();

    public NavigableMap<Periode, UttakArbeidsforholdPeriodeInfo> getPerioder() {
        return Collections.unmodifiableNavigableMap(perioder);
    }
    
    public UttakArbeidsforhold getArbeidsforhold() {
        return arbeidsforhold;
    }

    @JsonProperty(value = "perioder", required = true)
    public void setPerioder(Map<Periode, UttakArbeidsforholdPeriodeInfo> perioder) {
        this.perioder = new TreeMap<>(perioder);
    }

    public void setArbeidsforhold(UttakArbeidsforhold arbeidsforhold) {
        this.arbeidsforhold = arbeidsforhold;
    }
    
    @Override
    public int compareTo(UttakArbeid o) {
        return this.arbeidsforhold.compareTo(o.arbeidsforhold);
    }
}
