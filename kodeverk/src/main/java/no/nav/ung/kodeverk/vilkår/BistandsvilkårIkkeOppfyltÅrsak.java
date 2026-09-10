package no.nav.ung.kodeverk.vilkår;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

public enum BistandsvilkårIkkeOppfyltÅrsak implements IkkeOppfyltDetaljertÅrsak {

    // Søker har ikke oppfølgingsvedtak etter Navloven §14a.
    IKKE_14A_VEDTAK(Avslagsårsak.IKKE_14A_VEDTAK, true),
    // Saksbehandler har valgt å innvilge periode som er kortere enn perioden saksbehandlingssystemet tillater å innvilge.
    AVKORTET(Avslagsårsak.AVKORTET, false),
    UDEFINERT(null, false),
    ;

    private final Avslagsårsak avslagsårsak;
    private final boolean kreverFritekst;

    BistandsvilkårIkkeOppfyltÅrsak(Avslagsårsak avslagsårsak, boolean kreverFritekst) {
        this.avslagsårsak = avslagsårsak;
        this.kreverFritekst = kreverFritekst;
    }

    @Override
    public Optional<Avslagsårsak> avslagsårsak() {
        return Optional.ofNullable(avslagsårsak);
    }

    @Override
    public boolean kreverFritekst() {
        return kreverFritekst;
    }

    public static BistandsvilkårIkkeOppfyltÅrsak fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        try {
            return valueOf(kode);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Ukjent BistandsvilkårIkkeOppfyltÅrsak: " + kode, e);
        }
    }

    @JsonValue
    @Override
    public String getKode() {
        return name();
    }
}
