package no.nav.ung.sak.domene.registerinnhenting.impl.startpunkt;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPerioder;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.ung.sak.domene.registerinnhenting.EndringStartpunktUtleder;
import no.nav.ung.sak.domene.registerinnhenting.GrunnlagRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Endringer i kontrollperioder kommer bare fra synk av åpne behandlinger (utvidet snapshot). Diffen er allerede avgrenset til
 * feltene som betyr noe, så enhver sporet endring gir ny vurdering av kompletthet og kontroll av inntekt.
 */
@ApplicationScoped
@GrunnlagRef(KontrollertInntektPerioder.class)
@FagsakYtelseTypeRef
class StartpunktUtlederKontrollertInntekt implements EndringStartpunktUtleder {

    private static final Logger log = LoggerFactory.getLogger(StartpunktUtlederKontrollertInntekt.class);

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        if (Objects.equals(grunnlagId1, grunnlagId2)) {
            return StartpunktType.UDEFINERT;
        }
        log.info("Fant endringer i kontrollperioder for behandling {}. Flytter til vurder kompletthet.", ref.getBehandlingId());
        return StartpunktType.VURDER_KOMPLETTHET;
    }
}
