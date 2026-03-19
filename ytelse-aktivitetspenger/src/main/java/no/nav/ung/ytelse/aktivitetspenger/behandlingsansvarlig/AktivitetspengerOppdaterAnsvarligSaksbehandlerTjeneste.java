package no.nav.ung.ytelse.aktivitetspenger.behandlingsansvarlig;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.kodeverk.behandling.BehandlingDel;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingAnsvarligRepository;
import no.nav.ung.sak.domene.vedtak.OppdaterAnsvarligSaksbehandlerTjeneste;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.ung.sak.kontrakt.vedtak.FatterVedtakAksjonspunktDto;
import no.nav.ung.sak.kontrakt.vedtak.LokalkontorBeslutterVilkårAksjonspunktDto;

import java.util.Collection;

@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
@ApplicationScoped
public class AktivitetspengerOppdaterAnsvarligSaksbehandlerTjeneste implements OppdaterAnsvarligSaksbehandlerTjeneste {

    private BehandlingAnsvarligRepository behandlingAnsvarligRepository;

    AktivitetspengerOppdaterAnsvarligSaksbehandlerTjeneste() {
        // for CDI proxy
    }

    @Inject
    public AktivitetspengerOppdaterAnsvarligSaksbehandlerTjeneste(BehandlingAnsvarligRepository behandlingAnsvarligRepository) {
        this.behandlingAnsvarligRepository = behandlingAnsvarligRepository;
    }

    @Override
    public void oppdaterAnsvarligSaksbehandler(Collection<BekreftetAksjonspunktDto> bekreftedeAksjonspunktDtoer, Long behandlingId) {
        if (bekreftedeAksjonspunktDtoer.stream().anyMatch(dto -> dto instanceof FatterVedtakAksjonspunktDto || dto instanceof LokalkontorBeslutterVilkårAksjonspunktDto)) {
            return;
        }
        boolean harDel1Aksjonspunkt = bekreftedeAksjonspunktDtoer.stream().anyMatch(dto -> AksjonspunktDefinisjon.fraKode(dto.getKode()).getAksjonspunktType().erLokalkontorAksjonspunkt());
        boolean harAnnetAksjonspunkt = bekreftedeAksjonspunktDtoer.stream().anyMatch(dto -> !AksjonspunktDefinisjon.fraKode(dto.getKode()).getAksjonspunktType().erLokalkontorAksjonspunkt());
        if (harDel1Aksjonspunkt && harAnnetAksjonspunkt) {
            throw new IllegalArgumentException("Ikke støttet å løse både DEL1-aksjonspunkt og andre aksjonspunkt i samme kall");
        }
        String saksbehandlerIdent = SubjectHandler.getSubjectHandler().getUid();
        BehandlingDel behandlingDel = harDel1Aksjonspunkt ? BehandlingDel.LOKAL : BehandlingDel.SENTRAL;
        behandlingAnsvarligRepository.setAnsvarligSaksbehandler(behandlingId, behandlingDel, saksbehandlerIdent);
    }

    @Override
    public void oppdaterAnsvarligBeslutter(AksjonspunktDefinisjon fatteVedtakAksjonspunktDefinisjon, Long behandlingId) {
        boolean gjelderDel1 = fatteVedtakAksjonspunktDefinisjon.getAksjonspunktType().erLokalkontorAksjonspunkt();
        BehandlingDel behandlingDel = gjelderDel1 ? BehandlingDel.LOKAL : BehandlingDel.SENTRAL;
        String saksbehandlerIdent = SubjectHandler.getSubjectHandler().getUid();
        behandlingAnsvarligRepository.setAnsvarligBeslutter(behandlingId, behandlingDel, saksbehandlerIdent);
    }

}
