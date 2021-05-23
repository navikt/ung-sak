package no.nav.k9.sak.kontrakt.tilsyn.aksjonspunkt;

import com.fasterxml.jackson.annotation.*;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.typer.Periode;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_BEREDSKAP)
public class VurderingBeredskapDto extends BekreftetAksjonspunktDto implements Vurdering {


    @JsonProperty(value = "vurderingstekst")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String vurderingstekst;

    @JsonProperty(value = "resultat")
    @Valid
    private Resultat resultat;

    @JsonProperty(value = "nyePerioder")
    @Size(max = 1000)
    @Valid
    private List<Periode> perioder;


    protected VurderingBeredskapDto() {
        
    }
    
    public VurderingBeredskapDto(String vurderingstekst, Resultat resultat, List<Periode> perioder) {
        this.vurderingstekst = vurderingstekst;
        this.resultat = resultat;
        this.perioder = perioder;
    }



    @Override
    public String getVurderingstekst() {
        return vurderingstekst;
    }

    @Override
    public Resultat getResultat() {
        return resultat;
    }

    @Override
    public List<Periode> getPerioder() {
        return perioder;
    }


}
