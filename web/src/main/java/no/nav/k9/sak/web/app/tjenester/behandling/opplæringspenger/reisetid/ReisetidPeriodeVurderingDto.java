package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.reisetid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.sak.typer.Periode;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ReisetidPeriodeVurderingDto {

    @JsonProperty(value = "periode", required = true)
    @Valid
    private Periode periode;

    @JsonProperty(value = "resultat", required = true)
    @Valid
    @NotNull
    private Resultat resultat;

    public ReisetidPeriodeVurderingDto(Periode periode, Resultat resultat) {
        this.periode = periode;
        this.resultat = resultat;
    }

    public Periode getPeriode() {
        return periode;
    }

    public Resultat getResultat() {
        return resultat;
    }
}
