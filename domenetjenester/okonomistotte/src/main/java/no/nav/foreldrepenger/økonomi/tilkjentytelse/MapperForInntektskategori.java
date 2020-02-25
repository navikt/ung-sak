package no.nav.foreldrepenger.økonomi.tilkjentytelse;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;

class MapperForInntektskategori {

    private static final Map<Inntektskategori, no.nav.k9.oppdrag.kontrakt.kodeverk.Inntektskategori> INNTEKTSKATEGORI_MAP = genererMapping();

    private MapperForInntektskategori() {
        //for å unngå instansiering, slik at SonarQube blir glad
    }

    static no.nav.k9.oppdrag.kontrakt.kodeverk.Inntektskategori mapInntektskategori(Inntektskategori inntektskategori) {
        no.nav.k9.oppdrag.kontrakt.kodeverk.Inntektskategori resultat = INNTEKTSKATEGORI_MAP.get(inntektskategori);
        if (resultat != null) {
            return resultat;
        }
        throw new IllegalArgumentException("Utvikler-feil: Inntektskategorien " + inntektskategori + " er ikke støttet i mapping");
    }

    private static Map<Inntektskategori, no.nav.k9.oppdrag.kontrakt.kodeverk.Inntektskategori> genererMapping() {
        Map<Inntektskategori, no.nav.k9.oppdrag.kontrakt.kodeverk.Inntektskategori> map = Arrays.stream(no.nav.k9.oppdrag.kontrakt.kodeverk.Inntektskategori.values())
            .collect(Collectors.toMap(yt -> Inntektskategori.fraKode(yt.getKode()), Function.identity()));

        for (Inntektskategori egenKode : Inntektskategori.values()) {
            if (egenKode != Inntektskategori.UDEFINERT && !map.containsKey(egenKode)) {
                throw new IllegalStateException("Kan ikke opprette mapping fra " + Inntektskategori.class.getName() + "." + egenKode.name() + " til " + no.nav.k9.oppdrag.kontrakt.kodeverk.Inntektskategori.class.getName() + " siden det ikke finnes tilsvarende i " + no.nav.k9.oppdrag.kontrakt.kodeverk.Inntektskategori.class.getName());
            }
        }
        return map;
    }

}
