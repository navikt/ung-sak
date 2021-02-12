package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SykdomVurderingAnnenInformasjon {

    /**
     * Perioder som skal vurderes.
     */
    @JsonProperty(value = "resterendeVurderingsperioder")
    @Size(max = 1000)
    @Valid
    private List<Periode> resterendeVurderingsperioder = new ArrayList<>();
    
    @JsonProperty(value = "perioderSomKanVurderes")
    @Size(max = 1000)
    @Valid
    private List<Periode> perioderSomKanVurderes = new ArrayList<>();

    
    public SykdomVurderingAnnenInformasjon(List<Periode> resterendeVurderingsperioder,
            List<Periode> perioderSomKanVurderes) {
        this.resterendeVurderingsperioder = resterendeVurderingsperioder;
        this.perioderSomKanVurderes = perioderSomKanVurderes;
    }
    
    
    public List<Periode> getResterendeVurderingsperioder() {
        return resterendeVurderingsperioder;
    }
    
    public List<Periode> getPerioderSomKanVurderes() {
        return perioderSomKanVurderes;
    }
}
