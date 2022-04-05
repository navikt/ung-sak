package no.nav.k9.sak.ytelse.pleiepengerbarn.iverksett;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.hendelse.vedtak.SakMedPeriode;
import no.nav.k9.sak.hendelse.vedtak.VurderOmVedtakPåvirkerSakerTjeneste;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.ErEndringPåEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagService;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.EndringUnntakEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.SamtidigUttakTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.PSBVilkårsPerioderTilVurderingTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
public class VurderOmPSBVedtakPåvirkerAndreSakerTjeneste implements VurderOmVedtakPåvirkerSakerTjeneste {

    private static final Logger log = LoggerFactory.getLogger(VurderOmPSBVedtakPåvirkerAndreSakerTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private SykdomGrunnlagRepository sykdomGrunnlagRepository;
    private SykdomVurderingRepository sykdomVurderingRepository;
    private SykdomGrunnlagService sykdomGrunnlagService;
    private ErEndringPåEtablertTilsynTjeneste erEndringPåEtablertTilsynTjeneste;
    private EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste;
    private SamtidigUttakTjeneste samtidigUttakTjeneste;
    private VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste;

    VurderOmPSBVedtakPåvirkerAndreSakerTjeneste() {
    }

    @Inject
    public VurderOmPSBVedtakPåvirkerAndreSakerTjeneste(BehandlingRepository behandlingRepository,
                                                       FagsakRepository fagsakRepository,
                                                       VilkårResultatRepository vilkårResultatRepository,
                                                       SykdomGrunnlagRepository sykdomGrunnlagRepository,
                                                       SykdomVurderingRepository sykdomVurderingRepository,
                                                       SykdomGrunnlagService sykdomGrunnlagService,
                                                       ErEndringPåEtablertTilsynTjeneste erEndringPåEtablertTilsynTjeneste,
                                                       EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste,
                                                       @FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN) PSBVilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste,
                                                       SamtidigUttakTjeneste samtidigUttakTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
        this.sykdomVurderingRepository = sykdomVurderingRepository;
        this.sykdomGrunnlagService = sykdomGrunnlagService;
        this.erEndringPåEtablertTilsynTjeneste = erEndringPåEtablertTilsynTjeneste;
        this.endringUnntakEtablertTilsynTjeneste = endringUnntakEtablertTilsynTjeneste;
        this.samtidigUttakTjeneste = samtidigUttakTjeneste;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
    }

