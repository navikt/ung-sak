package no.nav.ung.sak.domene.behandling.steg.foreslåvedtak;

import no.nav.ung.sak.behandlingslager.behandling.Behandling;


public interface ForeslåVedtakManueltUtleder {
    boolean skalOppretteForeslåVedtakManuelt(Behandling behandling);
}
