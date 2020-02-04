package no.nav.folketrygdloven.beregningsgrunnlag.rest.dto;

import java.math.BigDecimal;

import no.nav.k9.kodeverk.iay.AktivitetStatus;
import no.nav.k9.kodeverk.iay.Inntektskategori;

public class AndelForFaktaOmBeregningDto {

    private BigDecimal belopReadOnly;
    private BigDecimal fastsattBelop;
    private Inntektskategori inntektskategori;
    private AktivitetStatus aktivitetStatus;
    private BigDecimal refusjonskrav;
    private String visningsnavn;
    private BeregningsgrunnlagArbeidsforholdDto arbeidsforhold;
    private Long andelsnr;
    private Boolean skalKunneEndreAktivitet;
    private Boolean lagtTilAvSaksbehandler;

    public BeregningsgrunnlagArbeidsforholdDto getArbeidsforhold() {
        return arbeidsforhold;
    }

    public void setArbeidsforhold(BeregningsgrunnlagArbeidsforholdDto arbeidsforhold) {
        this.arbeidsforhold = arbeidsforhold;
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public void setAktivitetStatus(AktivitetStatus aktivitetStatus) {
        this.aktivitetStatus = aktivitetStatus;
    }

    public Long getAndelsnr() {
        return andelsnr;
    }

    public void setAndelsnr(Long andelsnr) {
        this.andelsnr = andelsnr;
    }

    public String getVisningsnavn() {
        return visningsnavn;
    }

    public void setVisningsnavn(String visningsnavn) {
        this.visningsnavn = visningsnavn;
    }

    public BigDecimal getRefusjonskrav() {
        return refusjonskrav;
    }

    public void setRefusjonskrav(BigDecimal refusjonskrav) {
        this.refusjonskrav = refusjonskrav;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public void setInntektskategori(Inntektskategori inntektskategori) {
        this.inntektskategori = inntektskategori;
    }

    public BigDecimal getBelopReadOnly() {
        return belopReadOnly;
    }

    public void setBelopReadOnly(BigDecimal belopReadOnly) {
        this.belopReadOnly = belopReadOnly;
    }

    public BigDecimal getFastsattBelop() {
        return fastsattBelop;
    }

    public void setFastsattBelop(BigDecimal fastsattBelop) {
        this.fastsattBelop = fastsattBelop;
    }

    public Boolean getSkalKunneEndreAktivitet() {
        return skalKunneEndreAktivitet;
    }

    public void setSkalKunneEndreAktivitet(Boolean skalKunneEndreAktivitet) {
        this.skalKunneEndreAktivitet = skalKunneEndreAktivitet;
    }

    public Boolean getLagtTilAvSaksbehandler() {
        return lagtTilAvSaksbehandler;
    }

    public void setLagtTilAvSaksbehandler(Boolean lagtTilAvSaksbehandler) {
        this.lagtTilAvSaksbehandler = lagtTilAvSaksbehandler;
    }
}
