package no.nav.k9.sak.kontrakt.medisinsk.aksjonspunkt;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Pleiebehov {

    @JsonProperty(value = "perioderMedTilsynOgPleie")
    @Valid
    @Size(max = 100)
    private List<PeriodeMedTilsyn> perioderMedTilsynOgPleie;

    @JsonProperty(value = "perioderMedUtvidetTilsynOgPleie")
    @Valid
    @Size(max = 100)
    private List<PeriodeMedTilsyn> perioderMedUtvidetTilsynOgPleie;

    public Pleiebehov() {
    }

    public Pleiebehov(List<PeriodeMedTilsyn> perioderMedTilsynOgPleie, List<PeriodeMedTilsyn> perioderMedUtvidetTilsynOgPleie) {
        this.perioderMedTilsynOgPleie = List.copyOf(perioderMedTilsynOgPleie);
        this.perioderMedUtvidetTilsynOgPleie = List.copyOf(perioderMedUtvidetTilsynOgPleie);
    }

    public List<PeriodeMedTilsyn> getPerioderMedTilsynOgPleie() {
        return Collections.unmodifiableList(perioderMedTilsynOgPleie);
    }

    public List<PeriodeMedTilsyn> getPerioderMedUtvidetTilsynOgPleie() {
        return Collections.unmodifiableList(perioderMedUtvidetTilsynOgPleie);
    }

    public void setPerioderMedTilsynOgPleie(List<PeriodeMedTilsyn> perioderMedTilsynOgPleie) {
        this.perioderMedTilsynOgPleie = List.copyOf(perioderMedTilsynOgPleie);
    }

    public void setPerioderMedUtvidetTilsynOgPleie(List<PeriodeMedTilsyn> perioderMedUtvidetTilsynOgPleie) {
        this.perioderMedUtvidetTilsynOgPleie = List.copyOf(perioderMedUtvidetTilsynOgPleie);
    }
}
