package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

import no.nav.ung.kodeverk.vilkår.BostedsavklaringKildeType;

/**
 * Hvor saksbehandler fikk opplysningene fra. Feltene er gjensidig utelukkende og styrer
 * hvilken variant av «Vi har fått opplysninger om dette fra …» brevmalen viser.
 */
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
