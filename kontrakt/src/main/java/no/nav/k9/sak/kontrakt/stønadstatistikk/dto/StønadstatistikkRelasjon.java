package no.nav.k9.sak.kontrakt.stønadstatistikk.dto;

import no.nav.k9.sak.kontrakt.omsorg.BarnRelasjon;

public enum StønadstatistikkRelasjon {
    FOLKEREGISTRERT_FORELDER,
    MOR,
    MEDMOR,
    FAR,
    FOSTERFORELDER,
    ANNET;

    public static StønadstatistikkRelasjon fromBarnRelasjon(BarnRelasjon br) {
        switch (br) {
        case MOR: return MOR;
        case MEDMOR: return MEDMOR;
        case FAR: return FAR;
        case FOSTERFORELDER: return FOSTERFORELDER;
        case ANNET: return ANNET;
        default: throw new IllegalArgumentException("Ukjent BarnRelasjon: " + br);
        }
    }
}
