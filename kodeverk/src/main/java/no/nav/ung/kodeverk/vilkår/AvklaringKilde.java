package no.nav.ung.kodeverk.vilkår;

/**
 * Hvor Nav har fått opplysningene som ligger til grunn for en vilkårsavklaring.
 * Alle vilkår har en kilde, men utvalget av gyldige koder er vilkårsspesifikt
 */
public interface AvklaringKilde {

    String getKode();

    boolean kreverFritekst();

    static AvklaringKilde fraKode(VilkårType vilkårType, String kode) {
        if (kode == null) {
            return null;
        }
        return switch (vilkårType) {
            case BOSTEDSVILKÅR -> BostedsavklaringKildeType.fraKode(kode);
            case BISTANDSVILKÅR -> BistandsavklaringKildeType.fraKode(kode);
            default -> throw new IllegalArgumentException("Vilkår " + vilkårType + " har ingen avklaringskilder");
        };
    }
}
