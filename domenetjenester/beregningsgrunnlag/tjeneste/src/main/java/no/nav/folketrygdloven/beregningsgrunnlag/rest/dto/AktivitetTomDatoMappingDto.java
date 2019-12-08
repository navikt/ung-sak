package no.nav.folketrygdloven.beregningsgrunnlag.rest.dto;

import java.time.LocalDate;
import java.util.List;

public class AktivitetTomDatoMappingDto {
    private LocalDate tom;
    private List<BeregningAktivitetDto> aktiviteter;

    public LocalDate getTom() {
        return tom;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }

    public List<BeregningAktivitetDto> getAktiviteter() {
        return aktiviteter;
    }

    public void setAktiviteter(List<BeregningAktivitetDto> aktiviteter) {
        this.aktiviteter = aktiviteter;
    }
}
