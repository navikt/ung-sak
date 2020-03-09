package no.nav.foreldrepenger.domene.uttak.uttaksplan.kontrakt;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
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
    private List<Uttaksplan> uttaksplaner = Collections.emptyList();

    @JsonCreator
    public UttaksplanListe(@JsonProperty(value = "uttaksplaner", required = true) @NotNull @Valid List<Uttaksplan> uttaksplaner) {
        if (uttaksplaner != null) {
            this.uttaksplaner = Collections.unmodifiableList(uttaksplaner);
        }
    }

    public List<Uttaksplan> getUttaksplaner() {
        return uttaksplaner;
    }
}
