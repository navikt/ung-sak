package no.nav.folketrygdloven.beregningsgrunnlag.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import no.nav.k9.kodeverk.beregningsgrunnlag.Hjemmel;
import no.nav.k9.kodeverk.iay.AktivitetStatus;

public class BeregningsgrunnlagDto {

    private LocalDate skjaeringstidspunktBeregning;
    private LocalDate skjæringstidspunkt;
    private List<AktivitetStatus> aktivitetStatus;
    private List<BeregningsgrunnlagPeriodeDto> beregningsgrunnlagPeriode;
    private SammenligningsgrunnlagDto sammenligningsgrunnlag;
    private List<SammenligningsgrunnlagDto> sammenligningsgrunnlagPrStatus;
    private String ledetekstBrutto;
    private String ledetekstAvkortet;
    private String ledetekstRedusert;
    private Double halvG;
    private BigDecimal grunnbeløp;
    private FaktaOmBeregningDto faktaOmBeregning;
    private List<BeregningsgrunnlagPrStatusOgAndelDto> andelerMedGraderingUtenBG;
    private Hjemmel hjemmel;
    private FordelingDto faktaOmFordeling;
    private BigDecimal årsinntektVisningstall;
    private int dekningsgrad;

    public BeregningsgrunnlagDto() {
        // trengs for deserialisering av JSON
    }

    public LocalDate getSkjaeringstidspunktBeregning() {
        return skjaeringstidspunktBeregning;
    }

    public List<AktivitetStatus> getAktivitetStatus() {
        return aktivitetStatus;
    }

    public List<BeregningsgrunnlagPeriodeDto> getBeregningsgrunnlagPeriode() {
        return beregningsgrunnlagPeriode;
    }

    public String getLedetekstBrutto() {
        return ledetekstBrutto;
    }

    public String getLedetekstAvkortet() {
        return ledetekstAvkortet;
    }

    public String getLedetekstRedusert() {
        return ledetekstRedusert;
    }

    public SammenligningsgrunnlagDto getSammenligningsgrunnlag() {
        return sammenligningsgrunnlag;
    }

    public Double getHalvG() {
        return halvG;
    }

    public FordelingDto getFaktaOmFordeling() {
        return faktaOmFordeling;
    }

    public FaktaOmBeregningDto getFaktaOmBeregning() {
        return faktaOmBeregning;
    }

    public Hjemmel getHjemmel() {
        return hjemmel;
    }

    public List<SammenligningsgrunnlagDto> getSammenligningsgrunnlagPrStatus() {
        return sammenligningsgrunnlagPrStatus;
    }

    public void setSkjaeringstidspunktBeregning(LocalDate skjaeringstidspunktBeregning) {
        this.skjaeringstidspunktBeregning = skjaeringstidspunktBeregning;
    }

    public void setAktivitetStatus(List<AktivitetStatus> aktivitetStatus) {
        this.aktivitetStatus = aktivitetStatus;
    }

    public void setBeregningsgrunnlagPeriode(List<BeregningsgrunnlagPeriodeDto> perioder) {
        this.beregningsgrunnlagPeriode = perioder;
    }

    public void setSammenligningsgrunnlag(SammenligningsgrunnlagDto sammenligningsgrunnlag) {
        this.sammenligningsgrunnlag = sammenligningsgrunnlag;
    }

    public void setLedetekstBrutto(String ledetekstBrutto) {
        this.ledetekstBrutto = ledetekstBrutto;
    }

    public void setLedetekstAvkortet(String ledetekstAvkortet) {
        this.ledetekstAvkortet = ledetekstAvkortet;
    }

    public void setLedetekstRedusert(String ledetekstRedusert) {
        this.ledetekstRedusert = ledetekstRedusert;
    }

    public void setHalvG(Double halvG) {
        this.halvG = halvG;
    }

    public void setFaktaOmBeregning(FaktaOmBeregningDto faktaOmBeregning) {
        this.faktaOmBeregning = faktaOmBeregning;
    }

    public List<BeregningsgrunnlagPrStatusOgAndelDto> getAndelerMedGraderingUtenBG() {
        return andelerMedGraderingUtenBG;
    }

    public void setAndelerMedGraderingUtenBG(List<BeregningsgrunnlagPrStatusOgAndelDto> andelerMedGraderingUtenBG) {
        this.andelerMedGraderingUtenBG = andelerMedGraderingUtenBG;
    }

    public void setHjemmel(Hjemmel hjemmel) {
        this.hjemmel = hjemmel;
    }

    public void setFaktaOmFordeling(FordelingDto faktaOmFordelingDto) {
        this.faktaOmFordeling = faktaOmFordelingDto;
    }

    public BigDecimal getÅrsinntektVisningstall() {
        return årsinntektVisningstall;
    }

    public void setÅrsinntektVisningstall(BigDecimal årsinntektVisningstall) {
        this.årsinntektVisningstall = årsinntektVisningstall;
    }

    public int getDekningsgrad() {
        return dekningsgrad;
    }

    public void setDekningsgrad(int dekningsgrad) {
        this.dekningsgrad = dekningsgrad;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public void setSkjæringstidspunkt(LocalDate skjæringstidspunkt) {
        this.skjæringstidspunkt = skjæringstidspunkt;
    }

    public BigDecimal getGrunnbeløp() {
        return grunnbeløp;
    }

    public void setGrunnbeløp(BigDecimal grunnbeløp) {
        this.grunnbeløp = grunnbeløp;
    }

    public void setSammenligningsgrunnlagPrStatus(List<SammenligningsgrunnlagDto> sammenligningsgrunnlagPrStatus) {
        this.sammenligningsgrunnlagPrStatus = sammenligningsgrunnlagPrStatus;
    }
}
