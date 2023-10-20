package no.nav.k9.kodeverk.notat;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;

import no.nav.k9.kodeverk.api.Kodeverdi;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public enum NotatGjelderType implements Kodeverdi {
    FAGSAK("FAGSAK", "Fagsak"), PLEIETRENGENDE("PLEIETRENGENDE", "Pleietrengende") ;

    private final String kode;
    private final String navn;

    private static final String KODEVERK = "NOTAT_GJELDER";

    NotatGjelderType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

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
