package no.nav.k9.sak.hendelse.stønadstatistikk.dto;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.vilkår.Utfall;

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
    private int uttaksgrad;
    
    @JsonProperty(value = "utbetalingsgrader", required = true)
    @Size(max=1000)
    @NotNull
    @Valid
    private List<StønadstatistikkUtbetalingsgrad> utbetalingsgrader;
    
    @JsonProperty(value = "søkersTapteArbeidstid", required = true)
    @NotNull
    @Valid
    private int søkersTapteArbeidstid;
    
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
    private int pleiebehov;
    //private StønadstatistikkGraderingMotTilsyn graderingMotTilsyn;
    //private StønadstatistikkNattevåk nattevåk;
    //private StønadstatistikkBeredskap beredskap;
    //private Duration søkersTapteTimer;
    
    @JsonProperty(value = "bruttoBeregningsgrunnlag", required = true)
    @NotNull
    @Valid
    private long bruttoBeregningsgrunnlag;

    
    public StønadstatistikkPeriode() {
        
    }
            
    public StønadstatistikkPeriode(LocalDate fom,
            LocalDate tom,
            StønadstatistikkUtfall utfall,
            int uttaksgrad,
            List<StønadstatistikkUtbetalingsgrad> utbetalingsgrader,
            int søkersTapteArbeidstid,
            Duration oppgittTilsyn,
            List<String> årsaker,
            List<StønadstatistikkInngangsvilkår> inngangsvilkår,
            int pleiebehov,
            long bruttoBeregningsgrunnlag) {
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
        this.bruttoBeregningsgrunnlag = bruttoBeregningsgrunnlag;
    }
    
    
    
}
