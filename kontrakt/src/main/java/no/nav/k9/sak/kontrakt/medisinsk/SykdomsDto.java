package no.nav.k9.sak.kontrakt.medisinsk;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SykdomsDto {

    @JsonProperty(value = "legeerklæringer")
    @Size(max = 100)
    @Valid
    private List<Legeerklæring> legeerklæringer;

    @JsonProperty(value = "perioderMedKontinuerligTilsyn")
    @Size(max = 100)
    @Valid
    private List<Periode> perioderMedKontinuerligTilsyn;

    @JsonProperty(value = "perioderMedUtvidetKontinuerligTilsyn")
    @Size(max = 100)
    @Valid
    private List<Periode> perioderMedUtvidetKontinuerligTilsyn;

    public SykdomsDto(List<Legeerklæring> legeerklæringer, List<Periode> perioderMedKontinuerligTilsyn, List<Periode> perioderMedUtvidetKontinuerligTilsyn) {
        this.legeerklæringer = legeerklæringer;
        this.perioderMedKontinuerligTilsyn = perioderMedKontinuerligTilsyn;
        this.perioderMedUtvidetKontinuerligTilsyn = perioderMedUtvidetKontinuerligTilsyn;
    }

    public List<Legeerklæring> getLegeerklæringer() {
        return legeerklæringer;
    }

    public List<Periode> getPerioderMedKontinuerligTilsyn() {
        return perioderMedKontinuerligTilsyn;
    }

    public List<Periode> getPerioderMedUtvidetKontinuerligTilsyn() {
        return perioderMedUtvidetKontinuerligTilsyn;
    }
}
