package no.nav.k9.sak.domene.behandling.steg.utland;

import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTOMATISK_MARKERING_AV_UTENLANDSSAK;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_MARKERING_AV_UTLAND_SAKSTYPE;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittArbeidsforhold;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;

@BehandlingStegRef(kode = "VURDER_UTLAND")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderUtlandSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private OppgaveTjeneste oppgaveTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    VurderUtlandSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderUtlandSteg(BehandlingRepository behandlingRepository,
                            OppgaveTjeneste oppgaveTjeneste,
                            InntektArbeidYtelseTjeneste iayTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        // Vurder automatisk merking av opptjening utland
        if (!behandling.harAksjonspunktMedType(MANUELL_MARKERING_AV_UTLAND_SAKSTYPE) && harOppgittUtenlandskInntekt(kontekst.getBehandlingId())) {
            opprettOppgaveForInnhentingAvDokumentasjon(behandling);
            return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(AksjonspunktResultat.opprettForAksjonspunkt(AUTOMATISK_MARKERING_AV_UTENLANDSSAK)));
        } else {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
    }

    private boolean harOppgittUtenlandskInntekt(Long behandlingId) {
        Optional<OppgittOpptjening> oppgittOpptening = iayTjeneste.finnGrunnlag(behandlingId)
            .flatMap(InntektArbeidYtelseGrunnlag::getOppgittOpptjening);
        return oppgittOpptening.map(oppgittOpptjening -> oppgittOpptjening.getOppgittArbeidsforhold().stream().anyMatch(OppgittArbeidsforhold::erUtenlandskInntekt)).orElse(false);
    }

    private void opprettOppgaveForInnhentingAvDokumentasjon(Behandling behandling) {
        OppgaveÅrsak oppgaveÅrsak = OppgaveÅrsak.BEHANDLE_SAK;
        AksjonspunktDefinisjon aksjonspunktDef = AUTOMATISK_MARKERING_AV_UTENLANDSSAK;
        oppgaveTjeneste.opprettMedPrioritetOgBeskrivelseBasertPåFagsakId(behandling.getFagsakId(), oppgaveÅrsak,
            behandling.getBehandlendeEnhet(), aksjonspunktDef.getNavn(), false);
    }

}
