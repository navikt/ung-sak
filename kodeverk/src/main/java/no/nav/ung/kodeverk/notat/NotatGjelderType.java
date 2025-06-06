package no.nav.ung.kodeverk.notat;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

public enum NotatGjelderType implements Kodeverdi {
    FAGSAK("FAGSAK", "Fagsak");

    private final String kode;
    private final String navn;

    private static final String KODEVERK = "NOTAT_GJELDER";

    NotatGjelderType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonValue
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getNavn() {
        return navn;
    }
}
