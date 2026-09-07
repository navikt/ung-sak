package no.nav.ung.kodeverk.vilkår;

import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Optional;

// Dette er en mer detaljert årsak brukt for å spesifisere hvorfor en avslagsårsak er valgt.
// Disse vil i seg selv ikke være vurderte avslagsårsaker, men avslagsårsak kan som oftest utledes fra dem.
public interface IkkeOppfyltDetaljertÅrsak extends Kodeverdi {

    String getKode();

    Optional<Avslagsårsak> avslagsårsak();

    boolean krevesFritekst();

    static IkkeOppfyltDetaljertÅrsak fraKode(VilkårType vilkårType, String kode) {
        if (kode == null) {
            return null;
        }
        return switch (vilkårType) {
            case BOSTEDSVILKÅR -> BostedsvilkårIkkeOppfyltÅrsak.fraKode(kode);
            case BISTANDSVILKÅR -> BistandsvilkårIkkeOppfyltÅrsak.fraKode(kode);
            case AKTIVITETSVILKÅR -> AktivitetsvilkåretIkkeOppfyltÅrsak.fraKode(kode);
            case ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR -> AndreLivsoppholdsytelserIkkeOppfyltÅrsak.fraKode(kode);
            default -> throw new IllegalArgumentException("Vilkår " + vilkårType + " har ingen detaljerte ikke-oppfylt-årsaker");
        };
    }
}
