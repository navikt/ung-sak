package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.institusjon;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class InstitusjonerDto {

    @JsonProperty(value = "perioder")
    @Size(max = 1000)
    @Valid
    private List<InstitusjonPeriodeDto> perioder;

    @JsonProperty(value = "vurderinger")
    @Size(max = 1000)
    @Valid
    private List<InstitusjonVurderingDto> vurderinger;

    public InstitusjonerDto(List<InstitusjonPeriodeDto> perioder, List<InstitusjonVurderingDto> vurderinger) {
        this.perioder = perioder;
        this.vurderinger = vurderinger;
    }

    public List<InstitusjonPeriodeDto> getPerioder() {
        return perioder;
    }

    public List<InstitusjonVurderingDto> getVurderinger() {
        return vurderinger;
    }
}
