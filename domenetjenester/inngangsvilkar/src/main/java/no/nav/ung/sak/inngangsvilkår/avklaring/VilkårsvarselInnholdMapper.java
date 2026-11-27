package no.nav.ung.sak.inngangsvilkår.avklaring;

import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsvarselInnhold;

/**
 * Hvert vilkår eier hva som vises for bruker i varselet. Likheten på innholdet avgjør om en avklaring må varsles på nytt.
 */
@FunctionalInterface
public interface VilkårsvarselInnholdMapper {

    VilkårsvarselInnhold tilVarselInnhold(VilkårPeriodeAvklaring avklaring);
}
