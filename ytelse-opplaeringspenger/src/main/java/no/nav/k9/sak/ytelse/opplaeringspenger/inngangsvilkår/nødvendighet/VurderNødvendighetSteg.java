package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.NødvendighetGodkjenningStatus.GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.NødvendighetGodkjenningStatus.IKKE_GODKJENT;
import static no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.NødvendighetGodkjenningStatus.MANGLER_VURDERING;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.medisinsk.Pleiegrad;
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
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.OppfyltVilkårTidslinjeUtleder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.EtablertPleiebehovBuilder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.EtablertPleieperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderHolder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttaksPerioderGrunnlag;

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
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;
    private PleiebehovResultatRepository resultatRepository;
    private final VurderNødvendighetTjeneste vurderNødvendighetTjeneste = new VurderNødvendighetTjeneste();

    VurderNødvendighetSteg() {
        // CDI
    }

    @Inject
    public VurderNødvendighetSteg(BehandlingRepositoryProvider repositoryProvider,
                                  @FagsakYtelseTypeRef(OPPLÆRINGSPENGER) @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                  VurdertOpplæringRepository vurdertOpplæringRepository,
                                  UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository,
                                  PleiebehovResultatRepository resultatRepository) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.repositoryProvider = repositoryProvider;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.vurdertOpplæringRepository = vurdertOpplæringRepository;
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
        this.resultatRepository = resultatRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());

        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårene)
            .medKantIKantVurderer(perioderTilVurderingTjeneste.getKantIKantVurderer());
        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.NØDVENDIG_OPPLÆRING);

        var tidslinjeTilVurdering = TidslinjeUtil.tilTidslinjeKomprimert(perioderTilVurderingTjeneste.utled(kontekst.getBehandlingId(), VilkårType.NØDVENDIG_OPPLÆRING));

        var tidslinjeIkkeGodkjentTidligereVilkår = leggTilVilkårsresultatForIkkeGodkjentTidligereVilkår(vilkårene, vilkårBuilder, tidslinjeTilVurdering);
        tidslinjeTilVurdering = tidslinjeTilVurdering.disjoint(tidslinjeIkkeGodkjentTidligereVilkår);

        var uttaksPerioderGrunnlag = uttakPerioderGrunnlagRepository.hentGrunnlag(kontekst.getBehandlingId());
        var perioderFraSøknad = uttaksPerioderGrunnlag.map(UttaksPerioderGrunnlag::getRelevantSøknadsperioder)
            .map(UttakPerioderHolder::getPerioderFraSøknadene)
            .orElse(Set.of());
        var vurdertOpplæringGrunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(kontekst.getBehandlingId());

        var tidslinjeTilVurderingMedNødvendighetsgodkjenning = vurderNødvendighetTjeneste.hentTidslinjeTilVurderingMedNødvendighetsGodkjenning(perioderFraSøknad, vurdertOpplæringGrunnlag.orElse(null), tidslinjeTilVurdering);

        var manglerVurderingTidslinje = tidslinjeTilVurderingMedNødvendighetsgodkjenning.filterValue(godkjenning -> Objects.equals(godkjenning, MANGLER_VURDERING));
        if (!manglerVurderingTidslinje.isEmpty()) {
            return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(
                AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.VURDER_NØDVENDIGHET)));
        }

        var godkjentTidslinje = tidslinjeTilVurderingMedNødvendighetsgodkjenning.filterValue(godkjenning -> Objects.equals(godkjenning, GODKJENT));
        var ikkeGodkjentTidslinje = tidslinjeTilVurderingMedNødvendighetsgodkjenning.filterValue(godkjenning -> Objects.equals(godkjenning, IKKE_GODKJENT));

        leggTilVilkårResultat(vilkårBuilder, godkjentTidslinje, Utfall.OPPFYLT, Avslagsårsak.UDEFINERT);
        leggTilVilkårResultat(vilkårBuilder, ikkeGodkjentTidslinje, Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_NØDVENDIG);

        vilkårResultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), vilkårResultatBuilder.build());

        lagreResultat(kontekst);

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private void leggTilVilkårResultat(VilkårBuilder vilkårBuilder, LocalDateTimeline<?> tidslinje, Utfall utfall, Avslagsårsak avslagsårsak) {
        TidslinjeUtil.tilDatoIntervallEntiteter(tidslinje)
            .forEach(datoIntervallEntitet -> vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(datoIntervallEntitet)
                .medUtfall(utfall)
                .medAvslagsårsak(avslagsårsak)));
    }

    private LocalDateTimeline<Boolean> leggTilVilkårsresultatForIkkeGodkjentTidligereVilkår(Vilkårene vilkårene, VilkårBuilder vilkårBuilder, LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        var sykdomsTidslinje = OppfyltVilkårTidslinjeUtleder.utled(vilkårene, VilkårType.LANGVARIG_SYKDOM);
        var godkjentInstitusjonTidslinje = OppfyltVilkårTidslinjeUtleder.utled(vilkårene, VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON);
        var gjennomførtOpplæringTidslinje = OppfyltVilkårTidslinjeUtleder.utled(vilkårene, VilkårType.GJENNOMGÅ_OPPLÆRING);

        var tidslinjeUtenGodkjentInstitusjon = tidslinjeTilVurdering.disjoint(godkjentInstitusjonTidslinje);
        var tidslinjeUtenSykdomsvilkår = tidslinjeTilVurdering.disjoint(sykdomsTidslinje).disjoint(tidslinjeUtenGodkjentInstitusjon);
        var tidslinjeUtenGjennomgåttOpplæring = tidslinjeTilVurdering.disjoint(gjennomførtOpplæringTidslinje).disjoint(tidslinjeUtenGodkjentInstitusjon).disjoint(tidslinjeUtenSykdomsvilkår);

        leggTilVilkårResultat(vilkårBuilder, tidslinjeUtenGodkjentInstitusjon, Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_GODKJENT_INSTITUSJON);
        leggTilVilkårResultat(vilkårBuilder, tidslinjeUtenSykdomsvilkår, Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_DOKUMENTERT_SYKDOM_SKADE_ELLER_LYTE); // TODO: Endre til noe mer fornuftig
        leggTilVilkårResultat(vilkårBuilder, tidslinjeUtenGjennomgåttOpplæring, Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_GJENNOMGÅTT_OPPLÆRING);

        return tidslinjeUtenGodkjentInstitusjon.crossJoin(tidslinjeUtenSykdomsvilkår).crossJoin(tidslinjeUtenGjennomgåttOpplæring);
    }

    private void lagreResultat(BehandlingskontrollKontekst kontekst) {
        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());

        List<EtablertPleieperiode> pleieperioder = vilkårene.getVilkår(VilkårType.NØDVENDIG_OPPLÆRING)
            .orElseThrow()
            .getPerioder()
            .stream()
            .filter(vilkårPeriode -> List.of(Utfall.OPPFYLT, Utfall.IKKE_OPPFYLT).contains(vilkårPeriode.getUtfall()))
            .map(this::lagPleieperiode)
            .toList();

        EtablertPleiebehovBuilder builder = EtablertPleiebehovBuilder.builder();

        pleieperioder.forEach(builder::leggTil);

        resultatRepository.lagreOgFlush(kontekst.getBehandlingId(), builder);
    }

    private EtablertPleieperiode lagPleieperiode(VilkårPeriode vilkårPeriode) {
        return new EtablertPleieperiode(vilkårPeriode.getPeriode(), vilkårPeriode.getGjeldendeUtfall() == Utfall.OPPFYLT ? Pleiegrad.OPPLÆRINGSPENGER : Pleiegrad.INGEN);
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
