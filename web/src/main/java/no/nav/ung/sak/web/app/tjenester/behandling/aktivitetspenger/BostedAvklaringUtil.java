package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import no.nav.ung.kodeverk.bosatt.Kilde;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.behandlingslager.bosatt.BostedAvklaringData;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.BostedVurderingIkkeOppfyltDto;

import java.time.LocalDate;

/**
 * Hjelpemetoder for konvertering av {@link BostedVurderingIkkeOppfyltDto} til {@link BostedAvklaringData}.
 */
class BostedAvklaringUtil {

    private BostedAvklaringUtil() {
    }

    static BostedAvklaringData tilAvklaringData(LocalDate fom, BostedVurderingIkkeOppfyltDto vurdering) {
        if (vurdering == null) {
            return new BostedAvklaringData(true, null, null, Kilde.SAKSBEHANDLER);
        }
        BostedsvilkårIkkeOppfyltÅrsak årsak = vurdering.fraflyttingsÅrsak();
        return new BostedAvklaringData(false, fom, årsak, Kilde.SAKSBEHANDLER);
    }
}
