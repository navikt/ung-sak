package no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.behandling.steg.kompletthet.EtterlysningOppretter;

@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@ApplicationScoped
public class UngEtterlysningOppretter implements EtterlysningOppretter {

    private BehandlingRepository behandlingRepository;
    private UngEtterlysningsOrkestreringTjeneste etterlysningsOrkestreringTjeneste;

    public UngEtterlysningOppretter() {
    }

    @Inject
    public UngEtterlysningOppretter(BehandlingRepository behandlingRepository,
                                    UngEtterlysningsOrkestreringTjeneste etterlysningsOrkestreringTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.etterlysningsOrkestreringTjeneste = etterlysningsOrkestreringTjeneste;
    }

    @Override
    public void opprettEtterlysninger(BehandlingReferanse behandlingReferanse) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingReferanse.getBehandlingId());
        var årsaker = behandling.getBehandlingÅrsakerTyper();

        etterlysningsOrkestreringTjeneste.orkestrerEtterlysninger(behandlingReferanse, årsaker);
    }
}
