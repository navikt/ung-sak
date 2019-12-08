package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto;

import javax.validation.constraints.NotNull;

public class VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto {

    @NotNull
    private Boolean erNyIArbeidslivet;

    VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto() {
        // For Jackson
    }

    public VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto(Boolean erNyIArbeidslivet) { // NOSONAR
        this.erNyIArbeidslivet = erNyIArbeidslivet;
    }

    public void setErNyIArbeidslivet(Boolean erNyIArbeidslivet) {
        this.erNyIArbeidslivet = erNyIArbeidslivet;
    }

    public Boolean erNyIArbeidslivet() {
        return erNyIArbeidslivet;
    }
}
