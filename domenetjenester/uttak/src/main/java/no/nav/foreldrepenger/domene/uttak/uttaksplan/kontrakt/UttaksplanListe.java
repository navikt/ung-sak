package no.nav.foreldrepenger.domene.uttak.uttaksplan.kontrakt;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;

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
    private NavigableMap<UUID, Uttaksplan> uttaksplaner = Collections.emptyNavigableMap();

    @JsonCreator
    public UttaksplanListe(@JsonProperty(value = "uttaksplaner", required = true) @NotNull @Valid Map<UUID, Uttaksplan> uttaksplaner) {
        if (uttaksplaner != null) {
            this.uttaksplaner = new TreeMap<>(uttaksplaner);
        }
    }

    public NavigableMap<UUID, Uttaksplan> getUttaksplaner() {
        return Collections.unmodifiableNavigableMap(uttaksplaner);
    }
}
