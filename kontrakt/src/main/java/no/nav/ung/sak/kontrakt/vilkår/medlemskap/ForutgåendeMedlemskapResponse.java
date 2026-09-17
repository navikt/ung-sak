package no.nav.ung.sak.kontrakt.vilkår.medlemskap;

import java.util.List;

public record ForutgåendeMedlemskapResponse(
    MedlemskapDto medlemskapFraBruker,
    List<MedlemskapPeriodeResultatDto> resultater
) {
}
