package no.nav.ung.sak.kontrakt.vilkår.medlemskap;

import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.kontrakt.aktivitetspenger.medlemskap.MedlemskapAvslagsÅrsakType;
import no.nav.ung.sak.typer.Periode;

public record MedlemskapPeriodeInfoDto(
    Periode periode,
    Utfall utfall,
    MedlemskapAvslagsÅrsakType avslagsårsak,
    String begrunnelse,
    boolean vurderesIBehandlingen,
    boolean erManueltVurdert,
    MedlemskapDto medlemskapFraBruker
) {
}
