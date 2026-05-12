package no.nav.ung.kodeverk.bosatt;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Årsak til at saksbehandler vurderer bruker som fraflyttet fra Trondheim.
 * Gjelder for perioder der bruker enten aldri var bosatt eller har fraflyttingsdato.
 */
public enum FraflyttingsÅrsak {

    IKKE_BOSATTADRESSE_I_TRONDHEIM("IKKE_BOSATTADRESSE_I_TRONDHEIM", "Ikke bosattadresse i Trondheim"),
    IKKE_BOSTEDSADRESSE_OG_IKKE_FOLKEREGISTRERT_I_TRONDHEIM("IKKE_BOSTEDSADRESSE_OG_IKKE_FOLKEREGISTRERT_I_TRONDHEIM", "Ikke bostedsadresse i Trondheim og ikke folkeregistrert i Trondheim"),
    STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM("STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM", "Har studie- eller arbeidssted utenfor Trondheim"),
    ANNET("ANNET", "Annet");

    private final String kode;
    private final String beskrivelse;

    FraflyttingsÅrsak(String kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
    }

    @JsonValue
    public String getKode() {
        return kode;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    @Override
    public String toString() {
        return kode;
    }
}
