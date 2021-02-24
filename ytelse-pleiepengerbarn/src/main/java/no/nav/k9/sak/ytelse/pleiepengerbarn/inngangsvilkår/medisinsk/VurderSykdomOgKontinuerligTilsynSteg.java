package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.RyddVilkårTyper;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.pleiebehov.PleiebehovBuilder;
import no.nav.k9.sak.domene.uttak.repo.pleiebehov.PleiebehovResultat;
import no.nav.k9.sak.domene.uttak.repo.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.domene.uttak.repo.pleiebehov.Pleieperiode;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.MedisinskVilkårResultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.PleiePeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell.Pleiegrad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingService;

@BehandlingStegRef(kode = "VURDER_MEDISINSK")
@BehandlingTypeRef
@FagsakYtelseTypeRef("PSB")
@ApplicationScoped
public class VurderSykdomOgKontinuerligTilsynSteg implements BehandlingSteg {

    public static final VilkårType VILKÅRET = VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR;
    private BehandlingRepositoryProvider repositoryProvider;
    private PleiebehovResultatRepository resultatRepository;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private MedisinskVilkårTjeneste medisinskVilkårTjeneste;
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
                                                MedisinskVilkårTjeneste medisinskVilkårTjeneste,
                                                SykdomVurderingService sykdomVurderingService,
                                                SykdomGrunnlagRepository sykdomGrunnlagRepository) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.repositoryProvider = repositoryProvider;
        this.resultatRepository = resultatRepository;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.medisinskVilkårTjeneste = medisinskVilkårTjeneste;
        this.sykdomVurderingService = sykdomVurderingService;
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        final var perioder = perioderTilVurderingTjeneste.utled(kontekst.getBehandlingId(), VILKÅRET); // OK
        
        // TODO etter 18feb: Legge til støtte for at det legges inn vurderinger på utsiden av "perioder" som da trigger revurdering. Endre i PSBVilkårsPerioderTilVurderingTjeneste
        
        final Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        final SykdomGrunnlagBehandling sykdomGrunnlagBehandling = sykdomGrunnlagRepository.opprettGrunnlag(
            behandling.getFagsak().getSaksnummer(),
            behandling.getUuid(),
            behandling.getAktørId(),
            behandling.getFagsak().getPleietrengendeAktørId(),
            perioder.stream()
                .map(p -> new Periode(p.getFomDato(), p.getTomDato()))
                .collect(Collectors.toList())
        );
        
        final boolean trengerInput = sykdomVurderingService.harAksjonspunkt(behandlingRepository.hentBehandling(kontekst.getBehandlingId()));
        if (trengerInput || sykdomGrunnlagBehandling.isFørsteGrunnlagPåBehandling()) {
            return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_LEGEERKLÆRING)));
        }
       
        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());
        
        for (DatoIntervallEntitet periode : perioder) {
            final var vilkårData = medisinskVilkårTjeneste.vurderPerioder(kontekst, periode, sykdomGrunnlagBehandling);
            vilkårene = oppdaterBehandlingMedVilkårresultat(vilkårData, vilkårene);
            oppdaterResultatStruktur(kontekst, periode, vilkårData);
        }

        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), vilkårene);

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private void oppdaterResultatStruktur(BehandlingskontrollKontekst kontekst, DatoIntervallEntitet periodeTilVurdering, VilkårData vilkårData) {
        final var nåværendeResultat = resultatRepository.hentHvisEksisterer(kontekst.getBehandlingId());
        var builder = nåværendeResultat.map(PleiebehovResultat::getPleieperioder).map(PleiebehovBuilder::builder).orElse(PleiebehovBuilder.builder());
        builder.tilbakeStill(periodeTilVurdering);
        final var vilkårresultat = ((MedisinskVilkårResultat) vilkårData.getEkstraVilkårresultat());

        vilkårresultat.getPleieperioder().forEach(periode -> builder.leggTil(utledPeriode(periode)));
        resultatRepository.lagreOgFlush(behandlingRepository.hentBehandling(kontekst.getBehandlingId()), builder);
    }

    private Pleieperiode utledPeriode(PleiePeriode periode) {
        return new Pleieperiode(DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFraOgMed(), periode.getTilOgMed()), no.nav.k9.kodeverk.medisinsk.Pleiegrad.fraKode(periode.getGrad().name()));
    }

    private Vilkårene oppdaterBehandlingMedVilkårresultat(VilkårData vilkårData, Vilkårene vilkårene) {
        VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene);

        final var vilkårBuilder = builder.hentBuilderFor(vilkårData.getVilkårType());
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
                .forEach(it -> vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(it.getFraOgMed(), it.getTilOgMed())
                    .medUtfall(Utfall.IKKE_OPPFYLT)
                    .medMerknadParametere(vilkårData.getMerknadParametere())
                    .medRegelEvaluering(vilkårData.getRegelEvaluering())
                    .medRegelInput(vilkårData.getRegelInput())
                    .medAvslagsårsak(Avslagsårsak.IKKE_BEHOV_FOR_KONTINUERLIG_TILSYN_OG_PLEIE_PÅ_BAKGRUNN_AV_SYKDOM)
                    .medMerknad(vilkårData.getVilkårUtfallMerknad())));
        }
        builder.leggTil(vilkårBuilder);

        return builder.build();
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg, BehandlingStegType sisteSteg) {
        final var perioder = perioderTilVurderingTjeneste.utled(kontekst.getBehandlingId(), VILKÅRET);
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
                .filter(vilkår -> VILKÅRET.equals(vilkår.getVilkårType()))
                .map(Vilkår::getPerioder)
                .flatMap(Collection::stream)
                .filter(it -> it.getPeriode().overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)))
                .anyMatch(VilkårPeriode::getErOverstyrt);
        }
        return false;
    }
}
