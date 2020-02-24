package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.uttak.Tid;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FordelBeregningsgrunnlagPeriodeDto {

    @JsonProperty(value = "fom", required = true)
    @NotNull
    private LocalDate fom;

    @JsonProperty(value = "fordelBeregningsgrunnlagAndeler", required = true)
    @NotNull
    @Valid
    @Size(max = 200)
    private List<FordelBeregningsgrunnlagAndelDto> fordelBeregningsgrunnlagAndeler = new ArrayList<>();

    @JsonProperty(value = "harPeriodeAarsakGraderingEllerRefusjon", required = true)
    @NotNull
    private boolean harPeriodeAarsakGraderingEllerRefusjon = false;

    @JsonProperty(value = "skalKunneEndreRefuson", required = true)
    @NotNull
    private boolean skalKunneEndreRefusjon = false;

    @JsonProperty(value = "tom", required = true)
    @NotNull
    private LocalDate tom;

    public LocalDate getFom() {
        return fom;
    }

    public List<FordelBeregningsgrunnlagAndelDto> getFordelBeregningsgrunnlagAndeler() {
        return Collections.unmodifiableList(fordelBeregningsgrunnlagAndeler);
    }

    public LocalDate getTom() {
        return tom;
    }

    public boolean isHarPeriodeAarsakGraderingEllerRefusjon() {
        return harPeriodeAarsakGraderingEllerRefusjon;
    }

    public boolean isSkalKunneEndreRefusjon() {
        return skalKunneEndreRefusjon;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public void setFordelBeregningsgrunnlagAndeler(List<FordelBeregningsgrunnlagAndelDto> fordelBeregningsgrunnlagAndeler) {
        this.fordelBeregningsgrunnlagAndeler = List.copyOf(fordelBeregningsgrunnlagAndeler);
    }

    public void setHarPeriodeAarsakGraderingEllerRefusjon(boolean harPeriodeAarsakGraderingEllerRefusjon) {
        this.harPeriodeAarsakGraderingEllerRefusjon = harPeriodeAarsakGraderingEllerRefusjon;
    }

    public void setSkalKunneEndreRefusjon(boolean skalKunneEndreRefusjon) {
        this.skalKunneEndreRefusjon = skalKunneEndreRefusjon;
    }

    public void setTom(LocalDate tom) {
        if (!Tid.TIDENES_ENDE.equals(tom)) {
            this.tom = tom;
        }
    }
}
