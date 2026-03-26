package no.nav.ung.sak.kontrakt.vilkår.medlemskap;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.kontrakt.aktivitetspenger.medlemskap.MedlemskapAvslagsÅrsakType;

import java.util.List;

public record ForutgåendeMedlemskapResponse(
    @JsonProperty("medlemskapFraBruker")
    List<MedlemskapsPeriodeDto> medlemskapFraBruker,
    @JsonProperty(value = "vilkårUtfall")
    Utfall vilkårUtfall,
    @JsonProperty(value = "avslagsårsak")
    MedlemskapAvslagsÅrsakType avslagsårsak

) {
}
