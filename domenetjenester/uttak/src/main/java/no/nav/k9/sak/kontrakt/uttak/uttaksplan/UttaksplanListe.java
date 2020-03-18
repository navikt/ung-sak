package no.nav.k9.sak.kontrakt.uttak.uttaksplan;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class UttaksplanListe {

    @JsonProperty(value = "uttaksplaner", required = true)
    @Valid
    @NotNull
    private NavigableMap<String, Uttaksplan> uttaksplaner = Collections.emptyNavigableMap();

    @JsonCreator
    public UttaksplanListe(@JsonProperty(value = "uttaksplaner", required = true) @NotNull @Valid Map<String, Uttaksplan> uttaksplaner) {
        if (uttaksplaner != null) {
            this.uttaksplaner = new TreeMap<>(uttaksplaner);
        }
    }

    public NavigableMap<String, Uttaksplan> getUttaksplaner() {
        return Collections.unmodifiableNavigableMap(uttaksplaner);
    }
}
