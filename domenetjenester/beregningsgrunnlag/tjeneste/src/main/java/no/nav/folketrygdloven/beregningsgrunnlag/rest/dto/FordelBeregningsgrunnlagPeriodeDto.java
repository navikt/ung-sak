package no.nav.folketrygdloven.beregningsgrunnlag.rest.dto;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FordelBeregningsgrunnlagPeriodeDto {

    private LocalDate fom;
    private LocalDate tom;
    private List<FordelBeregningsgrunnlagAndelDto> fordelBeregningsgrunnlagAndeler = new ArrayList<>();
    private boolean harPeriodeAarsakGraderingEllerRefusjon = false;
    private boolean skalKunneEndreRefusjon = false;


    public boolean isHarPeriodeAarsakGraderingEllerRefusjon() {
        return harPeriodeAarsakGraderingEllerRefusjon;
    }

    public void setHarPeriodeAarsakGraderingEllerRefusjon(boolean harPeriodeAarsakGraderingEllerRefusjon) {
        this.harPeriodeAarsakGraderingEllerRefusjon = harPeriodeAarsakGraderingEllerRefusjon;
    }

    public LocalDate getFom() {
        return fom;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public void setTom(LocalDate tom) {
        if (!TIDENES_ENDE.equals(tom)) {
            this.tom = tom;
        }
    }

    public void setFordelBeregningsgrunnlagAndeler(List<FordelBeregningsgrunnlagAndelDto> fordelBeregningsgrunnlagAndeler) {
        this.fordelBeregningsgrunnlagAndeler = fordelBeregningsgrunnlagAndeler;
    }

    public List<FordelBeregningsgrunnlagAndelDto> getFordelBeregningsgrunnlagAndeler() {
        return fordelBeregningsgrunnlagAndeler;
    }

    public boolean isSkalKunneEndreRefusjon() {
        return skalKunneEndreRefusjon;
    }

    public void setSkalKunneEndreRefusjon(boolean skalKunneEndreRefusjon) {
        this.skalKunneEndreRefusjon = skalKunneEndreRefusjon;
    }
}
