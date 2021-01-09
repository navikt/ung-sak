package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.ResourceLink;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SykdomVurderingOversikt {

    @JsonProperty(value = "vurderingselementer")
    @Size(max = 1000)
    @Valid
    private List<SykdomVurderingOversiktElement> vurderingselementer = new ArrayList<>();
    
    /**
     * Perioder som skal vurderes.
     */
    @JsonProperty(value = "resterendeVurderingsperioder")
    @Size(max = 1000)
    @Valid
    private List<Periode> resterendeVurderingsperioder = new ArrayList<>();
    
    @JsonProperty(value = "søknadsperioderTilBehandling")
    @Size(max = 1000)
    @Valid
    private List<Periode> søknadsperioderTilBehandling = new ArrayList<>();
    
    /**
     * Perioder det er tillatt å gjøre vurderinger for (det finnes minst én søknadsperiode på barnet).
     */
    @JsonProperty(value = "perioderSomKanVurderes")
    @Size(max = 1000)
    @Valid
    private List<Periode> perioderSomKanVurderes = new ArrayList<>();
    
    /*
    // Om den siste versjonen har blitt besluttet iverksatt eller ikke.
    @JsonProperty(value = "besluttetIverksatt")
    @Valid
    private boolean besluttetIverksatt;
    
    // Saksnummeret til den saken som må besluttes før denne versjonen kan bli gjeldende (hvis annen sak en den man spør for).
    @JsonProperty(value = "annenSakSomMåBesluttesFørst")
    @Valid
    private SykdomAnnenSakDto annenSakSomMåBesluttesFørst;
    */
    
    @JsonProperty(value = "links")
    @Size(max = 100)
    @Valid
    private List<ResourceLink> links = new ArrayList<>();
    
    
    public SykdomVurderingOversikt(List<SykdomVurderingOversiktElement> vurderingselementer,
            List<Periode> resterendeVurderingsperioder, List<Periode> søknadsperioderTilBehandling,
            List<Periode> perioderSomKanVurderes,
            List<ResourceLink> links) {
        this.vurderingselementer = vurderingselementer;
        this.resterendeVurderingsperioder = resterendeVurderingsperioder;
        this.søknadsperioderTilBehandling = søknadsperioderTilBehandling;
        this.perioderSomKanVurderes = perioderSomKanVurderes;
        this.links = links;
    }
    
    
    public List<SykdomVurderingOversiktElement> getVurderingselementer() {
        return vurderingselementer;
    }
    
    public List<Periode> getResterendeVurderingsperioder() {
        return resterendeVurderingsperioder;
    }
    
    public List<Periode> getSøknadsperioderTilBehandling() {
        return søknadsperioderTilBehandling;
    }
    
    public List<Periode> getPerioderSomKanVurderes() {
        return perioderSomKanVurderes;
    }

}
