package no.nav.ung.kodeverk.vilkår;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

public enum AktivitetsvilkåretIkkeOppfyltÅrsak implements IkkeOppfyltDetaljertÅrsak {

    FRAVÆR_FRA_PLANLAGT_AKTIVITET(Avslagsårsak.AKTIVITETSVILKÅR_GENERELL_AVSLAGSÅRSAK, false),
    AVSTÅR_FRA_TILBUD_OM_ARBEID_UTEN_RIMELIG_GRUNN(Avslagsårsak.AKTIVITETSVILKÅR_GENERELL_AVSLAGSÅRSAK, false),
    ANNET(Avslagsårsak.AKTIVITETSVILKÅR_GENERELL_AVSLAGSÅRSAK, true),
    // Saksbehandler har valgt å innvilge periode som er kortere enn perioden saksbehandlingssystemet tillater å innvilge.
    AVKORTET(Avslagsårsak.AVKORTET, false),
    UDEFINERT(null, false),
    ;

    private final Avslagsårsak avslagsårsak;
    private final boolean kreverFritekst;

    AktivitetsvilkåretIkkeOppfyltÅrsak(Avslagsårsak avslagsårsak, boolean kreverFritekst) {
        this.avslagsårsak = avslagsårsak;
        this.kreverFritekst = kreverFritekst;
    }

    public static AktivitetsvilkåretIkkeOppfyltÅrsak fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        try {
            return valueOf(kode);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Ukjent AktivitetsvilkåretIkkeOppfyltÅrsak: " + kode, e);
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
