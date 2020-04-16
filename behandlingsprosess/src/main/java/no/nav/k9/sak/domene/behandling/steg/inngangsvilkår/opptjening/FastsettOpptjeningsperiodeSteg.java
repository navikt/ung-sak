package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.opptjening;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;


@BehandlingStegRef(kode = "VURDER_OPPTJ_PERIODE")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class FastsettOpptjeningsperiodeSteg extends FastsettOpptjeningsperiodeStegFelles {

    private OpptjeningRepository opptjeningRepository;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    FastsettOpptjeningsperiodeSteg() {
    }

    @Inject
    public FastsettOpptjeningsperiodeSteg(BehandlingRepositoryProvider repositoryProvider,
                                          OpptjeningRepository opptjeningRepository,
                                          InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste) {
        super(repositoryProvider, inngangsvilkårFellesTjeneste, BehandlingStegType.FASTSETT_OPPTJENINGSPERIODE);
        this.opptjeningRepository = opptjeningRepository;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
    }

    @Override
    protected void ryddOppVilkårsPeriode(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, DatoIntervallEntitet periode) {
        super.ryddOppVilkårsPeriode(kontekst, modell, periode);
        new RyddOpptjening(behandlingRepository, opptjeningRepository, vilkårResultatRepository, kontekst).ryddOpp(periode);
    }

    @Override
    public void vedHoppOverFramover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg,
                                    BehandlingStegType sisteSteg) {
    }

    @Override
    protected boolean erVilkårOverstyrt(Long behandlingId, LocalDate fom, LocalDate tom) {
        Optional<Vilkårene> resultatOpt = vilkårResultatRepository.hentHvisEksisterer(behandlingId);
        if (resultatOpt.isPresent()) {
            Vilkårene vilkårene = resultatOpt.get();
            return vilkårene.getVilkårene()
                .stream()
                .filter(vilkår -> VilkårType.OPPTJENINGSVILKÅRET.equals(vilkår.getVilkårType()))
                .map(Vilkår::getPerioder)
                .flatMap(Collection::stream)
                .filter(it -> it.getPeriode().overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)))
                .anyMatch(VilkårPeriode::getErOverstyrt);
        }
        return false;
    }
}
