package no.nav.k9.sak.behandling.revurdering.ytelse;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;

import no.nav.k9.sak.behandling.revurdering.GrunnlagKopierer;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;

@RequestScoped
@Alternative
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
public class GrunnlagKopiererPleiepengerMock implements GrunnlagKopierer {


    public GrunnlagKopiererPleiepengerMock() {
        // for CDI proxy
    }


    @Override
    public void kopierGrunnlagVedManuellOpprettelse(Behandling original, Behandling ny) {
        // kun for test
    }

    @Override
    public void kopierGrunnlagVedAutomatiskOpprettelse(Behandling original, Behandling ny) {
        // kun for test
    }

}
