package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.visning.gjennomgått;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.sak.typer.Periode;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OpplæringVurderingDto {

    @JsonProperty(value = "opplæring", required = true)
    @Valid
    @NotNull
    private Periode periode;

    @JsonProperty(value = "resultat", required = true)
    @Valid
    @NotNull
    private Resultat resultat;

    @JsonProperty(value = "begrunnelse", required = true)
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String begrunnelse;

    @JsonProperty(value = "tilknyttedeDokumenter", required = true)
    @Size(max = 100)
    @Valid
    private List<String> tilknyttedeDokumenter;

    public OpplæringVurderingDto(Periode periode, Resultat resultat, String begrunnelse, List<String> tilknyttedeDokumenter) {
        this.periode = periode;
        this.resultat = resultat;
        this.begrunnelse = begrunnelse;
        this.tilknyttedeDokumenter = tilknyttedeDokumenter;
    }

    public Periode getPeriode() {
        return periode;
    }

    public Resultat getResultat() {
        return resultat;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public List<String> getTilknyttedeDokumenter() {
        return tilknyttedeDokumenter;
    }
}
