package no.nav.k9.sak.domene.behandling.steg.foreslåvedtak;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;


public interface ForeslåVedtakManueltUtleder {
    boolean skalOppretteForeslåVedtakManuelt(Behandling behandling);
}
