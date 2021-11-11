package no.nav.k9.sak.kontrakt.stønadstatistikk.dto;

import java.math.BigDecimal;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class StønadstatistikkGraderingMotTilsyn {

    @JsonProperty(value = "etablertTilsyn", required = true)
    @NotNull
    @Valid
    private BigDecimal etablertTilsyn;
    
    /**
     * FOR_LAVT,
     * NATTEVÅK,
     * BEREDSKAP,
     * NATTEVÅK_OG_BEREDSKAP
     */
    @JsonProperty(value = "overseEtablertTilsynÅrsak")
    @Valid
    private String overseEtablertTilsynÅrsak;
    
    @JsonProperty(value = "andreSøkeresTilsyn", required = true)
    @NotNull
    @Valid
    private BigDecimal andreSøkeresTilsyn;
    
    @JsonProperty(value = "tilgjengeligForSøker", required = true)
    @NotNull
    @Valid
    private BigDecimal tilgjengeligForSøker;

    
    protected StønadstatistikkGraderingMotTilsyn() {
    }
    
    public StønadstatistikkGraderingMotTilsyn(BigDecimal etablertTilsyn,
            String overseEtablertTilsynÅrsak,
            BigDecimal andreSøkeresTilsyn,
            BigDecimal tilgjengeligForSøker) {
        this.etablertTilsyn = etablertTilsyn;
        this.overseEtablertTilsynÅrsak = overseEtablertTilsynÅrsak;
        this.andreSøkeresTilsyn = andreSøkeresTilsyn;
        this.tilgjengeligForSøker = tilgjengeligForSøker;
    }
    
    
}
