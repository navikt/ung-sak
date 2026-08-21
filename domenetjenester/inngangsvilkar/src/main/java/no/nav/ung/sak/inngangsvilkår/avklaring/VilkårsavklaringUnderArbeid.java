package no.nav.ung.sak.inngangsvilkår.avklaring;

import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

/**
 * Representerer den seneste vilkårsavklaringen som er under arbeid for en {@link VilkårsavklaringTjeneste},
 * med perioden avklaringen gjelder for. Perioden brukes bl.a. til å sjekke om det finnes avslåtte vilkårsperioder
 * som overlapper avklaringen.
 */
public record VilkårsavklaringUnderArbeid(Avklaringtype avklaringtype, DatoIntervallEntitet periode) {
}
