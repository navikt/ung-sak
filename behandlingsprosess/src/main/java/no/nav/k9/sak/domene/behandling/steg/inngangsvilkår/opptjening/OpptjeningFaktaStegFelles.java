package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.opptjening;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.AksjonspunktutlederForVurderBekreftetOpptjening;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.AksjonspunktutlederForVurderOppgittOpptjening;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.inngangsvilkår.opptjening.OpptjeningsVilkårTjeneste;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

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
        Optional<Vilkårene> resultatOpt = repositoryProvider.getVilkårResultatRepository().hentHvisEksisterer(behandlingId);
        if (resultatOpt.isPresent()) {
            Vilkårene vilkårene = resultatOpt.get();
            return vilkårene.getVilkårene()
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
