package no.nav.ung.sak.inngangsvilkår.avklaring;

import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

// Record som kun inneholder et minimalt sett med egenskaper som er delt av alle vilkårsavklaringer på tvers av vilkår
public record Vilkårsavklaring(
    Avklaringtype avklaringtype,
    DatoIntervallEntitet periode
) {
}
