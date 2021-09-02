package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.opptjening;

import java.util.List;
import java.util.NavigableSet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.AksjonspunktutlederForVurderBekreftetOpptjening;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.AksjonspunktutlederForVurderOppgittOpptjening;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.opptjening.OpptjeningsVilkårTjeneste;

/**
 * Steg 81 - Kontroller fakta for opptjening
 */
@BehandlingStegRef(kode = "VURDER_OPPTJ_FAKTA")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class OpptjeningFaktaSteg implements BehandlingSteg {

    private BehandlingRepositoryProvider repositoryProvider;
    private AksjonspunktutlederForVurderOppgittOpptjening aksjonspunktutlederOppgitt;
    private AksjonspunktutlederForVurderBekreftetOpptjening aksjonspunktutlederBekreftet;
    private OpptjeningsVilkårTjeneste opptjeningsVilkårTjeneste;
    private InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste;
    private Boolean overstyringFjernet;

    OpptjeningFaktaSteg() {
        // CDI
    }

    @Inject
    public OpptjeningFaktaSteg(BehandlingRepositoryProvider repositoryProvider,
                               AksjonspunktutlederForVurderBekreftetOpptjening aksjonspunktutlederBekreftet,
                               AksjonspunktutlederForVurderOppgittOpptjening aksjonspunktutlederOppgitt,
                               @FagsakYtelseTypeRef OpptjeningsVilkårTjeneste opptjeningsVilkårTjeneste,
                               InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste,
                               @KonfigVerdi(value = "OVERSTYRING_OPPTJ_AKT_FJERNET", defaultVerdi = "true") Boolean overstyringFjernet) {
        this.repositoryProvider = repositoryProvider;
        this.aksjonspunktutlederBekreftet = aksjonspunktutlederBekreftet;
        this.aksjonspunktutlederOppgitt = aksjonspunktutlederOppgitt;
        this.opptjeningsVilkårTjeneste = opptjeningsVilkårTjeneste;
        this.inngangsvilkårFellesTjeneste = inngangsvilkårFellesTjeneste;
        this.overstyringFjernet = overstyringFjernet;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        if (overstyringFjernet) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = repositoryProvider.getBehandlingRepository().hentBehandling(behandlingId);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling);

        final var perioderTilVurdering = perioderTilVurdering(ref.getBehandlingId());
        var utfall = Utfall.OPPFYLT;
        var resultat = opptjeningsVilkårTjeneste.vurderOpptjeningsVilkår(ref, perioderTilVurdering);
        for (var entry : resultat.entrySet()) {
            var vilkårUtfall = entry.getValue().getUtfallType();
            if (!Utfall.OPPFYLT.equals(vilkårUtfall)) {
                utfall = vilkårUtfall;
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
        if (!resultatRegister.isEmpty()) {
            return BehandleStegResultat.utførtMedAksjonspunktResultater(resultatRegister);
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private NavigableSet<DatoIntervallEntitet> perioderTilVurdering(Long behandlingId) {
        return inngangsvilkårFellesTjeneste.utledPerioderTilVurdering(behandlingId, VilkårType.OPPTJENINGSVILKÅRET);
    }
}
