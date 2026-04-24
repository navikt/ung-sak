package no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.behandling.steg.kompletthet.EtterlysningOppretter;
import no.nav.ung.sak.domene.behandling.steg.kompletthet.registerinntektkontroll.KontrollerInntektEtterlysningTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll.ProgramperiodeendringEtterlysningTjeneste;

@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@ApplicationScoped
public class UngEtterlysningOppretter implements EtterlysningOppretter {

    private KontrollerInntektEtterlysningTjeneste kontrollerInntektEtterlysningTjeneste;
    private ProgramperiodeendringEtterlysningTjeneste programperiodeendringEtterlysningTjeneste;
    private BehandlingRepository behandlingRepository;

    public UngEtterlysningOppretter() {
    }

    @Inject
    public UngEtterlysningOppretter(KontrollerInntektEtterlysningTjeneste kontrollerInntektEtterlysningTjeneste,
                                    ProgramperiodeendringEtterlysningTjeneste programperiodeendringEtterlysningTjeneste,
                                    BehandlingRepository behandlingRepository) {
        this.kontrollerInntektEtterlysningTjeneste = kontrollerInntektEtterlysningTjeneste;
        this.programperiodeendringEtterlysningTjeneste = programperiodeendringEtterlysningTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public void opprettEtterlysninger(BehandlingReferanse behandlingReferanse) {
        kontrollerInntektEtterlysningTjeneste.opprettEtterlysninger(behandlingReferanse);
        if (!erKvoteutvidelse(behandlingReferanse)) {
            programperiodeendringEtterlysningTjeneste.opprettEtterlysningerForProgramperiodeEndring(behandlingReferanse);
        }
    }

    private boolean erKvoteutvidelse(BehandlingReferanse behandlingReferanse) {
        var behandling = behandlingRepository.hentBehandling(behandlingReferanse.getBehandlingId());
        return behandling.getBehandlingÅrsakerTyper().contains(BehandlingÅrsakType.RE_HENDELSE_UTVIDET_KVOTE_UNGDOMSPROGRAM);
    }
}
