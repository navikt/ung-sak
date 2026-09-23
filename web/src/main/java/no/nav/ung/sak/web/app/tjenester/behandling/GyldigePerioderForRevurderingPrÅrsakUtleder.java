package no.nav.ung.sak.web.app.tjenester.behandling;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.behandling.ÅrsakOgPerioderDto;

import java.util.List;
import java.util.Optional;

public interface GyldigePerioderForRevurderingPrÅrsakUtleder {

    List<ÅrsakOgPerioderDto> utledPerioder(long fagsakId);

    boolean støtterÅrsak(BehandlingÅrsakType årsak);

    /**
     * Sjekker om en gitt periode er gyldig for revurdering med den gitte årsaken.
     * Skal kun kalles for årsaker der {@link #støtterÅrsak(BehandlingÅrsakType)} er {@code true}.
     */
    boolean periodeErGyldigForÅrsak(long fagsakId, Optional<DatoIntervallEntitet> periode, BehandlingÅrsakType årsak);

}
