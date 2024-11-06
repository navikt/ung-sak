package no.nav.k9.sak.ytelse.ung.revurdering;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandling.revurdering.GrunnlagKopierer;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.ytelse.ung.periode.UngdomsprogramPeriodeRepository;
import no.nav.k9.sak.ytelse.ung.søknadsperioder.UngdomsytelseSøknadsperiodeRepository;

@ApplicationScoped
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
public class GrunnlagKopiererUngdomsytelse implements GrunnlagKopierer {

    private PersonopplysningRepository personopplysningRepository;
    private MedlemskapRepository medlemskapRepository;
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
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.ungdomsytelseSøknadsperiodeRepository = ungdomsytelseSøknadsperiodeRepository;
    }


    @Override
    public void kopierGrunnlagVedManuellOpprettelse(Behandling original, Behandling ny) {
        Long originalBehandlingId = original.getId();
        Long nyBehandlingId = ny.getId();
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        medlemskapRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
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