    @Override
    public List<Saksnummer> utledSakerSomErKanVærePåvirket(Ytelse vedtakHendelse) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(vedtakHendelse.getSaksnummer())).orElseThrow();
        Behandling vedtattBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).orElseThrow();

        AktørId pleietrengende = vedtattBehandling.getFagsak().getPleietrengendeAktørId();
        List<Saksnummer> alleSaksnummer = sykdomVurderingRepository.hentAlleSaksnummer(pleietrengende);

        var result = new ArrayList<Saksnummer>();
        for (Saksnummer kandidatsaksnummer : alleSaksnummer) {
            if (!kandidatsaksnummer.equals(fagsak.getSaksnummer())) {
                var kandidatFagsak = fagsakRepository.hentSakGittSaksnummer(kandidatsaksnummer, false).orElseThrow();
                var sisteBehandlingPåKandidat = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(kandidatFagsak.getId()).orElseThrow();
                boolean skalRevurderesPgaSykdom = vurderBehovForRevurderingPgaSykdom(pleietrengende, kandidatsaksnummer, sisteBehandlingPåKandidat);
                var referanse = BehandlingReferanse.fra(sisteBehandlingPåKandidat);
                boolean skalRevurderesPgaEtablertTilsyn = skalRevurderesPgaEtablertTilsyn(referanse);
                boolean skalRevurderesPgaNattevåkOgBeredskap = skalRevurderesPgaNattevåkOgBeredskap(referanse);
                boolean skalRevurderesPgaEndretUttak = skalRevurderesPgaUttak(sisteBehandlingPåKandidat, referanse);
                if (skalRevurderesPgaSykdom || skalRevurderesPgaEtablertTilsyn || skalRevurderesPgaNattevåkOgBeredskap || skalRevurderesPgaEndretUttak) {
                    result.add(kandidatsaksnummer);
                    log.info("Sak='{}' revurderes pga => sykdom={}, etablertTilsyn={}, nattevåk&beredskap={}, uttak={}", kandidatsaksnummer, skalRevurderesPgaSykdom, skalRevurderesPgaEtablertTilsyn, skalRevurderesPgaNattevåkOgBeredskap, skalRevurderesPgaEndretUttak);
                }
            }
        }

        return result;
    }

    @Override
    public List<SakMedPeriode> utledSakerMedPerioderSomErKanVærePåvirket(Ytelse vedtakHendelse) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(vedtakHendelse.getSaksnummer())).orElseThrow();
        Behandling vedtattBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).orElseThrow();

        AktørId pleietrengende = vedtattBehandling.getFagsak().getPleietrengendeAktørId();
        List<Saksnummer> alleSaksnummer = sykdomVurderingRepository.hentAlleSaksnummer(pleietrengende);

        var result = new ArrayList<SakMedPeriode>();
        for (Saksnummer kandidatsaksnummer : alleSaksnummer) {
            if (!kandidatsaksnummer.equals(fagsak.getSaksnummer())) {
                var kandidatFagsak = fagsakRepository.hentSakGittSaksnummer(kandidatsaksnummer, false).orElseThrow();
                var sisteBehandlingPåKandidat = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(kandidatFagsak.getId()).orElseThrow();
                var skalRevurderesPgaSykdom = perioderMedRevurderingSykdom(pleietrengende, kandidatsaksnummer, sisteBehandlingPåKandidat);
                var referanse = BehandlingReferanse.fra(sisteBehandlingPåKandidat);
                var skalRevurderesPgaEtablertTilsyn = perioderMedRevurderingPgaEtablertTilsyn(referanse);
                var skalRevurderesPgaNattevåkOgBeredskap = perioderMedRevurderesPgaNattevåkOgBeredskap(referanse);
                var skalRevurderesPgaEndretUttak = perioderMedRevurderingPgaUttak(sisteBehandlingPåKandidat, referanse);

                if (!skalRevurderesPgaSykdom.isEmpty() || !skalRevurderesPgaEtablertTilsyn.isEmpty() || !skalRevurderesPgaNattevåkOgBeredskap.isEmpty() || !skalRevurderesPgaEndretUttak.isEmpty()) {
                    TreeSet<DatoIntervallEntitet> perioderMedEndring = utledPerioder(skalRevurderesPgaSykdom, skalRevurderesPgaEtablertTilsyn, skalRevurderesPgaNattevåkOgBeredskap, skalRevurderesPgaEndretUttak);
                    result.add(new SakMedPeriode(kandidatsaksnummer, perioderMedEndring));
                    log.info("Sak='{}' revurderes pga => sykdom={}, etablertTilsyn={}, nattevåk&beredskap={}, uttak={}", kandidatsaksnummer, !skalRevurderesPgaSykdom.isEmpty(), !skalRevurderesPgaEtablertTilsyn.isEmpty(), !skalRevurderesPgaNattevåkOgBeredskap.isEmpty(), !skalRevurderesPgaEndretUttak.isEmpty());
                }
            }
        }

        return result;
    }

    private TreeSet<DatoIntervallEntitet> utledPerioder(NavigableSet<DatoIntervallEntitet> skalRevurderesPgaSykdom, NavigableSet<DatoIntervallEntitet> skalRevurderesPgaEtablertTilsyn, NavigableSet<DatoIntervallEntitet> skalRevurderesPgaNattevåkOgBeredskap, NavigableSet<DatoIntervallEntitet> skalRevurderesPgaEndretUttak) {
        var perioderMedEndring = new TreeSet<>(skalRevurderesPgaEtablertTilsyn);
        perioderMedEndring.addAll(skalRevurderesPgaNattevåkOgBeredskap);
        perioderMedEndring.addAll(skalRevurderesPgaSykdom);
        perioderMedEndring.addAll(skalRevurderesPgaEndretUttak);

        var tidslinje = new LocalDateTimeline<Boolean>(List.of());

        for (DatoIntervallEntitet periode : perioderMedEndring) {
            tidslinje = tidslinje.combine(new LocalDateSegment<>(periode.toLocalDateInterval(), true), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        // tett hull
        var kantIKantVurderer = vilkårsPerioderTilVurderingTjeneste.getKantIKantVurderer();
        var segmenterSomMangler = utledHullSomMåTettes(tidslinje, kantIKantVurderer);
        for (LocalDateSegment<Boolean> segment : segmenterSomMangler) {
            tidslinje = tidslinje.combine(segment, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        return tidslinje.compress()
            .toSegments()
            .stream()
            .map(it -> DatoIntervallEntitet.fra(it.getLocalDateInterval()))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private List<LocalDateSegment<Boolean>> utledHullSomMåTettes(LocalDateTimeline<Boolean> tidslinjen, KantIKantVurderer kantIKantVurderer) {
        var segmenter = tidslinjen.compress().toSegments();

        LocalDateSegment<Boolean> periode = null;
        var resultat = new ArrayList<LocalDateSegment<Boolean>>();

        for (LocalDateSegment<Boolean> segment : segmenter) {
            if (periode == null) {
                periode = segment;
            } else if (kantIKantVurderer.erKantIKant(DatoIntervallEntitet.fra(segment.getLocalDateInterval()), DatoIntervallEntitet.fra(periode.getLocalDateInterval()))) {
                resultat.add(new LocalDateSegment<>(periode.getFom(), segment.getTom(), periode.getValue()));
            } else {
                periode = segment;
            }
        }

        return resultat;
    }

    private boolean skalRevurderesPgaUttak(Behandling sisteBehandlingPåKandidat, BehandlingReferanse referanse) {
        if (!sisteBehandlingPåKandidat.getStatus().erFerdigbehandletStatus() && !samtidigUttakTjeneste.harKommetTilUttak(referanse)) {
            return false;
        }
        return samtidigUttakTjeneste.isEndringerMedUbesluttedeData(referanse);
    }

    private NavigableSet<DatoIntervallEntitet> perioderMedRevurderingPgaUttak(Behandling sisteBehandlingPåKandidat, BehandlingReferanse referanse) {
        if (!sisteBehandlingPåKandidat.getStatus().erFerdigbehandletStatus() && !samtidigUttakTjeneste.harKommetTilUttak(referanse)) {
            return new TreeSet<>();
        }
        return samtidigUttakTjeneste.perioderMedEndringerMedUbesluttedeData(referanse);
    }

    private boolean skalRevurderesPgaNattevåkOgBeredskap(BehandlingReferanse referanse) {
        return endringUnntakEtablertTilsynTjeneste.harEndringerSidenBehandling(referanse.getBehandlingId(), referanse.getPleietrengendeAktørId());
    }

    private NavigableSet<DatoIntervallEntitet> perioderMedRevurderesPgaNattevåkOgBeredskap(BehandlingReferanse referanse) {
        return new TreeSet<>(endringUnntakEtablertTilsynTjeneste.utledRelevanteEndringerSidenBehandling(referanse.getBehandlingId(), referanse.getPleietrengendeAktørId()));
    }

    private boolean skalRevurderesPgaEtablertTilsyn(BehandlingReferanse referanse) {
        return erEndringPåEtablertTilsynTjeneste.erEndringerSidenBehandling(referanse);
    }

    private NavigableSet<DatoIntervallEntitet> perioderMedRevurderingPgaEtablertTilsyn(BehandlingReferanse referanse) {
        return erEndringPåEtablertTilsynTjeneste.perioderMedEndringerFraEksisterendeVersjon(referanse)
            .compress()
            .toSegments()
            .stream()
            .filter(it -> Objects.nonNull(it.getValue()))
            .map(it -> DatoIntervallEntitet.fra(it.getLocalDateInterval()))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private boolean vurderBehovForRevurderingPgaSykdom(AktørId pleietrengende, Saksnummer kandidatsaksnummer, Behandling sisteBehandlingPåKandidat) {
        var endringerISøktePerioder = perioderMedRevurderingSykdom(pleietrengende, kandidatsaksnummer, sisteBehandlingPåKandidat);

        return !endringerISøktePerioder.isEmpty();
    }

    private NavigableSet<DatoIntervallEntitet> perioderMedRevurderingSykdom(AktørId pleietrengende, Saksnummer kandidatsaksnummer, Behandling sisteBehandlingPåKandidat) {
        SykdomGrunnlagBehandling kandidatSykdomBehandling = sykdomGrunnlagRepository.hentGrunnlagForBehandling(sisteBehandlingPåKandidat.getUuid())
            .orElseThrow();
        var behandling = behandlingRepository.hentBehandlingHvisFinnes(kandidatSykdomBehandling.getBehandlingUuid()).orElseThrow();
        var vurderingsperioder = utledVurderingsperiode(behandling);
        var manglendeOmsorgenForPerioder = sykdomGrunnlagService.hentManglendeOmsorgenForPerioder(behandling.getId());
        var utledetGrunnlag = sykdomGrunnlagRepository.utledGrunnlag(kandidatsaksnummer, kandidatSykdomBehandling.getBehandlingUuid(), pleietrengende, vurderingsperioder, manglendeOmsorgenForPerioder);
        final LocalDateTimeline<Boolean> endringerISøktePerioder = sykdomGrunnlagService.sammenlignGrunnlag(Optional.of(kandidatSykdomBehandling.getGrunnlag()), utledetGrunnlag).getDiffPerioder();

        return endringerISøktePerioder.compress()
            .toSegments()
            .stream()
            .filter(it -> Objects.nonNull(it.getValue()))
            .map(it -> DatoIntervallEntitet.fra(it.getLocalDateInterval()))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private List<Periode> utledVurderingsperiode(Behandling behandling) {
        var vilkårene = vilkårResultatRepository.hent(behandling.getId());
        var vurderingsperioder = vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR)
            .map(Vilkår::getPerioder)
            .orElse(List.of())
            .stream()
            .map(VilkårPeriode::getPeriode)
            .map(it -> new Periode(it.getFomDato(), it.getTomDato()))
            .collect(Collectors.toCollection(ArrayList::new));

        vurderingsperioder.addAll(vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_18_ÅR)
            .map(Vilkår::getPerioder)
            .orElse(List.of())
            .stream()
            .map(VilkårPeriode::getPeriode)
            .map(it -> new Periode(it.getFomDato(), it.getTomDato()))
            .toList());

        return vurderingsperioder;
    }

}
