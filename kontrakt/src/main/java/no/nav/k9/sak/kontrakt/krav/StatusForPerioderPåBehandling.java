package no.nav.k9.sak.kontrakt.krav;

import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class StatusForPerioderPåBehandling {

    @Valid
    @Size
    @JsonProperty("perioderTilVurdering")
    private Set<Periode> perioderTilVurdering;

    @Valid
    @Size
    @JsonProperty("perioderMedÅrsak")
    private List<PeriodeMedÅrsaker> perioderMedÅrsak;

    @Valid
    @Size
    @JsonProperty("dokumenterTilBehandling")
    private List<KravDokumentMedSøktePerioder> dokumenterTilBehandling;

    @JsonCreator
    public StatusForPerioderPåBehandling(@Valid @Size @JsonProperty("perioderTilVurdering") Set<Periode> perioderTilVurdering,
                                         @Valid @Size @JsonProperty("perioderMedÅrsak") List<PeriodeMedÅrsaker> perioderMedÅrsak,
                                         @Valid @Size @JsonProperty("dokumenterTilBehandling") List<KravDokumentMedSøktePerioder> dokumenterTilBehandling) {
        this.perioderTilVurdering = perioderTilVurdering;
        this.perioderMedÅrsak = perioderMedÅrsak;
        this.dokumenterTilBehandling = dokumenterTilBehandling;
    }

    public Set<Periode> getPerioderTilVurdering() {
        return perioderTilVurdering;
    }

    public List<PeriodeMedÅrsaker> getPerioderMedÅrsak() {
        return perioderMedÅrsak;
    }

    public List<KravDokumentMedSøktePerioder> getDokumenterTilBehandling() {
        return dokumenterTilBehandling;
    }
}
