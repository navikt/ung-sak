package no.nav.k9.sak.behandling.revurdering;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
public class GrunnlagKopiererOmsorgspenger implements GrunnlagKopierer {

    private PersonopplysningRepository personopplysningRepository;
    private MedlemskapRepository medlemskapRepository;
    private UttakRepository uttakRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    public GrunnlagKopiererOmsorgspenger() {
        // for CDI proxy
    }

    @Inject
    public GrunnlagKopiererOmsorgspenger(BehandlingRepositoryProvider repositoryProvider,
                                         UttakRepository uttakRepository, InntektArbeidYtelseTjeneste iayTjeneste) {
        this.uttakRepository = uttakRepository;
        this.iayTjeneste = iayTjeneste;
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
    }


    @Override
    public void kopierAlleGrunnlagFraTidligereBehandling(Behandling original, Behandling ny) {
        Long originalBehandlingId = original.getId();
        Long nyBehandlingId = ny.getId();
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        medlemskapRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);

        uttakRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);

        // gjør til slutt, innebærer kall til abakus
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
    }

    @Override
    public void opprettAksjonspunktForSaksbehandlerOverstyring(Behandling revurdering) {
        // Ingen overstyring for saksbehandler er implementert
    }
}
