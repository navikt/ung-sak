package no.nav.ung.sak.ytelse.ung.revurdering;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandling.revurdering.GrunnlagKopierer;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.ytelse.ung.periode.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.ytelse.ung.søknadsperioder.UngdomsytelseSøknadsperiodeRepository;

@ApplicationScoped
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
public class GrunnlagKopiererUngdomsytelse implements GrunnlagKopierer {

    private PersonopplysningRepository personopplysningRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private UngdomsytelseSøknadsperiodeRepository ungdomsytelseSøknadsperiodeRepository;

    public GrunnlagKopiererUngdomsytelse() {
        // for CDI proxy
    }

    @Inject
    public GrunnlagKopiererUngdomsytelse(BehandlingRepositoryProvider repositoryProvider,
                                         InntektArbeidYtelseTjeneste iayTjeneste,
                                         UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository, UngdomsytelseSøknadsperiodeRepository ungdomsytelseSøknadsperiodeRepository) {
        this.iayTjeneste = iayTjeneste;
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.ungdomsytelseSøknadsperiodeRepository = ungdomsytelseSøknadsperiodeRepository;
    }


    @Override
    public void kopierGrunnlagVedManuellOpprettelse(Behandling original, Behandling ny) {
        Long originalBehandlingId = original.getId();
        Long nyBehandlingId = ny.getId();
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        ungdomsprogramPeriodeRepository.kopier(originalBehandlingId, nyBehandlingId);
        ungdomsytelseSøknadsperiodeRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);

        // gjør til slutt, innebærer kall til abakus
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
    }

    @Override
    public void kopierGrunnlagVedAutomatiskOpprettelse(Behandling original, Behandling ny) {
        kopierGrunnlagVedManuellOpprettelse(original, ny);
    }

}
