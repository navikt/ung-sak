package no.nav.ung.sak.behandling.revurdering.ytelse;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;

import no.nav.ung.sak.behandling.revurdering.GrunnlagKopierer;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

@RequestScoped
@Alternative
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
public class GrunnlagKopiererUngdomsytelseMock implements GrunnlagKopierer {


    public GrunnlagKopiererUngdomsytelseMock() {
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
