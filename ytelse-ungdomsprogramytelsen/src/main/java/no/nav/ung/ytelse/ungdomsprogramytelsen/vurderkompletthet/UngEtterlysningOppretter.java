package no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.behandling.steg.kompletthet.EtterlysningOppretter;
import no.nav.ung.sak.domene.behandling.steg.kompletthet.registerinntektkontroll.KontrollerInntektEtterlysningTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll.AutomatiskOpphørEtterlysningTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll.ProgramperiodeendringEtterlysningTjeneste;

@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@ApplicationScoped
public class UngEtterlysningOppretter implements EtterlysningOppretter {

    private KontrollerInntektEtterlysningTjeneste kontrollerInntektEtterlysningTjeneste;
    private ProgramperiodeendringEtterlysningTjeneste programperiodeendringEtterlysningTjeneste;
    private AutomatiskOpphørEtterlysningTjeneste automatiskOpphørEtterlysningTjeneste;
    private BehandlingRepository behandlingRepository;

    public UngEtterlysningOppretter() {
    }

    @Inject
    public UngEtterlysningOppretter(KontrollerInntektEtterlysningTjeneste kontrollerInntektEtterlysningTjeneste,
                                    ProgramperiodeendringEtterlysningTjeneste programperiodeendringEtterlysningTjeneste,
                                    AutomatiskOpphørEtterlysningTjeneste automatiskOpphørEtterlysningTjeneste,
                                    BehandlingRepository behandlingRepository) {
        this.kontrollerInntektEtterlysningTjeneste = kontrollerInntektEtterlysningTjeneste;
        this.programperiodeendringEtterlysningTjeneste = programperiodeendringEtterlysningTjeneste;
        this.automatiskOpphørEtterlysningTjeneste = automatiskOpphørEtterlysningTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public void opprettEtterlysninger(BehandlingReferanse behandlingReferanse) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingReferanse.getBehandlingId());
        var årsaker = behandling.getBehandlingÅrsakerTyper();

        boolean harVarselAutomatiskOpphør = årsaker.contains(BehandlingÅrsakType.RE_VARSEL_AUTOMATISK_OPPHOR);
        boolean harUtvidetKvote = årsaker.contains(BehandlingÅrsakType.RE_HENDELSE_UTVIDET_KVOTE_UNGDOMSPROGRAM);
        boolean harOpphør = årsaker.contains(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);

        // Scenario 2 & 3: Utvidet kvote eller manuelt opphør avbryter varsel om automatisk opphør
        if (harVarselAutomatiskOpphør && (harUtvidetKvote || harOpphør)) {
            automatiskOpphørEtterlysningTjeneste.avbrytEtterlysningForAutomatiskOpphør(behandlingReferanse);
            // Fortsett med normal etterlysningsflyt for den nye årsaken
            kontrollerInntektEtterlysningTjeneste.opprettEtterlysninger(behandlingReferanse);
            if (!harUtvidetKvote) {
                programperiodeendringEtterlysningTjeneste.opprettEtterlysningerForProgramperiodeEndring(behandlingReferanse);
            }
        } else if (harVarselAutomatiskOpphør) {
            // Scenario 1: Kun varsel om automatisk opphør — opprett etterlysning
            automatiskOpphørEtterlysningTjeneste.opprettEtterlysningForAutomatiskOpphør(behandlingReferanse);
        } else {
            // Normal flyt
            kontrollerInntektEtterlysningTjeneste.opprettEtterlysninger(behandlingReferanse);
            if (!harUtvidetKvote) {
                programperiodeendringEtterlysningTjeneste.opprettEtterlysningerForProgramperiodeEndring(behandlingReferanse);
            }
        }
    }
}
