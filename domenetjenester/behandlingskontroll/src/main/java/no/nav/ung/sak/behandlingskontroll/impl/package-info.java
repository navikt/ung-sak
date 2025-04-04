/**
 * Implementasjon av tjeneste for Behandlingskontroll.
 * <h1>Beskrivelse</h1>
 * Det er tilstandsmaskinen som prosesserer en behandling riktig framover gjennom et eller flere
 * {@link no.nav.ung.sak.behandlingskontroll.BehandlingSteg} og stopper på angitte
 * {@link no.nav.ung.sak.behandlingslager.behandling.Aksjonspunkt} som oppdages.
 * <p>
 * Hvilke {@link no.nav.ung.sak.behandlingskontroll.BehandlingSteg} som skal prosesseres er avhengig av
 * {@link no.nav.ung.kodeverk.behandling.BehandlingType}.
 * <p>
 * I tillegg til å definere hvordan behandlingen kan prosesseres framover, er det mulig å legge på vent, henlegge,
 * avslutte (avslag), hoppe framover eller bakover i stegene.
 * <p>
 * <h1>Events</h1>
 * Når Behandlingskontroll endrer steg eller stegstatus, eller aksjonspunkter oppdages eller utføres vil ulike typer
 * {@link jakarta.enterprise.event.Event} fyres.
 * Disse kan observeres synkront og implementere logikk for hva som skal skje når prosessen endrer tilstand.
 */
package no.nav.ung.sak.behandlingskontroll.impl;
