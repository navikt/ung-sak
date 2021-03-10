package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.mottak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.revurdering.GrunnlagKopierer;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP_KS")
@FagsakYtelseTypeRef("OMP_MA")
public class GrunnlagKopiererUtvidetRett implements GrunnlagKopierer {

    private PersonopplysningRepository personopplysningRepository;

    public GrunnlagKopiererUtvidetRett() {
        // for CDI proxy
    }

    @Inject
    public GrunnlagKopiererUtvidetRett(BehandlingRepositoryProvider repositoryProvider) {
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
    }

    @Override
    public void kopierGrunnlagVedManuellOpprettelse(Behandling original, Behandling ny) {
        Long originalBehandlingId = original.getId();
        Long nyBehandlingId = ny.getId();
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
    }

    @Override
    public void kopierGrunnlagVedAutomatiskOpprettelse(Behandling original, Behandling ny) {
        // Samme kopiering som manuell opprettelse
        kopierGrunnlagVedManuellOpprettelse(original, ny);
    }

}
