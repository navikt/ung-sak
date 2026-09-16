package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

import no.nav.ung.kodeverk.vilkår.AvklaringKilde;
import no.nav.ung.kodeverk.vilkår.BistandsavklaringKildeType;
import no.nav.ung.kodeverk.vilkår.BostedsavklaringKildeType;

public record KildeTilOpplysninger(
    boolean fraBruker,
    boolean fraFolkeregisteret,
    boolean fraNav,
    String annet
) {

    public static KildeTilOpplysninger nyFraBruker() {
        return new KildeTilOpplysninger(true, false, false, null);
    }

    public static KildeTilOpplysninger nyFraFolkeregisteret() {
        return new KildeTilOpplysninger(false, true, false, null);
    }

    public static KildeTilOpplysninger nyFraNav() {
        return new KildeTilOpplysninger(false, false, true, null);
    }

    public static KildeTilOpplysninger nyFraAnnet(String annet) {
        return new KildeTilOpplysninger(false, false, false, annet);
    }

    public static KildeTilOpplysninger av(AvklaringKilde kilde, String kildeFritekst) {
        if (kilde == null) {
            return null;
        }
        return switch (kilde) {
            case BostedsavklaringKildeType bosted -> switch (bosted) {
                case BRUKER -> nyFraBruker();
                case FOLKEREGISTER -> nyFraFolkeregisteret();
                case ANNET -> nyFraAnnet(kildeFritekst);
            };
            case BistandsavklaringKildeType bistand -> switch (bistand) {
                case BRUKER -> nyFraBruker();
                case NAV -> nyFraNav();
                case ANNET -> nyFraAnnet(kildeFritekst);
            };
            // Nye vilkår kobles på etter hvert som brevinnholdet for dem er på plass.
            default -> throw new IllegalArgumentException("Vedtaksbrev støtter ikke kildetypen " + kilde.getClass().getSimpleName() + "." + kilde.getKode());
        };
    }
}
