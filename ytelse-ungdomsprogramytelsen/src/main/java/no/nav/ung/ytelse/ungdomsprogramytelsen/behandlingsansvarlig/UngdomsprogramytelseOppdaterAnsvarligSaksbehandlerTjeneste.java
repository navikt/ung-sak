package no.nav.ung.ytelse.ungdomsprogramytelsen.behandlingsansvarlig;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.ung.kodeverk.behandling.BehandlingDel;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingAnsvarligRepository;
import no.nav.ung.sak.domene.vedtak.OppdaterAnsvarligSaksbehandlerTjeneste;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.ung.sak.kontrakt.vedtak.FatterVedtakAksjonspunktDto;

import java.util.Collection;

@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@ApplicationScoped
public class UngdomsprogramytelseOppdaterAnsvarligSaksbehandlerTjeneste implements OppdaterAnsvarligSaksbehandlerTjeneste {

    private BehandlingAnsvarligRepository behandlingAnsvarligRepository;

    UngdomsprogramytelseOppdaterAnsvarligSaksbehandlerTjeneste() {
        // for CDI proxy
    }

    @Inject
    public UngdomsprogramytelseOppdaterAnsvarligSaksbehandlerTjeneste(BehandlingAnsvarligRepository behandlingAnsvarligRepository) {
        this.behandlingAnsvarligRepository = behandlingAnsvarligRepository;
    }

    @Override
    public void oppdaterAnsvarligSaksbehandler(Collection<BekreftetAksjonspunktDto> bekreftedeAksjonspunktDtoer, Long behandlingId) {
        if (bekreftedeAksjonspunktDtoer.stream().anyMatch(dto -> dto instanceof FatterVedtakAksjonspunktDto)) {
            return;
        }
        String saksbehandlerIdent = SubjectHandler.getSubjectHandler().getUid();
        behandlingAnsvarligRepository.setAnsvarligSaksbehandler(behandlingId, BehandlingDel.SENTRAL, saksbehandlerIdent);
    }

    @Override
    public void oppdaterAnsvarligBeslutter(AksjonspunktDefinisjon fatteVedtakAksjonspunktDefinisjon, Long behandlingId) {
        String saksbehandlerIdent = SubjectHandler.getSubjectHandler().getUid();
        behandlingAnsvarligRepository.setAnsvarligBeslutter(behandlingId, BehandlingDel.SENTRAL, saksbehandlerIdent);
    }
}
