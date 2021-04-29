package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
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
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.RyddVilkårTyper;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.MedisinskVilkårResultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.PleiePeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.Pleiegrad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.EtablertPleiebehovBuilder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.EtablertPleieperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomAksjonspunkt;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingService;

@BehandlingStegRef(kode = "VURDER_MEDISINSK")
@BehandlingTypeRef
@FagsakYtelseTypeRef("PSB")
@ApplicationScoped
public class VurderSykdomOgKontinuerligTilsynSteg implements BehandlingSteg {

    private BehandlingRepositoryProvider repositoryProvider;
    private PleiebehovResultatRepository resultatRepository;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private MedisinskVilkårTjeneste medisinskVilkårTjeneste = new MedisinskVilkårTjeneste();
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private SykdomVurderingService sykdomVurderingService;
    private SykdomGrunnlagRepository sykdomGrunnlagRepository;

    VurderSykdomOgKontinuerligTilsynSteg() {
        // CDI
    }

    @Inject
    public VurderSykdomOgKontinuerligTilsynSteg(BehandlingRepositoryProvider repositoryProvider,
                                                PleiebehovResultatRepository resultatRepository,
                                                @FagsakYtelseTypeRef("PSB") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                                SykdomVurderingService sykdomVurderingService,
                                                SykdomGrunnlagRepository sykdomGrunnlagRepository) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.repositoryProvider = repositoryProvider;
        this.resultatRepository = resultatRepository;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.sykdomVurderingService = sykdomVurderingService;
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        final var perioderUnder18år = perioderTilVurderingTjeneste.utled(kontekst.getBehandlingId(), VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
        final var perioder18år = perioderTilVurderingTjeneste.utled(kontekst.getBehandlingId(), VilkårType.MEDISINSKEVILKÅR_18_ÅR);
        final var perioderSamlet = union(perioderUnder18år, perioder18år);
        
        final Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        final SykdomGrunnlagBehandling sykdomGrunnlagBehandling = opprettGrunnlag(perioderSamlet, behandling);

        if (trengerAksjonspunkt(kontekst, behandling, sykdomGrunnlagBehandling)) {
            return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_LEGEERKLÆRING)));
        }

        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());
        var builder = Vilkårene.builderFraEksisterende(vilkårene);
        vurder(kontekst, sykdomGrunnlagBehandling, builder, VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR, perioderUnder18år);
        vurder(kontekst, sykdomGrunnlagBehandling, builder, VilkårType.MEDISINSKEVILKÅR_18_ÅR, perioder18år);
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), builder.build());

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private SykdomGrunnlagBehandling opprettGrunnlag(NavigableSet<DatoIntervallEntitet> perioderSamlet, final Behandling behandling) {
        return sykdomGrunnlagRepository.opprettGrunnlag(
            behandling.getFagsak().getSaksnummer(),
            behandling.getUuid(),
            behandling.getAktørId(),
            behandling.getFagsak().getPleietrengendeAktørId(),
            perioderSamlet.stream()
                .map(p -> new Periode(p.getFomDato(), p.getTomDato()))
                .collect(Collectors.toList())
        );
    }
    
    private boolean trengerAksjonspunkt(BehandlingskontrollKontekst kontekst, final Behandling behandling,
            final SykdomGrunnlagBehandling sykdomGrunnlagBehandling) {
        final SykdomAksjonspunkt sykdomAksjonspunkt = sykdomVurderingService.vurderAksjonspunkt(behandlingRepository.hentBehandling(kontekst.getBehandlingId()));
        final boolean trengerInput = !sykdomAksjonspunkt.isKanLøseAksjonspunkt();
        final boolean førsteGangManuellRevurdering = behandling.erManueltOpprettet() && sykdomGrunnlagBehandling.isFørsteGrunnlagPåBehandling();
        return trengerInput || førsteGangManuellRevurdering;
    }

    private void vurder(BehandlingskontrollKontekst kontekst,
            SykdomGrunnlagBehandling sykdomGrunnlagBehandling,
            VilkårResultatBuilder builder,
            VilkårType vilkåret,
            NavigableSet<DatoIntervallEntitet> perioder) {
        
        if (perioder.isEmpty()) {
            return;
        }
        
        var vilkårBuilder = builder.hentBuilderFor(vilkåret);
        for (DatoIntervallEntitet periode : perioder) {
            final var vilkårData = medisinskVilkårTjeneste.vurderPerioder(vilkåret, kontekst, periode, sykdomGrunnlagBehandling);
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

        vilkårresultat.getPleieperioder().forEach(periode -> builder.leggTil(utledPeriode(periode)));
        resultatRepository.lagreOgFlush(kontekst.getBehandlingId(), builder);
    }

    private EtablertPleieperiode utledPeriode(PleiePeriode periode) {
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
            ekstraVilkårresultat.getPleieperioder()
                .stream()
                .filter(it -> Pleiegrad.INGEN.equals(it.getGrad()))
                .filter(it -> periode.overlapper(it.getFraOgMed(), it.getTilOgMed()))
                .forEach(it -> vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(it.getFraOgMed(), it.getTilOgMed())
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
        håndterHoppOverBakover(kontekst, modell, VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
        håndterHoppOverBakover(kontekst, modell, VilkårType.MEDISINSKEVILKÅR_18_ÅR);
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
    
    private static final <T> NavigableSet<T> union(NavigableSet<T> s1, NavigableSet<T> s2) {
        final var resultat = new TreeSet<>(s1);
        resultat.addAll(s2);
        return resultat;
    }
}
