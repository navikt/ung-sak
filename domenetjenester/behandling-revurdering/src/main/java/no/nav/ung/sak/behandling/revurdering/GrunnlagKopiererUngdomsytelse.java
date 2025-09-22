package no.nav.ung.sak.behandling.revurdering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseRepository;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

@ApplicationScoped
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
public class GrunnlagKopiererUngdomsytelse implements GrunnlagKopierer {

    private PersonopplysningRepository personopplysningRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository;
    private TilkjentYtelseRepository tilkjentYtelseRepository;
    private UttalelseRepository uttalelseRepository;

    public GrunnlagKopiererUngdomsytelse() {
        // for CDI proxy
    }

    @Inject
    public GrunnlagKopiererUngdomsytelse(BehandlingRepositoryProvider repositoryProvider,
                                         InntektArbeidYtelseTjeneste iayTjeneste,
                                         UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository, UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository, TilkjentYtelseRepository tilkjentYtelseRepository, UttalelseRepository uttalelseRepository) {
        this.iayTjeneste = iayTjeneste;
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.ungdomsytelseStartdatoRepository = ungdomsytelseStartdatoRepository;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.uttalelseRepository = uttalelseRepository;
    }


    @Override
    public void kopierGrunnlagVedManuellOpprettelse(Behandling original, Behandling ny) {
        Long originalBehandlingId = original.getId();
        Long nyBehandlingId = ny.getId();
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        ungdomsprogramPeriodeRepository.kopier(originalBehandlingId, nyBehandlingId);
        ungdomsytelseStartdatoRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        tilkjentYtelseRepository.kopierKontrollPerioder(originalBehandlingId, nyBehandlingId);
        uttalelseRepository.kopier(originalBehandlingId, nyBehandlingId);

        // gjør til slutt, innebærer kall til abakus
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
    }

    @Override
    public void kopierGrunnlagVedAutomatiskOpprettelse(Behandling original, Behandling ny) {
        kopierGrunnlagVedManuellOpprettelse(original, ny);
    }

}
