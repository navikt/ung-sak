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
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.NavKontorBeslutterVilkårAksjonspunktDto;

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
        if (bekreftedeAksjonspunktDtoer.stream().anyMatch(dto -> dto instanceof FatterVedtakAksjonspunktDto || dto instanceof NavKontorBeslutterVilkårAksjonspunktDto)) {
            return;
        }
        boolean harNavKontorAksjonspunkt = bekreftedeAksjonspunktDtoer.stream().anyMatch(dto -> AksjonspunktDefinisjon.fraKode(dto.getKode()).getAksjonspunktType().erNavKontorAksjonspunkt());
        boolean harAnnetAksjonspunkt = bekreftedeAksjonspunktDtoer.stream().anyMatch(dto -> !AksjonspunktDefinisjon.fraKode(dto.getKode()).getAksjonspunktType().erNavKontorAksjonspunkt());
        if (harNavKontorAksjonspunkt && harAnnetAksjonspunkt) {
            throw new IllegalArgumentException("Ikke støttet å løse både NavKontor-aksjonspunkt og andre aksjonspunkt i samme kall");
        }
        String saksbehandlerIdent = SubjectHandler.getSubjectHandler().getUid();
        BehandlingDel behandlingDel = harNavKontorAksjonspunkt ? BehandlingDel.NAV_KONTOR : BehandlingDel.SENTRAL;
        behandlingAnsvarligRepository.setAnsvarligSaksbehandler(behandlingId, behandlingDel, saksbehandlerIdent);
    }

    @Override
    public void oppdaterAnsvarligBeslutter(AksjonspunktDefinisjon fatteVedtakAksjonspunktDefinisjon, Long behandlingId) {
        boolean gjelderNavKontor = fatteVedtakAksjonspunktDefinisjon.getAksjonspunktType().erNavKontorAksjonspunkt();
        BehandlingDel behandlingDel = gjelderNavKontor ? BehandlingDel.NAV_KONTOR : BehandlingDel.SENTRAL;
        String saksbehandlerIdent = SubjectHandler.getSubjectHandler().getUid();
        behandlingAnsvarligRepository.setAnsvarligBeslutter(behandlingId, behandlingDel, saksbehandlerIdent);
    }

}
