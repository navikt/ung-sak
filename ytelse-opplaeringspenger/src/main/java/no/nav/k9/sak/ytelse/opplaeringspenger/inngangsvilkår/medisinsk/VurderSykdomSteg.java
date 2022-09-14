package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.medisinsk;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.fpsak.tidsserie.StandardCombinators;
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
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.RyddVilkårTyper;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.medisinsk.regelmodell.LangvarigSykdomPeriode;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.medisinsk.regelmodell.MedisinskVilkårResultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.EtablertPleiebehovBuilder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.EtablertPleieperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomAksjonspunkt;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.utils.Hjelpetidslinjer;

@BehandlingStegRef(value = BehandlingStegType.VURDER_MEDISINSKE_VILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER)
@ApplicationScoped
public class VurderSykdomSteg implements BehandlingSteg {

    private final MedisinskVilkårTjeneste medisinskVilkårTjeneste = new MedisinskVilkårTjeneste();
    private BehandlingRepositoryProvider repositoryProvider;
    private PleiebehovResultatRepository resultatRepository;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private SykdomVurderingTjeneste sykdomVurderingTjeneste;
    private MedisinskGrunnlagRepository medisinskGrunnlagRepository;
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;

    VurderSykdomSteg() {
        // CDI
    }

    @Inject
    public VurderSykdomSteg(BehandlingRepositoryProvider repositoryProvider,
                            PleiebehovResultatRepository resultatRepository,
                            @FagsakYtelseTypeRef(OPPLÆRINGSPENGER) @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                            SykdomVurderingTjeneste sykdomVurderingTjeneste,
                            MedisinskGrunnlagRepository medisinskGrunnlagRepository,
                            SøknadsperiodeTjeneste søknadsperiodeTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.repositoryProvider = repositoryProvider;
        this.resultatRepository = resultatRepository;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.sykdomVurderingTjeneste = sykdomVurderingTjeneste;
        this.medisinskGrunnlagRepository = medisinskGrunnlagRepository;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        final Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        if (søknadsperiodeTjeneste.utledFullstendigPeriode(kontekst.getBehandlingId()).isEmpty()) {
            if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.KONTROLLER_LEGEERKLÆRING)) {
                behandling.getAksjonspunktFor(AksjonspunktDefinisjon.KONTROLLER_LEGEERKLÆRING)
                    .avbryt();
                behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
            }
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());

        final var perioderSamlet = perioderTilVurderingTjeneste.utled(kontekst.getBehandlingId(), VilkårType.LANGVARIG_SYKDOM);

        final MedisinskGrunnlag medisinskGrunnlag = opprettGrunnlag(perioderSamlet, behandling);

        final boolean harPerioderTilVurdering = !perioderSamlet.isEmpty();
        if (harPerioderTilVurdering && trengerAksjonspunkt(kontekst, behandling)) {
            return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_LEGEERKLÆRING)));
        }

        var builder = Vilkårene.builderFraEksisterende(vilkårene);
        builder.medKantIKantVurderer(perioderTilVurderingTjeneste.getKantIKantVurderer());
        vurder(kontekst, medisinskGrunnlag, builder, VilkårType.LANGVARIG_SYKDOM, perioderSamlet);
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), builder.build());

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private MedisinskGrunnlag opprettGrunnlag(NavigableSet<DatoIntervallEntitet> perioderSamlet, final Behandling behandling) {
        return medisinskGrunnlagRepository.utledOgLagreGrunnlag(
            behandling.getFagsak().getSaksnummer(),
            behandling.getUuid(),
            behandling.getAktørId(),
            behandling.getFagsak().getPleietrengendeAktørId(),
            perioderSamlet.stream()
                .map(p -> new Periode(p.getFomDato(), p.getTomDato()))
                .collect(Collectors.toList()),
            //TODO tom liste her?
            perioderSamlet.stream()
                .map(p -> new Periode(p.getFomDato(), p.getTomDato()))
                .collect(Collectors.toList())
        );
    }

    private boolean trengerAksjonspunkt(BehandlingskontrollKontekst kontekst, final Behandling behandling) {
        final SykdomAksjonspunkt sykdomAksjonspunkt = sykdomVurderingTjeneste.vurderAksjonspunkt(behandlingRepository.hentBehandling(kontekst.getBehandlingId()));
        final boolean trengerInput = !sykdomAksjonspunkt.isKanLøseAksjonspunkt() || sykdomAksjonspunkt.isHarDataSomIkkeHarBlittTattMedIBehandling();
        final boolean førsteGangManuellRevurdering = behandling.erManueltOpprettet() && !behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.KONTROLLER_LEGEERKLÆRING);
        return trengerInput || førsteGangManuellRevurdering;
    }

    private void vurder(BehandlingskontrollKontekst kontekst,
                        MedisinskGrunnlag medisinskGrunnlag,
                        VilkårResultatBuilder builder,
                        VilkårType vilkåret,
                        NavigableSet<DatoIntervallEntitet> perioder) {

        var vilkårBuilder = builder.hentBuilderFor(vilkåret);
        for (DatoIntervallEntitet periode : perioder) {
            final var vilkårData = medisinskVilkårTjeneste.vurderPerioder(vilkåret, kontekst, periode, medisinskGrunnlag);
            oppdaterBehandlingMedVilkårresultat(vilkårData, vilkårBuilder);
            oppdaterResultatStruktur(kontekst, periode, vilkårData);
        }

        builder.leggTil(vilkårBuilder);
    }

    private void oppdaterResultatStruktur(BehandlingskontrollKontekst kontekst, DatoIntervallEntitet periodeTilVurdering, VilkårData vilkårData) {
        final var nåværendeResultat = resultatRepository.hentHvisEksisterer(kontekst.getBehandlingId());
        var builder = nåværendeResultat.map(PleiebehovResultat::getPleieperioder).map(EtablertPleiebehovBuilder::builder).orElse(EtablertPleiebehovBuilder.builder());
        builder.tilbakeStill(periodeTilVurdering);
        final var vilkårresultat = ((MedisinskVilkårResultat) vilkårData.getEkstraVilkårresultat());

        vilkårresultat.getLangvarigSykdomPerioder().forEach(periode -> builder.leggTil(utledPeriode(periode)));
        resultatRepository.lagreOgFlush(kontekst.getBehandlingId(), builder);
    }

    private EtablertPleieperiode utledPeriode(LangvarigSykdomPeriode periode) {
        return new EtablertPleieperiode(DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFraOgMed(), periode.getTilOgMed()), no.nav.k9.kodeverk.medisinsk.Pleiegrad.fraKode(periode.getGrad().name()));
    }

    private void oppdaterBehandlingMedVilkårresultat(VilkårData vilkårData, VilkårBuilder vilkårBuilder) {

        final var periode = vilkårData.getPeriode();
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periode.getFomDato(), periode.getTomDato())
            .medUtfall(vilkårData.getUtfallType())
            .medMerknadParametere(vilkårData.getMerknadParametere())
            .medRegelEvaluering(vilkårData.getRegelEvaluering())
            .medRegelInput(vilkårData.getRegelInput())
            .medAvslagsårsak(vilkårData.getAvslagsårsak())
            .medMerknad(vilkårData.getVilkårUtfallMerknad()));

        if (vilkårData.getUtfallType().equals(Utfall.OPPFYLT)) {
            final var ekstraVilkårresultat = (MedisinskVilkårResultat) vilkårData.getEkstraVilkårresultat();
            var timeline = new LocalDateTimeline<>(ekstraVilkårresultat.getLangvarigSykdomPerioder()
                .stream()
                .filter(it -> no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.Pleiegrad.INGEN.equals(it.getGrad()))
                .filter(it -> periode.overlapper(it.getFraOgMed(), it.getTilOgMed()))
                .map(it -> new LocalDateSegment<>(it.getFraOgMed(), it.getTilOgMed(), it.getGrad()))
                .toList());

            var fullstendigAvslagsTidslinje = new LocalDateTimeline<>(timeline.toSegments());
            timeline = timeline.disjoint(Hjelpetidslinjer.lagTidslinjeMedKunHelger(timeline));
            var tidslinjeMedHullSomMangler = Hjelpetidslinjer.utledHullSomMåTettes(timeline, new PåTversAvHelgErKantIKantVurderer());
            timeline = timeline.combine(tidslinjeMedHullSomMangler, StandardCombinators::coalesceRightHandSide, JoinStyle.CROSS_JOIN);
            var manglendeAvslag = fullstendigAvslagsTidslinje.disjoint(timeline);
            timeline = timeline.combine(manglendeAvslag, StandardCombinators::coalesceRightHandSide, JoinStyle.CROSS_JOIN);
            timeline.forEach(it -> vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(it.getFom(), it.getTom())
                .medUtfall(Utfall.IKKE_OPPFYLT)
                .medMerknadParametere(vilkårData.getMerknadParametere())
                .medRegelEvaluering(vilkårData.getRegelEvaluering())
                .medRegelInput(vilkårData.getRegelInput())
                .medAvslagsårsak(Avslagsårsak.IKKE_BEHOV_FOR_KONTINUERLIG_TILSYN_OG_PLEIE_PÅ_BAKGRUNN_AV_SYKDOM)
                .medMerknad(vilkårData.getVilkårUtfallMerknad())));

        }
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg, BehandlingStegType sisteSteg) {
        håndterHoppOverBakover(kontekst, modell, VilkårType.LANGVARIG_SYKDOM);
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
