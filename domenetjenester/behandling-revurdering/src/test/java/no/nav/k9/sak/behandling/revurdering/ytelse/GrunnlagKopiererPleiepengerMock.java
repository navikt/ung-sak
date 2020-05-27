package no.nav.k9.sak.behandling.revurdering.ytelse;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;

import no.nav.k9.sak.behandling.revurdering.GrunnlagKopierer;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;

@RequestScoped
@Alternative
@FagsakYtelseTypeRef("PSB")
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
