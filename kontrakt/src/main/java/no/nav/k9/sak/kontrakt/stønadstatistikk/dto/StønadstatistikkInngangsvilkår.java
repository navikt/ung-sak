package no.nav.k9.sak.kontrakt.stønadstatistikk.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StønadstatistikkInngangsvilkår {

    @JsonProperty(value = "vilkår", required = true)
    @NotNull
    @Valid
    private String vilkår;
    
    @JsonProperty(value = "utfall", required = true)
    @NotNull
    @Valid
    private StønadstatistikkUtfall utfall;
    
    
    protected StønadstatistikkInngangsvilkår() {
        
    }
    
    public StønadstatistikkInngangsvilkår(String vilkår, StønadstatistikkUtfall utfall) {
        this.vilkår = vilkår;
        this.utfall = utfall;
    }
    
    public String getVilkår() {
        return vilkår;
    }
    
    public StønadstatistikkUtfall getUtfall() {
        return utfall;
    }
}
