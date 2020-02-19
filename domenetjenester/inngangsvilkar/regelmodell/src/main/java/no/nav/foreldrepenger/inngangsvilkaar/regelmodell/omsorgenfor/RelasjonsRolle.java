package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.omsorgenfor;

import java.util.Arrays;

public enum RelasjonsRolle {
    MORA, FARA, UKJENT;

    public static RelasjonsRolle find(String kode) {
        return Arrays.stream(values())
            .filter(it -> it.name().equals(kode))
            .findFirst()
            .orElse(UKJENT);
    }
}
