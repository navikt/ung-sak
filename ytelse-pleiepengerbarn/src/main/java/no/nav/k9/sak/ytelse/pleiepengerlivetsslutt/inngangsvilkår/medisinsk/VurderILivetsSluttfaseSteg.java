package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VURDER_MEDISINSKVILKÅR;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
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
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.RyddVilkårTyper;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentType;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.EtablertPleiebehovBuilder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.EtablertPleieperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokumentRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingRepository;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell.LivetsSluttfaseDokumentasjon;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell.MedisinskVilkårResultat;

@BehandlingStegRef(stegtype = VURDER_MEDISINSKVILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef("PPN")
@ApplicationScoped
public class VurderILivetsSluttfaseSteg implements BehandlingSteg {

    private BehandlingRepositoryProvider repositoryProvider;
    private PleiebehovResultatRepository resultatRepository;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private MedisinskVilkårTjeneste medisinskVilkårTjeneste = new MedisinskVilkårTjeneste();
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private SykdomGrunnlagRepository sykdomGrunnlagRepository;
    private SykdomDokumentRepository sykdomDokumentRepository;
    private SykdomVurderingRepository sykdomVurderingRepository;

    VurderILivetsSluttfaseSteg() {
        // CDI
    }

    @Inject
    public VurderILivetsSluttfaseSteg(BehandlingRepositoryProvider repositoryProvider,
                                      PleiebehovResultatRepository resultatRepository,
                                      @FagsakYtelseTypeRef("PPN") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                      SykdomGrunnlagRepository sykdomGrunnlagRepository,
                                      SykdomDokumentRepository sykdomDokumentRepository, SykdomVurderingRepository sykdomVurderingRepository) {
        this.resultatRepository = resultatRepository;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.repositoryProvider = repositoryProvider;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
        this.sykdomDokumentRepository = sykdomDokumentRepository;
        this.sykdomVurderingRepository = sykdomVurderingRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        final Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        AktørId pleietrengendeAktørId = behandling.getFagsak().getPleietrengendeAktørId();
        var vilkårene = vilkårResultatRepository.hent(behandlingId);
        var perioder = perioderTilVurderingTjeneste.utled(behandlingId, VilkårType.I_LIVETS_SLUTTFASE);

        SykdomGrunnlagBehandling sykdomGrunnlagBehandling = opprettGrunnlag(perioder, behandling);

        boolean trengerAksjonspunkt = harUklassifiserteDokumenter(pleietrengendeAktørId)
            || manglerGodkjentLegeerklæring(pleietrengendeAktørId)
            || manglerSykdomVurdering(pleietrengendeAktørId);
        final boolean førsteGangManuellRevurdering = behandling.erManueltOpprettet() && sykdomGrunnlagBehandling.isFørsteGrunnlagPåBehandling();
        if (trengerAksjonspunkt || førsteGangManuellRevurdering) {
            return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_LEGEERKLÆRING)));
        }

        var builder = Vilkårene.builderFraEksisterende(vilkårene);
        vurderVilkår(behandlingId, sykdomGrunnlagBehandling, builder, VilkårType.I_LIVETS_SLUTTFASE, perioder);
        vilkårResultatRepository.lagre(behandlingId, builder.build());

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean harUklassifiserteDokumenter(AktørId pleietrengendeAktørId) {
        return sykdomDokumentRepository.hentAlleDokumenterFor(pleietrengendeAktørId).stream().anyMatch(d -> d.getType() == SykdomDokumentType.UKLASSIFISERT);
    }

    private boolean manglerGodkjentLegeerklæring(final AktørId pleietrengende) {
        return sykdomDokumentRepository.hentGodkjenteLegeerklæringer(pleietrengende).isEmpty();
    }

    private boolean harSykdomDokumenter(AktørId pleietrengendeAktørId) {
        Set<SykdomDokumentType> aktuelleDokumenttyper = Set.of(SykdomDokumentType.LEGEERKLÆRING_SYKEHUS, SykdomDokumentType.MEDISINSKE_OPPLYSNINGER);
        return sykdomDokumentRepository.hentAlleDokumenterFor(pleietrengendeAktørId).stream().anyMatch(d -> aktuelleDokumenttyper.contains(d.getType()));
    }

    private boolean manglerSykdomVurdering(AktørId pleietrengende) {
        if (!harSykdomDokumenter(pleietrengende)) {
            //uten sykdomsdokumenter, kan ikke sykdomsvurdering utføres
            return false;
        }
        var gjeldendeSykdomVurdering = sykdomVurderingRepository.getSisteVurderingstidslinjeFor(SykdomVurderingType.LIVETS_SLUTTFASE, pleietrengende);
        var sykdomVurderingSegmenter = gjeldendeSykdomVurdering.toSegments();
        if (sykdomVurderingSegmenter.isEmpty()) {
            return true;
        }
        return sykdomVurderingSegmenter
            .stream()
            .anyMatch(sykdomVurderingSegment -> sykdomVurderingSegment.getValue() == null
                || Resultat.IKKE_VURDERT == sykdomVurderingSegment.getValue().getResultat());
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
                              VilkårType vilkåret,
                              NavigableSet<DatoIntervallEntitet> perioder) {

        var vilkårBuilder = builder.hentBuilderFor(vilkåret);
        for (DatoIntervallEntitet periode : perioder) {
            final var vilkårData = medisinskVilkårTjeneste.vurderPerioder(vilkåret, periode, sykdomGrunnlagBehandling);
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
            ekstraVilkårresultat.getDokumentasjonLivetsSluttfasePerioder()
                .stream()
                .filter(it -> LivetsSluttfaseDokumentasjon.IKKE_DOKUMENTERT.equals(it.getLivetsSluttfaseDokumentasjon()))
                .filter(it -> periode.overlapper(it.getFraOgMed(), it.getTilOgMed()))
                .forEach(it -> vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(it.getFraOgMed(), it.getTilOgMed())
                    .medUtfall(Utfall.IKKE_OPPFYLT)
                    .medMerknadParametere(vilkårData.getMerknadParametere())
                    .medRegelEvaluering(vilkårData.getRegelEvaluering())
                    .medRegelInput(vilkårData.getRegelInput())
                    .medAvslagsårsak(Avslagsårsak.MANGLENDE_DOKUMENTASJON)
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
        håndterHoppOverBakover(kontekst, modell, VilkårType.I_LIVETS_SLUTTFASE);
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
