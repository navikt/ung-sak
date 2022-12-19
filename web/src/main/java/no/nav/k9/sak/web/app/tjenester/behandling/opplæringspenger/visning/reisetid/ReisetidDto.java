package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.visning.reisetid;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ReisetidDto {

    @JsonProperty(value = "perioder", required = true)
    @Size(max = 1000)
    @Valid
    private List<ReisetidPeriodeDto> perioder;

    @JsonProperty(value = "vurderinger", required = true)
    @Size(max = 1000)
    @Valid
    private List<ReisetidVurderingDto> vurderinger;

    public ReisetidDto(List<ReisetidPeriodeDto> perioder, List<ReisetidVurderingDto> vurderinger) {
        this.perioder = perioder;
        this.vurderinger = vurderinger;
    }

    public List<ReisetidPeriodeDto> getPerioder() {
        return perioder;
    }

    public List<ReisetidVurderingDto> getVurderinger() {
        return vurderinger;
    }
}
