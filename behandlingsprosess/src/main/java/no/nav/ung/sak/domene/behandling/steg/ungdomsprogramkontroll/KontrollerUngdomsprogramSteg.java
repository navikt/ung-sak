package no.nav.ung.sak.domene.behandling.steg.ungdomsprogramkontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;

import java.util.List;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.KONTROLLER_UNGDOMSPROGRAM;

@BehandlingStegRef(value = KONTROLLER_UNGDOMSPROGRAM)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class KontrollerUngdomsprogramSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private ProgramperiodeendringEtterlysningTjeneste etterlysningTjeneste;

    @Inject
    public KontrollerUngdomsprogramSteg(BehandlingRepository behandlingRepository,
                                        ProgramperiodeendringEtterlysningTjeneste etterlysningTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.etterlysningTjeneste = etterlysningTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        if (behandling.getBehandlingÅrsaker().isEmpty()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        List<BehandlingÅrsakType> behandlingÅrsakerTyper = behandling.getBehandlingÅrsakerTyper();

        boolean skalOppretteEtterlysning = behandlingÅrsakerTyper.stream()
            .anyMatch(årsak ->
                BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM == årsak ||
                    BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM == årsak
            );

        if (skalOppretteEtterlysning) {
            etterlysningTjeneste.opprettEtterlysningerForProgramperiodeEndring(kontekst.getBehandlingId(), kontekst.getFagsakId());
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }


}
