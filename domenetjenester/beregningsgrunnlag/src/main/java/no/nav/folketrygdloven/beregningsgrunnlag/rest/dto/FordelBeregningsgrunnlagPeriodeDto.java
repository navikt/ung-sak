package no.nav.folketrygdloven.beregningsgrunnlag.rest.dto;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FordelBeregningsgrunnlagPeriodeDto {

    @JsonProperty(value = "fom", required = true)
    @NotNull
    private LocalDate fom;

    @JsonProperty(value = "tom", required = true)
    @NotNull
    private LocalDate tom;

    @JsonProperty(value = "fordelBeregningsgrunnlagAndeler", required = true)
    @NotNull
    @Valid
    @Size(max = 200)
    private List<FordelBeregningsgrunnlagAndelDto> fordelBeregningsgrunnlagAndeler = new ArrayList<>();
    
    @JsonProperty(value="harPeriodeAarsakGraderingEllerRefusjon", required = true)
    @NotNull
    private boolean harPeriodeAarsakGraderingEllerRefusjon = false;
    
    @JsonProperty(value="skalKunneEndreRefuson", required = true)
    @NotNull
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
