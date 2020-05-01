package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.opptjening;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell.OpptjeningsvilkårResultat;

@BehandlingStegRef(kode = "VURDER_OPPTJ")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderOpptjeningsvilkårSteg extends VurderOpptjeningsvilkårStegFelles {

    @Inject
    public VurderOpptjeningsvilkårSteg(BehandlingRepositoryProvider repositoryProvider,
                                       OpptjeningRepository opptjeningRepository,
                                       InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste,
                                       @Any Instance<HåndtereAutomatiskAvslag> automatiskAvslagHåndterer) {
        super(repositoryProvider, opptjeningRepository, inngangsvilkårFellesTjeneste, BehandlingStegType.VURDER_OPPTJENINGSVILKÅR, automatiskAvslagHåndterer);
    }


    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandleStegResultat = super.utførSteg(kontekst);
        // vurder fremoverhopp
        behandleStegResultat = vurderStegResultat(kontekst, behandleStegResultat);
        return behandleStegResultat;
    }

    private BehandleStegResultat vurderStegResultat(BehandlingskontrollKontekst kontekst, BehandleStegResultat behandleStegResultat) {
        var behandling = repositoryProvider.getBehandlingRepository().hentBehandling(kontekst.getBehandlingId());
        var vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        var vilkåret = vilkårResultatRepository.hent(kontekst.getBehandlingId()).getVilkår(VilkårType.OPPTJENINGSVILKÅRET).orElseThrow();
        var vurdertePerioder = perioderTilVurdering(kontekst.getBehandlingId(), VilkårType.OPPTJENINGSVILKÅRET);

        return utledStegResultat(behandling, behandleStegResultat, vilkåret, vurdertePerioder);
    }

    BehandleStegResultat utledStegResultat(Behandling behandling, BehandleStegResultat behandleStegResultat, Vilkår vilkåret, List<DatoIntervallEntitet> vurdertePerioder) {
        var altAvslått = !vilkåret.getPerioder().isEmpty() && vilkåret.getPerioder()
            .stream()
            .filter(it -> vurdertePerioder.contains(it.getPeriode()))
            .allMatch(it -> Utfall.IKKE_OPPFYLT.equals(it.getGjeldendeUtfall()));

        if (behandleStegResultat.getAksjonspunktListe().isEmpty() && altAvslått) {
            behandling.setBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
            return BehandleStegResultat.fremoverført(FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT);
        }
        return behandleStegResultat;
    }

    @Override
    protected List<OpptjeningAktivitet> mapTilOpptjeningsaktiviteter(MapTilOpptjeningAktiviteter mapper, OpptjeningsvilkårResultat oppResultat) {
        List<OpptjeningAktivitet> aktiviteter = new ArrayList<>();
        aktiviteter.addAll(mapper.map(oppResultat.getUnderkjentePerioder(), OpptjeningAktivitetKlassifisering.BEKREFTET_AVVIST));
        aktiviteter.addAll(mapper.map(oppResultat.getBekreftetGodkjentePerioder(), OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT));
        return aktiviteter;
    }
}
