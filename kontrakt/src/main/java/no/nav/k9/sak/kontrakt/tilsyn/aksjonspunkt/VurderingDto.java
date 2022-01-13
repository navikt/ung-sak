package no.nav.k9.sak.kontrakt.tilsyn.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.typer.Periode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class VurderingDto {

    @JsonProperty(value = "begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String begrunnelse;

    @JsonProperty(value = "resultat")
    @Valid
    private Resultat resultat;

    @JsonProperty(value = "periode")
    @Valid
    private Periode periode;

    public VurderingDto() {
        //
    }

    public VurderingDto(String begrunnelse, Resultat resultat, Periode periode) {
        this.begrunnelse = begrunnelse;
        this.resultat = resultat;
        this.periode = periode;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public Resultat getResultat() {
        return resultat;
    }

    public Periode getPeriode() {
        return periode;
    }
}



