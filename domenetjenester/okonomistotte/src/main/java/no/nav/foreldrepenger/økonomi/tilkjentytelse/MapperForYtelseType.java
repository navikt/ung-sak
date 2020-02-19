package no.nav.foreldrepenger.økonomi.tilkjentytelse;

import java.util.Map;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.oppdrag.kontrakt.kodeverk.YtelseType;

class MapperForYtelseType {

    private static final Map<FagsakYtelseType, YtelseType> YTELSE_TYPE_MAP = Map.of(
        FagsakYtelseType.ENGANGSTØNAD, YtelseType.ENGANGSTØNAD,
        FagsakYtelseType.FORELDREPENGER, YtelseType.FORELDREPENGER,
        FagsakYtelseType.SVANGERSKAPSPENGER, YtelseType.SVANGERSKAPSPENGER
    );

    private MapperForYtelseType() {
        //for å unngå instansiering, slik at SonarQube blir glad
    }

    static YtelseType mapYtelseType(FagsakYtelseType fagsakYtelseType) {
        YtelseType resultat = YTELSE_TYPE_MAP.get(fagsakYtelseType);
        if (resultat != null) {
            return resultat;
        }
        throw new IllegalArgumentException("Utvikler-feil: FagsakYtelseType " + fagsakYtelseType + " er ikke støttet i mapping");
    }
}
