package no.nav.k9.sak.kontrakt.stønadstatistikk.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class StønadstatistikkRelasjonPeriode {

    @JsonProperty(value = "fom", required = true)
    @NotNull
    @Valid
    private LocalDate fom;
    
    @JsonProperty(value = "tom", required = true)
    @NotNull
    @Valid
    private LocalDate tom;
    
    @JsonProperty(value = "kode", required = true)
    @NotNull
    @Valid
    private StønadstatistikkRelasjon kode;
    
    protected StønadstatistikkRelasjonPeriode() {
        
    }
    
    public StønadstatistikkRelasjonPeriode(LocalDate fom, LocalDate tom, StønadstatistikkRelasjon kode) {
        this.fom = fom;
        this.tom = tom;
        this.kode = kode;
    }
    
    
    public LocalDate getFom() {
        return fom;
    }
    
    public LocalDate getTom() {
        return tom;
    }
    
    public StønadstatistikkRelasjon getKode() {
        return kode;
    }
}
