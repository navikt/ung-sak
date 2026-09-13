package no.nav.ung.kodeverk.vilkår;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

public enum AndreLivsoppholdsytelserIkkeOppfyltÅrsak implements IkkeOppfyltDetaljertÅrsak {

    HAR_ANNEN_LIVSOPPHOLDSYTELSE(Avslagsårsak.SØKER_HAR_ANNEN_LIVSOPPHOLDSYTELSE, false),
    // Saksbehandler har valgt å innvilge periode som er kortere enn perioden saksbehandlingssystemet tillater å innvilge.
    AVKORTET(Avslagsårsak.AVKORTET, false),
    UDEFINERT(null, false),
    ;

    private final Avslagsårsak avslagsårsak;
    private final boolean kreverFritekst;

    AndreLivsoppholdsytelserIkkeOppfyltÅrsak(Avslagsårsak avslagsårsak, boolean kreverFritekst) {
        this.avslagsårsak = avslagsårsak;
        this.kreverFritekst = kreverFritekst;
    }

    public static AndreLivsoppholdsytelserIkkeOppfyltÅrsak fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        try {
            return valueOf(kode);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Ukjent AndreLivsoppholdsytelserIkkeOppfyltÅrsak: " + kode, e);
        }
    }

    @JsonValue
    @Override
    public String getKode() {
        return name();
    }

    @Override
    public Optional<Avslagsårsak> avslagsårsak() {
        return Optional.ofNullable(avslagsårsak);
    }

    @Override
    public boolean kreverFritekst() {
        return kreverFritekst;
    }
}
