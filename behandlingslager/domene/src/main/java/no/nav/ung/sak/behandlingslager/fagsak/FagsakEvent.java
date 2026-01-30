package no.nav.ung.sak.behandlingslager.fagsak;

import no.nav.ung.sak.typer.AktørId;

/**
 * Marker interface for events fyrt på en Fagsak.
 * Disse fyres ved hjelp av CDI Events.
 */
public interface FagsakEvent {

    Long getFagsakId();

    AktørId getAktørId();

}
