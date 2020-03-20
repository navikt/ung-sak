package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.opptjening;

import static java.util.Collections.singletonList;

import java.util.List;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.InngangsvilkårStegImpl;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkaar.RegelResultat;
import no.nav.k9.sak.inngangsvilkaar.regelmodell.opptjening.OpptjeningsPeriode;

public abstract class FastsettOpptjeningsperiodeStegFelles extends InngangsvilkårStegImpl {

    private OpptjeningRepository opptjeningRepository;

    private static List<VilkårType> STØTTEDE_VILKÅR = singletonList(VilkårType.OPPTJENINGSPERIODEVILKÅR);

    protected FastsettOpptjeningsperiodeStegFelles() {
        // CDI
    }

    protected FastsettOpptjeningsperiodeStegFelles(BehandlingRepositoryProvider repositoryProvider, InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste, BehandlingStegType behandlingStegType) {
        super(repositoryProvider, inngangsvilkårFellesTjeneste, behandlingStegType);
        this.opptjeningRepository = repositoryProvider.getOpptjeningRepository();
    }

    @Override
    protected void utførtRegler(BehandlingskontrollKontekst kontekst, Behandling behandling, RegelResultat regelResultat, DatoIntervallEntitet periode) {
        OpptjeningsPeriode op = ((OpptjeningsPeriode) regelResultat.getEkstraResultaterPerPeriode().get(VilkårType.OPPTJENINGSPERIODEVILKÅR).get(periode));
        if (op == null) {
            throw new IllegalArgumentException(
                "Utvikler-feil: finner ikke resultat etter evaluering av Inngangsvilkår/Opptjening:" + behandling.getId());
        }
        Opptjening opptjening = opptjeningRepository.lagreOpptjeningsperiode(behandling, op.getOpptjeningsperiodeFom(), op.getOpptjeningsperiodeTom(), erVilkårOverstyrt(behandling.getId(), periode.getFomDato(), periode.getTomDato()));
        if (opptjening == null) {
            throw new IllegalArgumentException(
                "Utvikler-feil: får ikke persistert ny opptjeningsperiode:" + behandling.getId());
        }
    }

    @Override
    public List<VilkårType> vilkårHåndtertAvSteg() {
        return STØTTEDE_VILKÅR;
    }

}
