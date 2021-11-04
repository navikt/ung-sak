package no.nav.k9.sak.hendelse.stønadstatistikk.dto;

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
    
    
    public StønadstatistikkInngangsvilkår() {
        
    }
    
    public StønadstatistikkInngangsvilkår(String vilkår, StønadstatistikkUtfall utfall) {
        this.vilkår = vilkår;
        this.utfall = utfall;
    }
    
    
}
