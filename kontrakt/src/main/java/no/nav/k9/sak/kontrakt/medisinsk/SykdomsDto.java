package no.nav.k9.sak.kontrakt.medisinsk;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.medisinsk.aksjonspunkt.PeriodeMedTilsyn;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SykdomsDto {

    @JsonProperty(value = "legeerklæringer")
    @Size(max = 100)
    @Valid
    private List<Legeerklæring> legeerklæringer;

    @JsonProperty(value = "perioderMedKontinuerligTilsynOgPleie")
    @Size(max = 100)
    @Valid
    private List<PeriodeMedTilsyn> perioderMedKontinuerligTilsyn;

    @JsonProperty(value = "perioderMedUtvidetKontinuerligTilsynOgPleie")
    @Size(max = 100)
    @Valid
    private List<PeriodeMedTilsyn> perioderMedUtvidetKontinuerligTilsyn;

    @JsonProperty(value = "periodeTilVurdering")
    @Valid
    private Periode periodeTilVurdering;

    public SykdomsDto() {
        //
    }

    public SykdomsDto(Periode periodeTilVurdering) {
        this.periodeTilVurdering = periodeTilVurdering;
        this.legeerklæringer = List.of();
        this.perioderMedKontinuerligTilsyn = List.of();
        this.perioderMedUtvidetKontinuerligTilsyn = List.of();
    }

    public SykdomsDto(Periode periodeTilVurdering, List<Legeerklæring> legeerklæringer, List<PeriodeMedTilsyn> perioderMedKontinuerligTilsyn,
                      List<PeriodeMedTilsyn> perioderMedUtvidetKontinuerligTilsyn) {
        this.periodeTilVurdering = periodeTilVurdering;
        this.legeerklæringer = legeerklæringer;
        this.perioderMedKontinuerligTilsyn = perioderMedKontinuerligTilsyn;
        this.perioderMedUtvidetKontinuerligTilsyn = perioderMedUtvidetKontinuerligTilsyn;
    }

    public List<Legeerklæring> getLegeerklæringer() {
        return Collections.unmodifiableList(legeerklæringer);
    }

    public List<PeriodeMedTilsyn> getPerioderMedKontinuerligTilsyn() {
        return Collections.unmodifiableList(perioderMedKontinuerligTilsyn);
    }

    public List<PeriodeMedTilsyn> getPerioderMedUtvidetKontinuerligTilsyn() {
        return Collections.unmodifiableList(perioderMedUtvidetKontinuerligTilsyn);
    }

    public Periode getPeriodeTilVurdering() {
        return periodeTilVurdering;
    }

    public void setLegeerklæringer(List<Legeerklæring> legeerklæringer) {
        this.legeerklæringer = legeerklæringer;
    }

    public void setPerioderMedKontinuerligTilsyn(List<PeriodeMedTilsyn> perioderMedKontinuerligTilsyn) {
        this.perioderMedKontinuerligTilsyn = perioderMedKontinuerligTilsyn;
    }

    public void setPerioderMedUtvidetKontinuerligTilsyn(List<PeriodeMedTilsyn> perioderMedUtvidetKontinuerligTilsyn) {
        this.perioderMedUtvidetKontinuerligTilsyn = perioderMedUtvidetKontinuerligTilsyn;
    }

    public void setPeriodeTilVurdering(Periode periodeTilVurdering) {
        this.periodeTilVurdering = periodeTilVurdering;
    }

}
