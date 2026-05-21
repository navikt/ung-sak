package no.nav.ung.sak.web.app.tjenester.behandling;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.behandling.ÅrsakOgPerioderDto;

import java.util.Optional;

public interface GyldigePerioderForRevurderingPrÅrsakUtleder {


    ÅrsakOgPerioderDto utledPerioder(long fagsakId);

    /**
     * Sjekker om en gitt periode er gyldig for revurdering med angitt årsak.
     * Dersom denne utlederen ikke håndterer den gitte årsaken, returneres true (ingen begrensning).
     * Dersom årsaken håndteres men ingen periode er oppgitt, returneres false.
     */
    default boolean periodeErGyldigForÅrsak(long fagsakId, BehandlingÅrsakType årsak, Optional<DatoIntervallEntitet> periode) {
        var utledtePerioder = utledPerioder(fagsakId);
        if (utledtePerioder.årsak() != årsak) {
            return true;
        }
        if (periode.isEmpty()) {
            return false;
        }
        return utledtePerioder.perioder().stream()
            .map(DatoIntervallEntitet::fra)
            .anyMatch(periode.get()::equals);
    }

}
