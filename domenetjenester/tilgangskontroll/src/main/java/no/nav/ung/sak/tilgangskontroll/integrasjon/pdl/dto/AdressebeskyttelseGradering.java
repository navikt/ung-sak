package no.nav.ung.sak.tilgangskontroll.integrasjon.pdl.dto;

import java.util.Arrays;

public enum AdressebeskyttelseGradering {
    STRENGT_FORTROLIG_UTLAND("STRENGT_FORTROLIG_UTLAND"),
    STRENGT_FORTROLIG("STRENGT_FORTROLIG"),
    FORTROLIG("FORTROLIG"),
    UGRADERT("UGRADERT");

    private final String kode;

    AdressebeskyttelseGradering(String kode) {
        this.kode = kode;
    }

    public String toString() {
        return this.kode;
    }

    public static AdressebeskyttelseGradering fraKode(String kode) {
        return Arrays.stream(values())
            .filter(it->it.kode.equals(kode))
            .findFirst()
            .orElseThrow();
    }
}
