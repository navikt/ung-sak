package no.nav.k9.sak.kontrakt.stønadstatistikk.dto;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class StønadstatistikkInngangsvilkår {

    @JsonProperty(value = "vilkår", required = true)
    @NotNull
    @Valid
    private String vilkår;

    @JsonProperty(value = "utfall", required = true)
    @NotNull
    @Valid
    private StønadstatistikkUtfall utfall;

    @JsonProperty(value = "detaljertUtfall", required = false)
    @Valid
    private List<StønadstatistikkDetaljertUtfall> detaljertUtfall;

    protected StønadstatistikkInngangsvilkår() {

    }

    public StønadstatistikkInngangsvilkår(String vilkår, StønadstatistikkUtfall utfall) {
        this.vilkår = vilkår;
        this.utfall = utfall;
        this.detaljertUtfall = List.of();
    }

    public StønadstatistikkInngangsvilkår(String vilkår, StønadstatistikkUtfall utfall, List<StønadstatistikkDetaljertUtfall> detaljertUtfall) {
        Objects.requireNonNull(detaljertUtfall, "detaljertUtfall");
        this.vilkår = vilkår;
        this.utfall = utfall;
        this.detaljertUtfall = detaljertUtfall;
    }

    public String getVilkår() {
        return vilkår;
    }

    public StønadstatistikkUtfall getUtfall() {
        return utfall;
    }

    public List<StønadstatistikkDetaljertUtfall> getDetaljertUtfall() {
        return detaljertUtfall;
    }
}
