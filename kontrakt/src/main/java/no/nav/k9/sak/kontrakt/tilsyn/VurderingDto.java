package no.nav.k9.sak.kontrakt.tilsyn;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.typer.Periode;

import javax.validation.Valid;
import javax.validation.constraints.Size;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class VurderingDto {

    @JsonProperty(value = "id")
    @Valid
    private Long id;

    @JsonProperty(value = "periode")
    @Valid
    private Periode periode;

    @JsonProperty(value = "begrunnelse")
    @Size(max = 4000)
    @Valid
    private String begrunnelse;

    @JsonProperty(value = "resultat")
    @Valid
    private Resultat resultat;

    @JsonProperty(value = "kilde")
    @Valid
    private Kilde kilde;

    public VurderingDto(Long id, Periode periode, String begrunnelse, Resultat resultat, Kilde kilde) {
        this.periode = periode;
        this.begrunnelse = begrunnelse;
        this.resultat = resultat;
        this.kilde = kilde;
    }

    public Long getId() {
        return id;
    }

    public Periode getPeriode() {
        return periode;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public Resultat getResultat() {
        return resultat;
    }

    public Kilde getKilde() {
        return kilde;
    }
}
