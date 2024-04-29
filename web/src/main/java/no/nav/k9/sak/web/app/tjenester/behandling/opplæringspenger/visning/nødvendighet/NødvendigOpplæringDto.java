package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.visning.nødvendighet;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class NødvendigOpplæringDto {

    @JsonProperty(value = "perioder", required = true)
    @Size(max = 1000)
    @Valid
    @NotNull
    private List<NødvendighetPeriodeDto> perioder;

    @JsonProperty(value = "vurderinger", required = true)
    @Size(max = 1000)
    @Valid
    @NotNull
    private List<NødvendighetVurderingDto> vurderinger;

    public NødvendigOpplæringDto(List<NødvendighetPeriodeDto> perioder, List<NødvendighetVurderingDto> vurderinger) {
        this.perioder = perioder;
        this.vurderinger = vurderinger;
    }

    public List<NødvendighetPeriodeDto> getPerioder() {
        return perioder;
    }

    public List<NødvendighetVurderingDto> getVurderinger() {
        return vurderinger;
    }
}
