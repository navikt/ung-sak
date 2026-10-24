package no.nav.ung.sak.produksjonsstyring.behandlingenhet;

import no.nav.ung.kodeverk.behandling.BehandlingDel;

import java.util.Set;

/**
 * Hvilke deler av behandlingen som har egen behandlende enhet, og hvilket behandlingstema som brukes mot NORG2.
 * Implementeres per ytelse med {@code @FagsakYtelseTypeRef}.
 */
public interface BehandlendeEnhetOppsett {

    Set<BehandlingDel> deler();

    /** null gir oppslag uten behandlingstema. */
    String behandlingstema(BehandlingDel del);
}
