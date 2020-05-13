package no.nav.k9.sak.behandling.revurdering;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;

public interface GrunnlagKopierer {
    void kopierAlleGrunnlagFraTidligereBehandling(Behandling original, Behandling ny);

    void opprettAksjonspunktForSaksbehandlerOverstyring(Behandling revurdering);
}
