package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.fp;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.RyddOpptjening;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.felles.FastsettOpptjeningsperiodeStegFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.vedtak.konfig.Tid;


@BehandlingStegRef(kode = "VURDER_OPPTJ_PERIODE")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class FastsettOpptjeningsperiodeSteg extends FastsettOpptjeningsperiodeStegFelles {

    private BehandlingsresultatRepository behandlingsresultatRepository;
    private OpptjeningRepository opptjeningRepository;
    private BehandlingRepository behandlingRepository;

    FastsettOpptjeningsperiodeSteg() {
    }

    @Inject
    public FastsettOpptjeningsperiodeSteg(BehandlingRepositoryProvider repositoryProvider,
                                          OpptjeningRepository opptjeningRepository,
                                          InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste,
                                          BehandlingsresultatRepository behandlingsresultatRepository) {
        super(repositoryProvider, inngangsvilkårFellesTjeneste, BehandlingStegType.FASTSETT_OPPTJENINGSPERIODE);
        this.opptjeningRepository = opptjeningRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg,
                                   BehandlingStegType sisteSteg) {
        if (!erVilkårOverstyrt(kontekst.getBehandlingId(), Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE)) {
            super.vedHoppOverBakover(kontekst, modell, førsteSteg, sisteSteg);
            new RyddOpptjening(behandlingRepository, opptjeningRepository, kontekst).ryddOpp();
        }
    }

    @Override
    public void vedHoppOverFramover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg,
                                    BehandlingStegType sisteSteg) {
        if (!behandlingRepository.hentBehandling(kontekst.getBehandlingId()).erRevurdering()) {
            if (!erVilkårOverstyrt(kontekst.getBehandlingId(), Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE)) {
                super.vedHoppOverFramover(kontekst, modell, førsteSteg, sisteSteg);
                new RyddOpptjening(behandlingRepository, opptjeningRepository, kontekst).ryddOpp();
            }

        }
    }

    @Override
    protected boolean erVilkårOverstyrt(Long behandlingId, LocalDate fom, LocalDate tom) {
        Optional<Behandlingsresultat> behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandlingId);
        Optional<VilkårResultat> resultatOpt = behandlingsresultat.map(Behandlingsresultat::getVilkårResultat);
        if (resultatOpt.isPresent()) {
            VilkårResultat vilkårResultat = resultatOpt.get();
            return vilkårResultat.getVilkårene()
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
