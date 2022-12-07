package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.gjennomgått;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.sak.typer.Periode;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OpplæringPeriodeDto {

    @JsonProperty(value = "opplæring", required = true)
    @Valid
    @NotNull
    private Periode periode;

    @JsonProperty(value = "reisetid", required = true)
    @Valid
    @NotNull
    private ReisetidDto reisetid;

    public OpplæringPeriodeDto(Periode periode, ReisetidDto reisetidDto) {
        this.periode = periode;
        this.reisetid = reisetidDto;
    }

    public Periode getPeriode() {
        return periode;
    }

    public ReisetidDto getReisetid() {
        return reisetid;
    }
}
