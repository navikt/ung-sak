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
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.maksdato.MaksdatoEtterlysningTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll.ProgramperiodeendringEtterlysningTjeneste;

@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@ApplicationScoped
public class UngEtterlysningOppretter implements EtterlysningOppretter {

    private KontrollerInntektEtterlysningTjeneste kontrollerInntektEtterlysningTjeneste;
    private ProgramperiodeendringEtterlysningTjeneste programperiodeendringEtterlysningTjeneste;
    private MaksdatoEtterlysningTjeneste maksdatoEtterlysningTjeneste;
    private BehandlingRepository behandlingRepository;

    public UngEtterlysningOppretter() {
    }

    @Inject
    public UngEtterlysningOppretter(KontrollerInntektEtterlysningTjeneste kontrollerInntektEtterlysningTjeneste, ProgramperiodeendringEtterlysningTjeneste programperiodeendringEtterlysningTjeneste, MaksdatoEtterlysningTjeneste maksdatoEtterlysningTjeneste, BehandlingRepository behandlingRepository) {
        this.kontrollerInntektEtterlysningTjeneste = kontrollerInntektEtterlysningTjeneste;
        this.programperiodeendringEtterlysningTjeneste = programperiodeendringEtterlysningTjeneste;
        this.maksdatoEtterlysningTjeneste = maksdatoEtterlysningTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public void opprettEtterlysninger(BehandlingReferanse behandlingReferanse) {
        // Rene varsel-om-opphør-ved-maksdato-behandlinger skal kun varsle om opphør, og ikke trigge
        // inntektskontroll eller programperiodeendring-varsling som del av opphørsløpet.
        if (erRentVarselOpphørVedMaksdatoLøp(behandlingReferanse)) {
            maksdatoEtterlysningTjeneste.opprettEtterlysningForOpphørVedMaksdatoDersomRelevant(behandlingReferanse);
            return;
        }
        kontrollerInntektEtterlysningTjeneste.opprettEtterlysninger(behandlingReferanse);
        programperiodeendringEtterlysningTjeneste.opprettEtterlysningerForProgramperiodeEndring(behandlingReferanse);
        maksdatoEtterlysningTjeneste.opprettEtterlysningForOpphørVedMaksdatoDersomRelevant(behandlingReferanse);
    }

    private boolean erRentVarselOpphørVedMaksdatoLøp(BehandlingReferanse behandlingReferanse) {
        var årsaker = behandlingRepository.hentBehandling(behandlingReferanse.getBehandlingId()).getBehandlingÅrsakerTyper();
        return !årsaker.isEmpty() && årsaker.stream().allMatch(å -> å == BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO);
    }
}
