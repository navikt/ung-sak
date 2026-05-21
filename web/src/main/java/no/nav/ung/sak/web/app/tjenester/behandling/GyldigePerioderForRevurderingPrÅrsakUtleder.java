package no.nav.ung.sak.web.app.tjenester.behandling;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.behandling.ÅrsakOgPerioderDto;

import java.util.Set;

public interface GyldigePerioderForRevurderingPrÅrsakUtleder {


    ÅrsakOgPerioderDto utledPerioder(long fagsakId);

    /**
     * Returnerer settet med behandlingsårsaker denne utlederen håndterer.
     */
    Set<BehandlingÅrsakType> støttedeÅrsaker();

    /**
     * Sjekker om en gitt periode er gyldig for revurdering.
     * Skal kun kalles for årsaker som er i {@link #støttedeÅrsaker()}.
     */
    boolean periodeErGyldigForÅrsak(long fagsakId, DatoIntervallEntitet periode);

}
