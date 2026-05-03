package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import no.nav.ung.kodeverk.bosatt.FraflyttingsÅrsak;
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
     *   <li>borITrondheimIHelePerioden = true → (erBosattITrondheim=true, fraflyttingsDato=null, årsak=null)</li>
     *   <li>borITrondheimIHelePerioden = false og fraflyttingsDato etter fom → (true, fraflyttingsDato, årsak)</li>
     *   <li>borITrondheimIHelePerioden = false og fraflyttingsDato null eller ≤ fom → (false, null, årsak)</li>
     * </ul>
     */
    static BostedAvklaringData tilAvklaringData(LocalDate fom, BostedVurderingDto vurdering) {
        FraflyttingsÅrsak årsak = vurdering.fraflyttingsÅrsak();
        if (Boolean.TRUE.equals(vurdering.borITrondheimIHelePerioden())) {
            return new BostedAvklaringData(true, null, null);
        }
        LocalDate fraflyttingsDato = vurdering.fraflyttingsDato();
        if (fraflyttingsDato != null && fraflyttingsDato.isAfter(fom)) {
            return new BostedAvklaringData(true, fraflyttingsDato, årsak);
        }
        return new BostedAvklaringData(false, null, årsak);
    }
}

