package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SykdomVurderingOversikt {

    @JsonProperty(value = "vurderingselementer")
    @Valid
    private List<SykdomVurderingOversiktElement> vurderingselementer = new ArrayList<>();
    
    /**
     * Perioder som skal vurderes.
     */
    @JsonProperty(value = "resterendeVurderingsperioder")
    @Valid
    private List<Periode> resterendeVurderingsperioder = new ArrayList<>();
    
    @JsonProperty(value = "søknadsperioderTilBehandling")
    @Valid
    private List<Periode> søknadsperioderTilBehandling = new ArrayList<>();
    
    /**
     * Perioder det er tillatt å gjøre vurderinger for (det finnes minst én søknadsperiode på barnet).
     */
    @JsonProperty(value = "perioderSomKanVurderes")
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
    
    
    
    public SykdomVurderingOversikt(List<SykdomVurderingOversiktElement> vurderingselementer,
            List<Periode> resterendeVurderingsperioder, List<Periode> søknadsperioderTilBehandling,
            List<Periode> perioderSomKanVurderes) {
        this.vurderingselementer = vurderingselementer;
        this.resterendeVurderingsperioder = resterendeVurderingsperioder;
        this.søknadsperioderTilBehandling = søknadsperioderTilBehandling;
        this.perioderSomKanVurderes = perioderSomKanVurderes;
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
