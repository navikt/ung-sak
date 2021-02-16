package no.nav.k9.sak.økonomi.tilkjentytelse;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.oppdrag.kontrakt.kodeverk.YtelseType;

class MapperForYtelseType {

    private static final Map<FagsakYtelseType, YtelseType> YTELSE_TYPE_MAP = genererMapping();

    private static Map<FagsakYtelseType, YtelseType> genererMapping() {
        Map<FagsakYtelseType, YtelseType> map = Arrays.stream(YtelseType.values())
            .collect(Collectors.toMap(yt -> FagsakYtelseType.fraKode(yt.getKode()), Function.identity()));

        Set<FagsakYtelseType> ignored = Set.of(
            FagsakYtelseType.UDEFINERT,
            FagsakYtelseType.OBSOLETE,
            FagsakYtelseType.OMSORGSPENGER_KS, // rammevedtak - medfører ikke utbetaling
            FagsakYtelseType.OMSORGSPENGER_MA // rammevedtak - medfører ikke utbetaling
        );
        for (FagsakYtelseType egenKode : FagsakYtelseType.values()) {
            if (ignored.contains(egenKode)) {
                continue;
            }
            if (!map.containsKey(egenKode)) {
                throw new IllegalStateException("Kan ikke opprette mapping fra " + FagsakYtelseType.class.getName() + "." + egenKode.name() + " til " + YtelseType.class.getName()
                    + " siden det ikke finnes tilsvarende i " + YtelseType.class.getName());
            }
        }
        return map;
    }

    private MapperForYtelseType() {
        // for å unngå instansiering, slik at SonarQube blir glad
    }

    static YtelseType mapYtelseType(FagsakYtelseType fagsakYtelseType) {
        YtelseType resultat = YTELSE_TYPE_MAP.get(fagsakYtelseType);
        if (resultat != null) {
            return resultat;
        }
        throw new IllegalArgumentException("Utvikler-feil: FagsakYtelseType " + fagsakYtelseType + " er ikke støttet i mapping");
    }

    public static void main(String[] args) {
        System.out.println(genererMapping());
    }
}
