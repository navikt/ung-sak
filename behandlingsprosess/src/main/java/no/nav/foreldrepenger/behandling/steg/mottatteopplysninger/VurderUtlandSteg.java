package no.nav.foreldrepenger.behandling.steg.mottatteopplysninger;

import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTOMATISK_MARKERING_AV_UTENLANDSSAK;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_MARKERING_AV_UTLAND_SAKSTYPE;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittArbeidsforhold;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.inngangsvilkaar.impl.DefaultVilkårUtleder;
import no.nav.foreldrepenger.inngangsvilkaar.impl.UtledeteVilkår;
import no.nav.foreldrepenger.inngangsvilkaar.perioder.PerioderTilVurderingTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;

@BehandlingStegRef(kode = "VURDER_UTLAND")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderUtlandSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private OppgaveTjeneste oppgaveTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private PerioderTilVurderingTjeneste perioderTilVurderingTjeneste;

    VurderUtlandSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderUtlandSteg(BehandlingRepositoryProvider provider, // NOSONAR
                            PerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                            OppgaveTjeneste oppgaveTjeneste,
                            InntektArbeidYtelseTjeneste iayTjeneste) {
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.behandlingRepository = provider.getBehandlingRepository();
        this.vilkårResultatRepository = provider.getVilkårResultatRepository();
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        // Utleder vilkår med en gang
        utledVilkår(kontekst);

        // Vurder automatisk merking av opptjening utland
        List<AksjonspunktResultat> aksjonspunkter = new ArrayList<>();

        if (!behandling.harAksjonspunktMedType(MANUELL_MARKERING_AV_UTLAND_SAKSTYPE) && harOppgittUtenlandskInntekt(kontekst.getBehandlingId())) {
            aksjonspunkter.add(AksjonspunktResultat.opprettForAksjonspunkt(AUTOMATISK_MARKERING_AV_UTENLANDSSAK));
            opprettOppgaveForInnhentingAvDokumentasjon(behandling);
        }

        return aksjonspunkter.isEmpty() ? BehandleStegResultat.utførtUtenAksjonspunkter()
            : BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
    }


    private void utledVilkår(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        UtledeteVilkår utledeteVilkår = new DefaultVilkårUtleder().utledVilkår(behandling);
        opprettVilkår(utledeteVilkår, behandling, kontekst.getSkriveLås());
    }

    private void opprettVilkår(UtledeteVilkår utledeteVilkår, Behandling behandling, BehandlingLås skriveLås) {
        // Opprett Vilkårsresultat med vilkårne som som skal vurderes, og sett dem som ikke vurdert
        opprettBehandlingsresultatHvisIkkeEksisterende(behandling);
        final var eksisterendeVilkår = vilkårResultatRepository.hentHvisEksisterer(behandling.getId());
        VilkårResultatBuilder vilkårBuilder = Vilkårene.builderFraEksisterende(eksisterendeVilkår.orElse(null));
        final var vilkårResultat = vilkårBuilder.leggTilIkkeVurderteVilkår(utledPerioderTilVurdering(behandling.getId()), utledeteVilkår.getAlleAvklarte()).build();
        vilkårResultatRepository.lagre(behandling.getId(), vilkårResultat);
    }

    private void opprettBehandlingsresultatHvisIkkeEksisterende(Behandling behandling) {
        var behandlingsresultat = behandling.getBehandlingsresultat();
        if (behandlingsresultat == null) {
            Behandlingsresultat.builder().buildFor(behandling);
        }
    }

    private List<DatoIntervallEntitet> utledPerioderTilVurdering(Long behandlingId) {
        final var perioder = perioderTilVurderingTjeneste.utled(behandlingId);
        return new ArrayList<>(perioder);
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
