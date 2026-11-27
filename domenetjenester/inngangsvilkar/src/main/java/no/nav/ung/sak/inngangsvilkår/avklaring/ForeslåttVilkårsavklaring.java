package no.nav.ung.sak.inngangsvilkår.avklaring;

import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaringForeslått;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsvarselInnhold;

import java.util.UUID;

/**
 * En vilkårsavklaring slik den skal lagres, før den har fått referanse. Referansen settes av
 * {@link VilkårsavklaringTjeneste}, som gjenbruker referansen til en eksisterende avklaring når {@link #innhold()} er uendret.
 */
public interface ForeslåttVilkårsavklaring {

    VilkårsvarselInnhold innhold();

    VilkårPeriodeAvklaringForeslått tilEntitet(UUID referanse);
}
