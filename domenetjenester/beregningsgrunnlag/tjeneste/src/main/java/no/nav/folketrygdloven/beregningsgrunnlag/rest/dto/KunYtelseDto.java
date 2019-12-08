package no.nav.folketrygdloven.beregningsgrunnlag.rest.dto;

import java.util.ArrayList;
import java.util.List;

public class KunYtelseDto {

    private List<AndelMedBeløpDto> andeler = new ArrayList<>();

    public List<AndelMedBeløpDto> getAndeler() {
        return andeler;
    }

    public void setAndeler(List<AndelMedBeløpDto> andeler) {
        this.andeler = andeler;
    }

    public void leggTilAndel(AndelMedBeløpDto andel) {
        andeler.add(andel);
    }
}
