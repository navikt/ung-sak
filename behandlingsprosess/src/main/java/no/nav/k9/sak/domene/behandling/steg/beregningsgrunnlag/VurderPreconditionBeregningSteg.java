package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.perioder.VilkårsPerioderTilVurderingTjeneste;

@FagsakYtelseTypeRef
@BehandlingStegRef(kode = "PRECONDITION_BERGRUNN")
@BehandlingTypeRef
@ApplicationScoped
public class VurderPreconditionBeregningSteg implements BeregningsgrunnlagSteg {

    private VilkårResultatRepository vilkårResultatRepository;
    private BehandlingRepository behandlingRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste;

    protected VurderPreconditionBeregningSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderPreconditionBeregningSteg(VilkårResultatRepository vilkårResultatRepository,
                                           BehandlingRepository behandlingRepository,
                                           @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.behandlingRepository = behandlingRepository;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var vilkåret = vilkårResultatRepository.hent(kontekst.getBehandlingId())
            .getVilkår(VilkårType.OPPTJENINGSVILKÅRET)
            .orElseThrow();
        var vurdertePerioder = vurdertePerioder(VilkårType.OPPTJENINGSVILKÅRET, behandling);

        var altAvslått = !vilkåret.getPerioder().isEmpty() && vilkåret.getPerioder()
            .stream()
            .filter(it -> vurdertePerioder.contains(it.getPeriode()))
            .allMatch(it -> Utfall.IKKE_OPPFYLT.equals(it.getGjeldendeUtfall()));

        if (altAvslått) {
            behandling.setBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
            behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
            return BehandleStegResultat.fremoverført(FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT);
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    public Set<DatoIntervallEntitet> vurdertePerioder(VilkårType vilkårType, Behandling behandling) {
        var tjeneste = FagsakYtelseTypeRef.Lookup.find(perioderTilVurderingTjeneste, behandling.getFagsakYtelseType()).orElseThrow();
        return tjeneste.utled(behandling.getId(), vilkårType);
    }
}
