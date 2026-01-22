package no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.domene.behandling.steg.kompletthet.EtterlysningOppretter;
import no.nav.ung.sak.domene.behandling.steg.kompletthet.registerinntektkontroll.KontrollerInntektEtterlysningTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll.ProgramperiodeendringEtterlysningTjeneste;

@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@ApplicationScoped
public class UngEtterlysningOppretter implements EtterlysningOppretter {

    private KontrollerInntektEtterlysningTjeneste kontrollerInntektEtterlysningTjeneste;
    private ProgramperiodeendringEtterlysningTjeneste programperiodeendringEtterlysningTjeneste;

    public UngEtterlysningOppretter() {
    }

    @Inject
    public UngEtterlysningOppretter(KontrollerInntektEtterlysningTjeneste kontrollerInntektEtterlysningTjeneste, ProgramperiodeendringEtterlysningTjeneste programperiodeendringEtterlysningTjeneste) {
        this.kontrollerInntektEtterlysningTjeneste = kontrollerInntektEtterlysningTjeneste;
        this.programperiodeendringEtterlysningTjeneste = programperiodeendringEtterlysningTjeneste;
    }

    @Override
    public void opprettEtterlysninger(BehandlingReferanse behandlingReferanse) {
        kontrollerInntektEtterlysningTjeneste.opprettEtterlysninger(behandlingReferanse);
        programperiodeendringEtterlysningTjeneste.opprettEtterlysningerForProgramperiodeEndring(behandlingReferanse);
    }
}
