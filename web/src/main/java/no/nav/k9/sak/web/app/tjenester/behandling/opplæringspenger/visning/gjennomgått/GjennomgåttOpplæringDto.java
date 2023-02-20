package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.visning.gjennomgått;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import no.nav.k9.sak.typer.Periode;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class GjennomgåttOpplæringDto {

    @JsonProperty(value = "perioder", required = true)
    @Size(max = 1000)
    @Valid
    private List<Periode> perioder;

    @JsonProperty(value = "vurderinger", required = true)
    @Size(max = 1000)
    @Valid
    private List<OpplæringVurderingDto> vurderinger;

    public GjennomgåttOpplæringDto(List<Periode> perioder, List<OpplæringVurderingDto> vurderinger) {
        this.perioder = perioder;
        this.vurderinger = vurderinger;
    }

    public List<Periode> getPerioder() {
        return perioder;
    }

    public List<OpplæringVurderingDto> getVurderinger() {
        return vurderinger;
    }
}
