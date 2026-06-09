package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import no.nav.ung.kodeverk.bosatt.FraflyttingsÅrsak;
import no.nav.ung.kodeverk.bosatt.Kilde;
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
     * Konverterer én {@link BostedVurderingDto} til {@link BostedAvklaringData} med kilde=SAKSBEHANDLER.
     * <ul>
     * <li> Hvis borITrondheimIHelePerioden=true, settes fraflyttingsÅrsak og fom til null.</li>
     * <li> Hvis borITrondheimIHelePerioden=false, settes fraflyttingsÅrsak og fom til verdiene fra vurdering.</li>
     * </ul>
     */
    static BostedAvklaringData tilAvklaringData(LocalDate fom, BostedVurderingDto vurdering) {
        FraflyttingsÅrsak årsak = vurdering.fraflyttingsÅrsak();
        if (Boolean.TRUE.equals(vurdering.borITrondheimIHelePerioden())) {
            return new BostedAvklaringData(true, null, null, Kilde.SAKSBEHANDLER);
        } else {
            return new BostedAvklaringData(false, fom, årsak, Kilde.SAKSBEHANDLER);
        }
    }
}
