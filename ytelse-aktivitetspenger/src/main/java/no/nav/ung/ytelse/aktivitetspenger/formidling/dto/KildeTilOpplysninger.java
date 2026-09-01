package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

import no.nav.ung.kodeverk.vilkår.BostedsavklaringKildeType;

public record KildeTilOpplysninger(
    boolean fraBruker,
    boolean fraFolkeregisteret,
    String annet
) {

    public static KildeTilOpplysninger av(BostedsavklaringKildeType kilde, String kildeFritekst) {
        if (kilde == null) {
            return null;
        }
        return switch (kilde) {
            case BRUKER -> new KildeTilOpplysninger(true, false, null);
            case FOLKEREGISTER -> new KildeTilOpplysninger(false, true, null);
            case ANNET -> new KildeTilOpplysninger(false, false, kildeFritekst);
        };
    }
}
