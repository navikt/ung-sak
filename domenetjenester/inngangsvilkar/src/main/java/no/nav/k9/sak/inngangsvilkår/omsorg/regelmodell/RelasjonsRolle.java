package no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell;

import java.util.Arrays;

public enum RelasjonsRolle {
    BARN, UKJENT;

    public static RelasjonsRolle find(String kode) {
        return Arrays.stream(values())
            .filter(it -> it.name().equals(kode))
            .findFirst()
            .orElse(UKJENT);
    }
}
