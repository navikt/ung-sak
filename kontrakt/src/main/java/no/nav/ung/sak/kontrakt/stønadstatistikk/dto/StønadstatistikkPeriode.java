package no.nav.ung.sak.kontrakt.stønadstatistikk.dto;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
    @Valid
    private BigDecimal uttaksgrad;

    @JsonProperty(value = "utbetalingsgrader", required = true)
    @Size(max = 1000)
    @NotNull
    @Valid
    private List<StønadstatistikkUtbetalingsgrad> utbetalingsgrader;

    @JsonProperty(value = "søkersTapteArbeidstid")
    @Valid
    private BigDecimal søkersTapteArbeidstid;

    @JsonProperty(value = "oppgittTilsyn")
    @Valid
    private Duration oppgittTilsyn;

    @JsonProperty(value = "årsaker", required = true)
    @Size(max = 100)
    @NotNull
    @Valid
    private List<String> årsaker;

    @JsonProperty(value = "inngangsvilkår", required = true)
    @Size(max = 1000)
    @NotNull
    @Valid
    private List<StønadstatistikkInngangsvilkår> inngangsvilkår;

    @JsonProperty(value = "pleiebehov")
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

    @JsonProperty(value = "totalUtbetalingsgrad")
    @Valid
    private BigDecimal totalUtbetalingsgrad;

    @JsonProperty(value = "totalUtbetalingsgradFraUttak")
    @Valid
    private BigDecimal totalUtbetalingsgradFraUttak;

    @JsonProperty(value = "totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt")
    @Valid
    private BigDecimal totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt;


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
                                   BigDecimal bruttoBeregningsgrunnlag,
                                   BigDecimal totalUtbetalingsgradFraUttak,
                                   BigDecimal totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt) {
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
        this.totalUtbetalingsgradFraUttak = totalUtbetalingsgradFraUttak;
        this.totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt = totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt;

        if (totalUtbetalingsgradFraUttak != null
                && totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt != null
                && totalUtbetalingsgradFraUttak.compareTo(totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt) > 0){
            totalUtbetalingsgrad = totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt;
        } else {
            totalUtbetalingsgrad = totalUtbetalingsgradFraUttak;
        }
    }

    public static StønadstatistikkPeriode forOmsorgspenger(LocalDate fom,
                                                           LocalDate tom,
                                                           StønadstatistikkUtfall utfall,
                                                           List<StønadstatistikkUtbetalingsgrad> utbetalingsgrader,
                                                           List<StønadstatistikkInngangsvilkår> inngangsvilkår,
                                                           BigDecimal bruttoBeregningsgrunnlag) {
        StønadstatistikkPeriode sp = new StønadstatistikkPeriode();
        sp.fom = fom;
        sp.tom = tom;
        sp.utfall = utfall;
        sp.uttaksgrad = null; //tror ikke det er verdifullt å forsøke å lage et samlet tall basert på ulike arbeidsforhold
        sp.utbetalingsgrader = utbetalingsgrader;
        sp.inngangsvilkår = inngangsvilkår;
        sp.bruttoBeregningsgrunnlag = bruttoBeregningsgrunnlag;

        sp.årsaker = List.of();
        return sp;
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

    public BigDecimal getTotalUtbetalingsgrad() {
        return totalUtbetalingsgrad;
    }

    public BigDecimal getTotalUtbetalingsgradFraUttak() {
        return totalUtbetalingsgradFraUttak;
    }

    public BigDecimal getTotalUtbetalingsgradEtterReduksjonVedTilkommetInntekt() {
        return totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt;
    }
}
