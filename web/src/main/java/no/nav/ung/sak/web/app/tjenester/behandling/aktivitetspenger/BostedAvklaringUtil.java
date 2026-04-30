package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import no.nav.ung.sak.behandlingslager.bosatt.BostedAvklaringData;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.BostedVurderingDto;

import java.time.LocalDate;

/**
 * Hjelpemetoder for konvertering av {@link BostedVurderingDto} til {@link BostedAvklaringData}.
 */
class BostedAvklaringUtil {

    private BostedAvklaringUtil() {
    }

    /**
     * Konverterer én {@link BostedVurderingDto} til {@link BostedAvklaringData}.
     * <ul>
     *   <li>borITrondheimIHelePerioden = true → (erBosattITrondheim=true, fraflyttingsDato=null)</li>
     *   <li>borITrondheimIHelePerioden = false og fraflyttingsDato etter fom → (true, fraflyttingsDato)</li>
     *   <li>borITrondheimIHelePerioden = false og fraflyttingsDato null eller ≤ fom → (false, null)</li>
     * </ul>
     */
    static BostedAvklaringData tilAvklaringData(LocalDate fom, BostedVurderingDto vurdering) {
        if (Boolean.TRUE.equals(vurdering.borITrondheimIHelePerioden())) {
            return new BostedAvklaringData(true, null);
        }
        LocalDate fraflyttingsDato = vurdering.fraflyttingsDato();
        if (fraflyttingsDato != null && fraflyttingsDato.isAfter(fom)) {
            return new BostedAvklaringData(true, fraflyttingsDato);
        }
        return new BostedAvklaringData(false, null);
    }
}

