package no.nav.ung.sak.behandlingslager.bosatt;

import no.nav.ung.kodeverk.vilkår.AvklaringStatus;

import java.util.List;
import java.util.Set;

/**
 * Skrivebeskyttet visning av {@link BostedsAvklaringHolder}. Skal kun eksponere metoder som lesing periodeavklaringer uten muteringer.
 * Mutasjon ({@code leggTilEllerErstattPeriodeAvklaringerUnderArbeid}/{@code settAlleAvklaringerTilFerdig}) skal
 * kun skje gjennom settere på {@link BostedsGrunnlag}, som sørger for korrekt kopiering/deduplisering av holder-instanser.
 */
public interface BostedsAvklaringHolderSkrivebeskyttet {

    Long getId();

    Set<BostedsPeriodeAvklaring> hentPeriodeAvklaringer();

    List<BostedsPeriodeAvklaring> hentPeriodeAvklaringerMedStatus(AvklaringStatus... status);
}
