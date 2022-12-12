package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.gjennomgått;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class GjennomgåttOpplæringDto {

    @JsonProperty(value = "perioder", required = true)
    @Size(max = 1000)
    @Valid
    @NotNull
    private List<OpplæringPeriodeDto> perioder;

    @JsonProperty(value = "vurderinger", required = true)
    @Size(max = 1000)
    @Valid
    @NotNull
    private List<OpplæringVurderingDto> vurderinger;

    @JsonProperty(value = "trengerVurderingAvReisetid", required = true)
    @Valid
    @NotNull
    private Boolean trengerVurderingAvReisetid;

    public GjennomgåttOpplæringDto(List<OpplæringPeriodeDto> perioder, List<OpplæringVurderingDto> vurderinger, Boolean trengerVurderingAvReisetid) {
        this.perioder = perioder;
        this.vurderinger = vurderinger;
        this.trengerVurderingAvReisetid = trengerVurderingAvReisetid;
    }

    public List<OpplæringPeriodeDto> getPerioder() {
        return perioder;
    }

    public List<OpplæringVurderingDto> getVurderinger() {
        return vurderinger;
    }

    public Boolean getTrengerVurderingAvReisetid() {
        return trengerVurderingAvReisetid;
    }
}
