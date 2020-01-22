package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.felles;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Utfall;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.AksjonspunktutlederForVurderBekreftetOpptjening;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.AksjonspunktutlederForVurderOppgittOpptjening;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.opptjening.OpptjeningsVilkårTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

public abstract class OpptjeningFaktaStegFelles implements BehandlingSteg {

    private BehandlingRepositoryProvider repositoryProvider;
    private AksjonspunktutlederForVurderOppgittOpptjening aksjonspunktutlederOppgitt;
    private AksjonspunktutlederForVurderBekreftetOpptjening aksjonspunktutlederBekreftet;
    private OpptjeningsVilkårTjeneste opptjeningsVilkårTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    protected OpptjeningFaktaStegFelles() {
        // CDI proxy
    }

    protected OpptjeningFaktaStegFelles(BehandlingRepositoryProvider repositoryProvider,
                                        AksjonspunktutlederForVurderBekreftetOpptjening aksjonspunktutlederBekreftet,
                                        AksjonspunktutlederForVurderOppgittOpptjening aksjonspunktutlederOppgitt,
                                        OpptjeningsVilkårTjeneste opptjeningsVilkårTjeneste,
                                        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.repositoryProvider = repositoryProvider;
        this.aksjonspunktutlederBekreftet = aksjonspunktutlederBekreftet;
        this.aksjonspunktutlederOppgitt = aksjonspunktutlederOppgitt;
        this.opptjeningsVilkårTjeneste = opptjeningsVilkårTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = repositoryProvider.getBehandlingRepository().hentBehandling(behandlingId);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId));

        final var perioderTilVurdering = perioderTilVurdering(ref.getBehandlingId());
        var utfall = Utfall.OPPFYLT;
        for (DatoIntervallEntitet periode : perioderTilVurdering) {
            VilkårData resultat = opptjeningsVilkårTjeneste.vurderOpptjeningsVilkår(ref, periode);
            if (!Utfall.OPPFYLT.equals(resultat.getUtfallType())) {
                utfall = resultat.getUtfallType();
                break;
            }
        }
        if (Utfall.OPPFYLT.equals(utfall)) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        List<AksjonspunktResultat> resultatOppgitt = aksjonspunktutlederOppgitt.utledAksjonspunkterFor(new AksjonspunktUtlederInput(ref));
        if (!resultatOppgitt.isEmpty()) {
            return BehandleStegResultat.utførtMedAksjonspunktResultater(resultatOppgitt);
        }

        List<AksjonspunktResultat> resultatRegister = aksjonspunktutlederBekreftet.utledAksjonspunkterFor(new AksjonspunktUtlederInput(ref));
        return BehandleStegResultat.utførtMedAksjonspunktResultater(resultatRegister);
    }

    private List<DatoIntervallEntitet> perioderTilVurdering(Long behandlingId) {
        Optional<Behandlingsresultat> behandlingsresultat = repositoryProvider.getBehandlingsresultatRepository().hentHvisEksisterer(behandlingId);
        Optional<VilkårResultat> resultatOpt = behandlingsresultat.map(Behandlingsresultat::getVilkårResultat);
        if (resultatOpt.isPresent()) {
            VilkårResultat vilkårResultat = resultatOpt.get();
            return vilkårResultat.getVilkårene()
                .stream()
                .filter(vilkår -> VilkårType.OPPTJENINGSVILKÅRET.equals(vilkår.getVilkårType()))
                .map(Vilkår::getPerioder)
                .flatMap(Collection::stream)
                .filter(vp -> Utfall.IKKE_VURDERT.equals(vp.getGjeldendeUtfall()))
                .map(VilkårPeriode::getPeriode)
                .collect(Collectors.toList());
        }
        return List.of();
    }
}
