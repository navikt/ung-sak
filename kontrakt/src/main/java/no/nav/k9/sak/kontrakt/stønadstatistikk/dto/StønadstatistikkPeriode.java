package no.nav.k9.sak.kontrakt.stønadstatistikk.dto;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class StønadstatistikkPeriode {

    @JsonProperty(value = "fom", required = true)
    @NotNull
    @Valid
    private LocalDate fom;
    
    @JsonProperty(value = "tom", required = true)
    @NotNull
    @Valid
    private LocalDate tom;
    
    @JsonProperty(value = "utfall", required = true)
    @NotNull
    @Valid
    private StønadstatistikkUtfall utfall;
    
    @JsonProperty(value = "uttaksgrad", required = true)
    @NotNull
    @Valid
    private BigDecimal uttaksgrad;
    
    @JsonProperty(value = "utbetalingsgrader", required = true)
    @Size(max=1000)
    @NotNull
    @Valid
    private List<StønadstatistikkUtbetalingsgrad> utbetalingsgrader;
    
    @JsonProperty(value = "søkersTapteArbeidstid", required = true)
    @NotNull
    @Valid
    private BigDecimal søkersTapteArbeidstid;
    
    @JsonProperty(value = "oppgittTilsyn", required = true)
    @NotNull
    @Valid
    private Duration oppgittTilsyn;
    
    @JsonProperty(value = "årsaker", required = true)
    @Size(max=100)
    @NotNull
    @Valid
    private List<String> årsaker;
    
    @JsonProperty(value = "inngangsvilkår", required = true)
    @Size(max=1000)
    @NotNull
    @Valid
    private List<StønadstatistikkInngangsvilkår> inngangsvilkår;
    
    @JsonProperty(value = "pleiebehov", required = true)
    @NotNull
    @Valid
    private BigDecimal pleiebehov;
    
    @JsonProperty(value = "graderingMotTilsyn")
    @Valid
    private StønadstatistikkGraderingMotTilsyn graderingMotTilsyn;
    
    @JsonProperty(value = "nattevåk")
    @Valid
    private StønadstatistikkUtfall nattevåk;
    
    @JsonProperty(value = "beredskap")
    @Valid
    private StønadstatistikkUtfall beredskap;
    
    @JsonProperty(value = "søkersTapteTimer")
    @Valid
    private Duration søkersTapteTimer;
    
    @JsonProperty(value = "bruttoBeregningsgrunnlag", required = true)
    @NotNull
    @Valid
    private BigDecimal bruttoBeregningsgrunnlag;

    
    protected StønadstatistikkPeriode() {
        
    }
            
    public StønadstatistikkPeriode(LocalDate fom,
            LocalDate tom,
            StønadstatistikkUtfall utfall,
            BigDecimal uttaksgrad,
            List<StønadstatistikkUtbetalingsgrad> utbetalingsgrader,
            BigDecimal søkersTapteArbeidstid,
            Duration oppgittTilsyn,
            List<String> årsaker,
            List<StønadstatistikkInngangsvilkår> inngangsvilkår,
            BigDecimal pleiebehov,
            StønadstatistikkGraderingMotTilsyn graderingMotTilsyn,
            StønadstatistikkUtfall nattevåk,
            StønadstatistikkUtfall beredskap,
            Duration søkersTapteTimer,
            BigDecimal bruttoBeregningsgrunnlag) {
        this.fom = fom;
        this.tom = tom;
        this.utfall = utfall;
        this.uttaksgrad = uttaksgrad;
        this.utbetalingsgrader = utbetalingsgrader;
        this.søkersTapteArbeidstid = søkersTapteArbeidstid;
        this.oppgittTilsyn = oppgittTilsyn;
        this.årsaker = årsaker;
        this.inngangsvilkår = inngangsvilkår;
        this.pleiebehov = pleiebehov;
        this.graderingMotTilsyn = graderingMotTilsyn;
        this.nattevåk = nattevåk;
        this.beredskap = beredskap;
        this.søkersTapteTimer = søkersTapteTimer;
        this.bruttoBeregningsgrunnlag = bruttoBeregningsgrunnlag;
    }
    

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public StønadstatistikkUtfall getUtfall() {
        return utfall;
    }

    public BigDecimal getUttaksgrad() {
        return uttaksgrad;
    }

    public List<StønadstatistikkUtbetalingsgrad> getUtbetalingsgrader() {
        return utbetalingsgrader;
    }

    public BigDecimal getSøkersTapteArbeidstid() {
        return søkersTapteArbeidstid;
    }

    public Duration getOppgittTilsyn() {
        return oppgittTilsyn;
    }

    public List<String> getÅrsaker() {
        return årsaker;
    }

    public List<StønadstatistikkInngangsvilkår> getInngangsvilkår() {
        return inngangsvilkår;
    }

    public BigDecimal getPleiebehov() {
        return pleiebehov;
    }

    public StønadstatistikkGraderingMotTilsyn getGraderingMotTilsyn() {
        return graderingMotTilsyn;
    }

    public StønadstatistikkUtfall getNattevåk() {
        return nattevåk;
    }

    public StønadstatistikkUtfall getBeredskap() {
        return beredskap;
    }

    public Duration getSøkersTapteTimer() {
        return søkersTapteTimer;
    }

    public BigDecimal getBruttoBeregningsgrunnlag() {
        return bruttoBeregningsgrunnlag;
    }
}
