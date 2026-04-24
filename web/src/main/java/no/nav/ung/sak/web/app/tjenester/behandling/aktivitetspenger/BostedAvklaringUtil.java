package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.BostedVurderingDto;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hjelpemetoder for splitting av bostedavklaring basert på {@link BostedVurderingDto}.
 */
class BostedAvklaringUtil {

    private BostedAvklaringUtil() {
    }

    /**
     * Splitter én {@link BostedVurderingDto} til en map fomDato → erBosattITrondheim.
     * <ul>
     *   <li>borITrondheimIHelePerioden = true → én avklaring (fom, true)</li>
     *   <li>borITrondheimIHelePerioden = false og fraflyttingsDato etter fom → to avklaringer: (fom, true) + (fraflyttingsDato, false)</li>
     *   <li>borITrondheimIHelePerioden = false og fraflyttingsDato null eller ≤ fom → én avklaring (fom, false)</li>
     * </ul>
     */
    static Map<LocalDate, Boolean> splittAvklaring(LocalDate fom, BostedVurderingDto vurdering) {
        if (Boolean.TRUE.equals(vurdering.getBorITrondheimIHelePerioden())) {
            return Map.of(fom, true);
        }
        LocalDate fraflyttingsDato = vurdering.getFraflyttingsDato();
        if (fraflyttingsDato != null && fraflyttingsDato.isAfter(fom)) {
            var resultat = new LinkedHashMap<LocalDate, Boolean>();
            resultat.put(fom, true);
            resultat.put(fraflyttingsDato, false);
            return resultat;
        }
        return Map.of(fom, false);
    }
}
