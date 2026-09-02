package no.nav.ung.kodeverk.vilkår;

import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Optional;

// Dette er en mer detaljert årsak brukt for å spesifisere hvorfor en avslagsårsak er valgt.
// Disse vil i seg selv ikke være vurderte avslagsårsaker, men avslagsårsak kan som oftest utledes fra dem.
public interface IkkeOppfyltDetaljertÅrsak extends Kodeverdi {

    Optional<Avslagsårsak> avslagsårsak();

    boolean krevesFritekst();

    boolean erGyldigAvklaringsårsak();
}
