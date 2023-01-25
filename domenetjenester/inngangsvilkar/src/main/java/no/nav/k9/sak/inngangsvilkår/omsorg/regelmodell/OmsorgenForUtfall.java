package no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell;

import java.util.Arrays;

public enum OmsorgenForUtfall {
    HAR, HAR_IKKE, UKJENT;

    public static OmsorgenForUtfall find(String kode) {
        return Arrays.stream(values())
            .filter(it -> it.name().equals(kode))
            .findFirst()
            .orElse(UKJENT);
    }
}
