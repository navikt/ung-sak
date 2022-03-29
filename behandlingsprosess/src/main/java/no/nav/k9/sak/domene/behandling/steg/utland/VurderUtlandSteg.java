package no.nav.k9.sak.domene.behandling.steg.utland;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VURDER_UTLAND;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTOMATISK_MARKERING_AV_UTENLANDSSAK;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_MARKERING_AV_UTLAND_SAKSTYPE;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
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
import no.nav.k9.sak.domene.iay.modell.OppgittArbeidsforhold;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilterProvider;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;

@BehandlingStegRef(stegtype = VURDER_UTLAND)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderUtlandSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private OppgaveTjeneste oppgaveTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste;
    private OppgittOpptjeningFilterProvider oppgittOpptjeningFilterProvider;

    VurderUtlandSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderUtlandSteg(BehandlingRepository behandlingRepository,
                            OppgaveTjeneste oppgaveTjeneste,
                            InntektArbeidYtelseTjeneste iayTjeneste,
                            @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste,
                            OppgittOpptjeningFilterProvider oppgittOpptjeningFilterProvider) {
        this.iayTjeneste = iayTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.oppgittOpptjeningFilterProvider = oppgittOpptjeningFilterProvider;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var vilkårPerioder = getPerioderTilVurderingTjeneste(behandling).utled(behandling.getId(), VilkårType.MEDLEMSKAPSVILKÅRET);

        // Vurder automatisk merking av opptjening utland
        for (DatoIntervallEntitet vilkårPeriode : vilkårPerioder) {
            if (!behandling.harAksjonspunktMedType(MANUELL_MARKERING_AV_UTLAND_SAKSTYPE) && harOppgittUtenlandskInntekt(kontekst.getBehandlingId(), vilkårPeriode)) {
                opprettOppgaveForInnhentingAvDokumentasjon(behandling);
                return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(AksjonspunktResultat.opprettForAksjonspunkt(AUTOMATISK_MARKERING_AV_UTENLANDSSAK)));
            }
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean harOppgittUtenlandskInntekt(Long behandlingId, DatoIntervallEntitet vilkårPeriode) {
        var iayGrunnlag = iayTjeneste.finnGrunnlag(behandlingId).orElse(null);
        if (iayGrunnlag == null) {
            return false;
        }
        return oppgittOpptjeningFilterProvider.finnOpptjeningFilter(behandlingId).hentOppgittOpptjening(behandlingId, iayGrunnlag, vilkårPeriode)
            .map(oppgittOpptjening -> oppgittOpptjening.getOppgittArbeidsforhold()
                .stream()
                .anyMatch(OppgittArbeidsforhold::erUtenlandskInntekt))
            .orElse(false);
    }

    private void opprettOppgaveForInnhentingAvDokumentasjon(Behandling behandling) {
        OppgaveÅrsak oppgaveÅrsak = OppgaveÅrsak.BEHANDLE_SAK_VL;
        AksjonspunktDefinisjon aksjonspunktDef = AUTOMATISK_MARKERING_AV_UTENLANDSSAK;
        oppgaveTjeneste.opprettMedPrioritetOgBeskrivelseBasertPåFagsakId(behandling.getFagsakId(), oppgaveÅrsak,
            behandling.getBehandlendeEnhet(), aksjonspunktDef.getNavn(), false);
    }

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(Behandling behandling) {
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, perioderTilVurderingTjeneste, behandling.getFagsakYtelseType(), behandling.getType())
            .orElseThrow(() -> new UnsupportedOperationException("VilkårsPerioderTilVurderingTjeneste ikke implementert for ytelse [" + behandling.getFagsakYtelseType() + "], behandlingtype [" + behandling.getType() + "]"));
    }

}
