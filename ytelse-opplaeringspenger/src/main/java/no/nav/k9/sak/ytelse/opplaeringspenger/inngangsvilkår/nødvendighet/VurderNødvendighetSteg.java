package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.RyddVilkårTyper;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;

@BehandlingStegRef(value = BehandlingStegType.VURDER_NØDVENDIGHETS_VILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER)
@ApplicationScoped
public class VurderNødvendighetSteg implements BehandlingSteg {

    private BehandlingRepositoryProvider repositoryProvider;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private VurdertOpplæringRepository vurdertOpplæringRepository;
    private final NødvendighetVilkårTjeneste nødvendighetVilkårTjeneste = new NødvendighetVilkårTjeneste();

    VurderNødvendighetSteg() {
        // CDI
    }

    @Inject
    public VurderNødvendighetSteg(BehandlingRepositoryProvider repositoryProvider,
                                  @FagsakYtelseTypeRef(OPPLÆRINGSPENGER) @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                  VurdertOpplæringRepository vurdertOpplæringRepository) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.repositoryProvider = repositoryProvider;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.vurdertOpplæringRepository = vurdertOpplæringRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());

        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårene)
            .medKantIKantVurderer(perioderTilVurderingTjeneste.getKantIKantVurderer());
        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.NØDVENDIG_OPPLÆRING);

        // TODO: Hent ut resultatet til sykdom
        var sykdomsVilkåret = vilkårene.getVilkår(VilkårType.LANGVARIG_SYKDOM)
            .orElseThrow()
            .getPerioder()
            .stream()
            .filter(it -> Objects.equals(Utfall.OPPFYLT, it.getGjeldendeUtfall()))
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toCollection(TreeSet<DatoIntervallEntitet>::new));
        var sykdomsTidslinje = TidslinjeUtil.tilTidslinjeKomprimert(sykdomsVilkåret);

        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(kontekst.getBehandlingId(), VilkårType.NØDVENDIG_OPPLÆRING);
        var tidslinjeTilVurdering = TidslinjeUtil.tilTidslinjeKomprimert(perioderTilVurdering);

        var tidslinjeUtenSykdomsvilkår = tidslinjeTilVurdering.disjoint(sykdomsTidslinje);
        leggTilVilkårResultat(vilkårBuilder, tidslinjeUtenSykdomsvilkår, Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_DOKUMENTERT_SYKDOM_SKADE_ELLER_LYTE); // TODO: Endre til noe mer fornuftig
        tidslinjeTilVurdering = tidslinjeTilVurdering.disjoint(tidslinjeUtenSykdomsvilkår);

        var vurdertOpplæringGrunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(kontekst.getBehandlingId())
            .orElse(VurdertOpplæringGrunnlag.lagTomtGrunnlag());
        var vurdertePerioder = nødvendighetVilkårTjeneste.vurderPerioder(vilkårBuilder, vurdertOpplæringGrunnlag, tidslinjeTilVurdering);

        tidslinjeTilVurdering = tidslinjeTilVurdering.disjoint(TidslinjeUtil.tilTidslinjeKomprimert(vurdertePerioder));

        vilkårResultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), vilkårResultatBuilder.build());

        if (tidslinjeTilVurdering.isEmpty()) {
            // Ingen flere perioder å vurdere
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        } else {
            // Vi har perioder uten vurdering
            return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(
                AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.VURDER_INSTITUSJON_OG_NØDVENDIGHET)));
        }
    }

    private void leggTilVilkårResultat(VilkårBuilder vilkårBuilder, LocalDateTimeline<Boolean> tidslinje, Utfall utfall, Avslagsårsak avslagsårsak) {
        TidslinjeUtil.tilDatoIntervallEntiteter(tidslinje)
            .forEach(datoIntervallEntitet -> vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(datoIntervallEntitet)
                .medUtfall(utfall)
                .medAvslagsårsak(avslagsårsak)));
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg, BehandlingStegType sisteSteg) {
        håndterHoppOverBakover(kontekst, modell, VilkårType.NØDVENDIG_OPPLÆRING);
    }

    private void håndterHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, VilkårType vilkåret) {
        final var perioder = perioderTilVurderingTjeneste.utled(kontekst.getBehandlingId(), vilkåret);
        perioder.forEach(periode -> {
            if (!erVilkårOverstyrt(vilkåret, kontekst.getBehandlingId(), periode.getFomDato(), periode.getTomDato())) {
                Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
                RyddVilkårTyper ryddVilkårTyper = new RyddVilkårTyper(modell, repositoryProvider, behandling, kontekst);
                ryddVilkårTyper.ryddVedTilbakeføring();
                behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
            }
        });
    }

    protected boolean erVilkårOverstyrt(VilkårType vilkåret, Long behandlingId, LocalDate fom, LocalDate tom) {
        Optional<Vilkårene> resultatOpt = vilkårResultatRepository.hentHvisEksisterer(behandlingId);
        if (resultatOpt.isPresent()) {
            Vilkårene vilkårene = resultatOpt.get();
            return vilkårene.getVilkårene()
                .stream()
                .filter(vilkår -> vilkåret.equals(vilkår.getVilkårType()))
                .map(Vilkår::getPerioder)
                .flatMap(Collection::stream)
                .filter(it -> it.getPeriode().overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)))
                .anyMatch(VilkårPeriode::getErOverstyrt);
        }
        return false;
    }
}
