package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VURDER_MEDISINSKE_VILKÅR;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;

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
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
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
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.EtablertPleiebehovBuilder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.EtablertPleieperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomAksjonspunkt;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.utils.Hjelpetidslinjer;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell.LivetsSluttfaseDokumentasjon;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell.MedisinskVilkårResultat;

@BehandlingStegRef(value = VURDER_MEDISINSKE_VILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@ApplicationScoped
public class VurderILivetsSluttfaseSteg implements BehandlingSteg {

    private final MedisinskVilkårTjeneste medisinskVilkårTjeneste = new MedisinskVilkårTjeneste();
    private BehandlingRepositoryProvider repositoryProvider;
    private PleiebehovResultatRepository resultatRepository;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private SykdomVurderingTjeneste sykdomVurderingTjeneste;
    private SykdomGrunnlagRepository sykdomGrunnlagRepository;

    VurderILivetsSluttfaseSteg() {
        // CDI
    }

    @Inject
    public VurderILivetsSluttfaseSteg(BehandlingRepositoryProvider repositoryProvider,
                                      PleiebehovResultatRepository resultatRepository,
                                      @FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE) @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                      SykdomVurderingTjeneste sykdomVurderingTjeneste,
                                      SykdomGrunnlagRepository sykdomGrunnlagRepository) {
        this.resultatRepository = resultatRepository;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.repositoryProvider = repositoryProvider;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.sykdomVurderingTjeneste = sykdomVurderingTjeneste;
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        final Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var vilkårene = vilkårResultatRepository.hent(behandlingId);
        var perioder = perioderTilVurderingTjeneste.utled(behandlingId, VilkårType.I_LIVETS_SLUTTFASE);

        SykdomGrunnlagBehandling sykdomGrunnlagBehandling = opprettGrunnlag(perioder, behandling);

        boolean trengerAksjonspunkt = trengerAksjonspunkt(kontekst, behandling);
        if (trengerAksjonspunkt) {
            return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_LEGEERKLÆRING)));
        }

        var builder = Vilkårene.builderFraEksisterende(vilkårene);
        builder.medKantIKantVurderer(perioderTilVurderingTjeneste.getKantIKantVurderer());
        vurderVilkår(behandlingId, sykdomGrunnlagBehandling, builder, perioder);
        vilkårResultatRepository.lagre(behandlingId, builder.build());

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean trengerAksjonspunkt(BehandlingskontrollKontekst kontekst, final Behandling behandling) {
        final SykdomAksjonspunkt sykdomAksjonspunkt = sykdomVurderingTjeneste.vurderAksjonspunkt(behandlingRepository.hentBehandling(kontekst.getBehandlingId()));
        final boolean trengerInput = !sykdomAksjonspunkt.isKanLøseAksjonspunkt() || sykdomAksjonspunkt.isHarDataSomIkkeHarBlittTattMedIBehandling();
        final boolean førsteGangManuellRevurdering = behandling.erManueltOpprettet() && !behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.KONTROLLER_LEGEERKLÆRING);
        return trengerInput || førsteGangManuellRevurdering;
    }

    private SykdomGrunnlagBehandling opprettGrunnlag(NavigableSet<DatoIntervallEntitet> perioderSamlet, final Behandling behandling) {
        return sykdomGrunnlagRepository.utledOgLagreGrunnlag(
            behandling.getFagsak().getSaksnummer(),
            behandling.getUuid(),
            behandling.getAktørId(),
            behandling.getFagsak().getPleietrengendeAktørId(),
            perioderSamlet.stream()
                .map(p -> new Periode(p.getFomDato(), p.getTomDato()))
                .collect(Collectors.toList()),
            List.of()
        );
    }

    private void vurderVilkår(Long behandlingId,
                              SykdomGrunnlagBehandling sykdomGrunnlagBehandling,
                              VilkårResultatBuilder builder,
                              NavigableSet<DatoIntervallEntitet> perioder) {

        var vilkårBuilder = builder.hentBuilderFor(VilkårType.I_LIVETS_SLUTTFASE);
        for (DatoIntervallEntitet periode : perioder) {
            final var vilkårData = medisinskVilkårTjeneste.vurderPerioder(VilkårType.I_LIVETS_SLUTTFASE, periode, sykdomGrunnlagBehandling);
            oppdaterBehandlingMedVilkårresultat(vilkårData, vilkårBuilder);
            oppdaterPleiebehovResultat(behandlingId, periode, vilkårData);
        }
        builder.leggTil(vilkårBuilder);
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

        // Håndtering av Delvis innvilgelse
        if (vilkårData.getUtfallType().equals(Utfall.OPPFYLT)) {
            final var ekstraVilkårresultat = (MedisinskVilkårResultat) vilkårData.getEkstraVilkårresultat();
            var timeline = new LocalDateTimeline<>(ekstraVilkårresultat.getDokumentasjonLivetsSluttfasePerioder()
                .stream()
                .filter(it -> LivetsSluttfaseDokumentasjon.IKKE_DOKUMENTERT.equals(it.getLivetsSluttfaseDokumentasjon()))
                .filter(it -> periode.overlapper(it.getFraOgMed(), it.getTilOgMed()))
                .map(it -> new LocalDateSegment<>(it.getFraOgMed(), it.getTilOgMed(), it.getLivetsSluttfaseDokumentasjon()))
                .toList());
            timeline = timeline.disjoint(Hjelpetidslinjer.lagTidslinjeMedKunHelger(timeline));
            var tidslinjeMedHullSomMangler = Hjelpetidslinjer.utledHullSomMåTettes(timeline, new PåTversAvHelgErKantIKantVurderer());
            timeline = timeline.combine(tidslinjeMedHullSomMangler, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
            timeline.forEach(it -> vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(it.getFom(), it.getTom())
                .medUtfall(Utfall.IKKE_OPPFYLT)
                .medMerknadParametere(vilkårData.getMerknadParametere())
                .medRegelEvaluering(vilkårData.getRegelEvaluering())
                .medRegelInput(vilkårData.getRegelInput())
                .medAvslagsårsak(Avslagsårsak.IKKE_I_LIVETS_SLUTTFASE)
                .medMerknad(vilkårData.getVilkårUtfallMerknad())));
        }
    }

    private void oppdaterPleiebehovResultat(Long behandlingId, DatoIntervallEntitet periodeTilVurdering, VilkårData vilkårData) {
        var nåværendeResultat = resultatRepository.hentHvisEksisterer(behandlingId);
        var builder = nåværendeResultat.map(PleiebehovResultat::getPleieperioder).map(EtablertPleiebehovBuilder::builder).orElse(EtablertPleiebehovBuilder.builder());
        builder.tilbakeStill(periodeTilVurdering);
        final var vilkårresultat = ((MedisinskVilkårResultat) vilkårData.getEkstraVilkårresultat());

        LocalDateTimeline<Pleiegrad> pleiegradTidslinje = PleiegradKalkulator.regnUtPleiegrad(vilkårresultat);
        pleiegradTidslinje.stream()
            .map(periode -> new EtablertPleieperiode(DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom()), periode.getValue()))
            .forEach(builder::leggTil);

        resultatRepository.lagreOgFlush(behandlingId, builder);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg, BehandlingStegType sisteSteg) {
        final var perioder = perioderTilVurderingTjeneste.utled(kontekst.getBehandlingId(), VilkårType.I_LIVETS_SLUTTFASE);
        perioder.forEach(periode -> {
            if (!erVilkårOverstyrt(kontekst.getBehandlingId(), periode.getFomDato(), periode.getTomDato())) {
                Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
                RyddVilkårTyper ryddVilkårTyper = new RyddVilkårTyper(modell, repositoryProvider, behandling, kontekst);
                ryddVilkårTyper.ryddVedTilbakeføring();
                behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
            }
        });
    }

    protected boolean erVilkårOverstyrt(Long behandlingId, LocalDate fom, LocalDate tom) {
        Optional<Vilkårene> resultatOpt = vilkårResultatRepository.hentHvisEksisterer(behandlingId);
        if (resultatOpt.isPresent()) {
            Vilkårene vilkårene = resultatOpt.get();
            return vilkårene.getVilkårene()
                .stream()
                .filter(vilkår -> VilkårType.I_LIVETS_SLUTTFASE.equals(vilkår.getVilkårType()))
                .map(Vilkår::getPerioder)
                .flatMap(Collection::stream)
                .filter(it -> it.getPeriode().overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)))
                .anyMatch(VilkårPeriode::getErOverstyrt);
        }
        return false;
    }
}
