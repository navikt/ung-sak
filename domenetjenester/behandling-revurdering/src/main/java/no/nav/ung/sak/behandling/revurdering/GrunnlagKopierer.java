package no.nav.ung.sak.behandling.revurdering;

import no.nav.ung.sak.behandlingslager.behandling.Behandling;

public interface GrunnlagKopierer {
    void kopierGrunnlagVedManuellOpprettelse(Behandling original, Behandling ny);

    void kopierGrunnlagVedAutomatiskOpprettelse(Behandling original, Behandling ny);

}
