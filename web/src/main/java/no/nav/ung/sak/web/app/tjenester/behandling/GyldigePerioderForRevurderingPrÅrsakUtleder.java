package no.nav.ung.sak.web.app.tjenester.behandling;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.behandling.ÅrsakOgPerioderDto;

public interface GyldigePerioderForRevurderingPrÅrsakUtleder {


    ÅrsakOgPerioderDto utledPerioder(long fagsakId);

    /**
     * Returnerer behandlingsårsaken denne utlederen håndterer.
     */
    BehandlingÅrsakType støttetÅrsak();

    /**
     * Sjekker om en gitt periode er gyldig for revurdering.
     * Skal kun kalles for årsaken returnert av {@link #støttetÅrsak()}.
     */
    boolean periodeErGyldigForÅrsak(long fagsakId, DatoIntervallEntitet periode);

}
