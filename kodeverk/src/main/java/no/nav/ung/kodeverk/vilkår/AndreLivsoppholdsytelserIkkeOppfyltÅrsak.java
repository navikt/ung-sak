package no.nav.ung.kodeverk.vilkår;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

public enum AndreLivsoppholdsytelserIkkeOppfyltÅrsak implements IkkeOppfyltDetaljertÅrsak {

    MOTTAR_ARBEIDSAVKLARINGSPENGER(Avslagsårsak.SØKER_HAR_ANNEN_LIVSOPPHOLDSYTELSE, false),
    MOTTAR_TILTAKSPENGER(Avslagsårsak.SØKER_HAR_ANNEN_LIVSOPPHOLDSYTELSE, false),
    MOTTAR_KVALIFISERINGSSTØNAD(Avslagsårsak.SØKER_HAR_ANNEN_LIVSOPPHOLDSYTELSE, false),
    MOTTAR_DAGPENGER(Avslagsårsak.SØKER_HAR_ANNEN_LIVSOPPHOLDSYTELSE, false),
    MOTTAR_FORELDREPENGER(Avslagsårsak.SØKER_HAR_ANNEN_LIVSOPPHOLDSYTELSE, false),
    MOTTAR_SVANGERSKAPSPENGER(Avslagsårsak.SØKER_HAR_ANNEN_LIVSOPPHOLDSYTELSE, false),
    MOTTAR_UFØRETRYGD(Avslagsårsak.SØKER_HAR_ANNEN_LIVSOPPHOLDSYTELSE, false),
    MOTTAR_INTRODUKSJONSSTØNAD(Avslagsårsak.SØKER_HAR_ANNEN_LIVSOPPHOLDSYTELSE, false),
    MOTTAR_BARNEPENSJON(Avslagsårsak.SØKER_HAR_ANNEN_LIVSOPPHOLDSYTELSE, false),
    // Ytelsen må navngis i fritekst, og avklaringen kan derfor aldri avslås automatisk.
    MOTTAR_ANNEN_YTELSE(Avslagsårsak.SØKER_HAR_ANNEN_LIVSOPPHOLDSYTELSE, true),
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
