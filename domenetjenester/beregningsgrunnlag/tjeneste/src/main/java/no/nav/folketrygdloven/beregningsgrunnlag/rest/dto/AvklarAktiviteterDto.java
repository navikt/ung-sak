package no.nav.folketrygdloven.beregningsgrunnlag.rest.dto;

import java.util.List;

public class AvklarAktiviteterDto {

    private List<AktivitetTomDatoMappingDto> aktiviteterTomDatoMapping;

    public List<AktivitetTomDatoMappingDto> getAktiviteterTomDatoMapping() {
        return aktiviteterTomDatoMapping;
    }

    public void setAktiviteterTomDatoMapping(List<AktivitetTomDatoMappingDto> aktiviteterTomDatoMapping) {
        this.aktiviteterTomDatoMapping = aktiviteterTomDatoMapping;
    }
}
