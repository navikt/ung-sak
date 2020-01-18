package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.felles;

import static java.util.Collections.singletonList;

import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårStegImpl;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultat;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsPeriode;
import no.nav.vedtak.konfig.Tid;

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
        OpptjeningsPeriode op = (OpptjeningsPeriode) regelResultat.getEkstraResultater().get(VilkårType.OPPTJENINGSPERIODEVILKÅR);
        if (op == null) {
            throw new IllegalArgumentException(
                "Utvikler-feil: finner ikke resultat etter evaluering av Inngangsvilkår/Opptjening:" + behandling.getId());
        }
        Opptjening opptjening = opptjeningRepository.lagreOpptjeningsperiode(behandling, op.getOpptjeningsperiodeFom(), op.getOpptjeningsperiodeTom(), erVilkårOverstyrt(behandling.getId(), Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE));
        if (opptjening == null) {
            throw new IllegalArgumentException(
                "Utvikler-feil: får ikke persistert ny opptjeningsperiode:" + behandling.getId());
        }
    }

    @Override
    protected boolean skipVurderingAvPeriode(BehandlingskontrollKontekst kontekst, DatoIntervallEntitet periode) {
        // for OPPTJENINGSPERIODEVIKÅRET skipper vi ikke selv om det er overstyrt.
        return false;
    }

    @Override
    public List<VilkårType> vilkårHåndtertAvSteg() {
        return STØTTEDE_VILKÅR;
    }

}
