package no.nav.k9.sak.ytelse.pleiepengerbarn.iverksett;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingModell;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.hendelse.vedtak.SakMedPeriode;
import no.nav.k9.sak.hendelse.vedtak.VurderOmVedtakPåvirkerSakerTjeneste;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriode;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriodeRepository;
import no.nav.k9.sak.utsatt.UtsattPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.ErEndringPåEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagService;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.EndringUnntakEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.SamtidigUttakTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
public class VurderOmPleiepengerVedtakPåvirkerAndreSakerTjeneste implements VurderOmVedtakPåvirkerSakerTjeneste {

    private static final Logger log = LoggerFactory.getLogger(VurderOmPleiepengerVedtakPåvirkerAndreSakerTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private SykdomGrunnlagRepository sykdomGrunnlagRepository;
    private SykdomVurderingRepository sykdomVurderingRepository;
    private SykdomGrunnlagService sykdomGrunnlagService;
    private ErEndringPåEtablertTilsynTjeneste erEndringPåEtablertTilsynTjeneste;
    private EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste;
    private SamtidigUttakTjeneste samtidigUttakTjeneste;
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;
    private BehandlingModellRepository behandlingModellRepository;
    private UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;
    private Boolean aktivertUtsattBehandlingAvPeriode;

    VurderOmPleiepengerVedtakPåvirkerAndreSakerTjeneste() {
    }

    @Inject
    public VurderOmPleiepengerVedtakPåvirkerAndreSakerTjeneste(BehandlingRepository behandlingRepository,
                                                               FagsakRepository fagsakRepository,
                                                               VilkårResultatRepository vilkårResultatRepository,
                                                               SykdomGrunnlagRepository sykdomGrunnlagRepository,
                                                               SykdomVurderingRepository sykdomVurderingRepository,
                                                               SykdomGrunnlagService sykdomGrunnlagService,
                                                               ErEndringPåEtablertTilsynTjeneste erEndringPåEtablertTilsynTjeneste,
                                                               EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste,
                                                               SamtidigUttakTjeneste samtidigUttakTjeneste,
                                                               SøknadsperiodeTjeneste søknadsperiodeTjeneste,
                                                               BehandlingModellRepository behandlingModellRepository,
                                                               UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository,
                                                               @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                                               @KonfigVerdi(value = "utsatt.behandling.av.periode.aktivert", defaultVerdi = "false") Boolean aktivertUtsattBehandlingAvPeriode) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
        this.sykdomVurderingRepository = sykdomVurderingRepository;
        this.sykdomGrunnlagService = sykdomGrunnlagService;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
        this.erEndringPåEtablertTilsynTjeneste = erEndringPåEtablertTilsynTjeneste;
        this.endringUnntakEtablertTilsynTjeneste = endringUnntakEtablertTilsynTjeneste;
        this.samtidigUttakTjeneste = samtidigUttakTjeneste;
        this.behandlingModellRepository = behandlingModellRepository;
        this.utsattBehandlingAvPeriodeRepository = utsattBehandlingAvPeriodeRepository;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.aktivertUtsattBehandlingAvPeriode = aktivertUtsattBehandlingAvPeriode;
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
                var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, sisteBehandlingPåKandidat.getFagsakYtelseType(), sisteBehandlingPåKandidat.getType());
                var skalRevurderesPgaSykdom = perioderMedRevurderingSykdom(pleietrengende, kandidatsaksnummer, sisteBehandlingPåKandidat);
                var referanse = BehandlingReferanse.fra(sisteBehandlingPåKandidat);
                var skalRevurderesPgaEtablertTilsyn = perioderMedRevurderingPgaEtablertTilsyn(referanse);
                var skalRevurderesPgaNattevåkOgBeredskap = perioderMedRevurderesPgaNattevåkOgBeredskap(referanse);
                var skalRevurderesPgaEndretUttak = perioderMedRevurderingPgaUttak(sisteBehandlingPåKandidat, referanse);

                if (!skalRevurderesPgaSykdom.isEmpty() || !skalRevurderesPgaEtablertTilsyn.isEmpty() || !skalRevurderesPgaNattevåkOgBeredskap.isEmpty() || !skalRevurderesPgaEndretUttak.isEmpty()) {
                    NavigableSet<DatoIntervallEntitet> perioderMedEndring = utledPerioder(perioderTilVurderingTjeneste, skalRevurderesPgaSykdom, skalRevurderesPgaEtablertTilsyn, skalRevurderesPgaNattevåkOgBeredskap, skalRevurderesPgaEndretUttak);
                    result.add(new SakMedPeriode(kandidatsaksnummer, perioderMedEndring));
                    log.info("Sak='{}' revurderes pga => sykdom={}, etablertTilsyn={}, nattevåk&beredskap={}, uttak={}", kandidatsaksnummer, !skalRevurderesPgaSykdom.isEmpty(), !skalRevurderesPgaEtablertTilsyn.isEmpty(), !skalRevurderesPgaNattevåkOgBeredskap.isEmpty(), !skalRevurderesPgaEndretUttak.isEmpty());
                }
            }
        }
        var utsattBehandlingAvPeriode = utsattBehandlingAvPeriodeRepository.hentGrunnlag(vedtattBehandling.getId()).map(UtsattBehandlingAvPeriode::getPerioder).orElse(Set.of());

        if (aktivertUtsattBehandlingAvPeriode && !utsattBehandlingAvPeriode.isEmpty()) {
            var perioderSomErUtsatt = utsattBehandlingAvPeriode.stream().map(UtsattPeriode::getPeriode).collect(Collectors.toCollection(TreeSet::new));
            result.add(new SakMedPeriode(fagsak.getSaksnummer(), perioderSomErUtsatt));
            log.info("Sak='{}' har utsatte perioder som må behandles", fagsak.getSaksnummer());
        }

        return result;
    }

    private NavigableSet<DatoIntervallEntitet> utledPerioder(VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                                             NavigableSet<DatoIntervallEntitet> skalRevurderesPgaSykdom,
                                                             NavigableSet<DatoIntervallEntitet> skalRevurderesPgaEtablertTilsyn,
                                                             NavigableSet<DatoIntervallEntitet> skalRevurderesPgaNattevåkOgBeredskap,
                                                             NavigableSet<DatoIntervallEntitet> skalRevurderesPgaEndretUttak) {
        var perioderMedEndring = new TreeSet<>(skalRevurderesPgaEtablertTilsyn);
        perioderMedEndring.addAll(skalRevurderesPgaNattevåkOgBeredskap);
        perioderMedEndring.addAll(skalRevurderesPgaSykdom);
        perioderMedEndring.addAll(skalRevurderesPgaEndretUttak);

        List<LocalDateSegment<Boolean>> segmenter = perioderMedEndring.stream()
            .map(periode -> new LocalDateSegment<>(periode.toLocalDateInterval(), true))
            .toList();
        var tidslinje = new LocalDateTimeline<>(segmenter, StandardCombinators::coalesceRightHandSide);

        // tett hull
        var kantIKantVurderer = perioderTilVurderingTjeneste.getKantIKantVurderer();
        var segmenterSomMangler = utledHullSomMåTettes(tidslinje, kantIKantVurderer);
        var tidslinjeHull = new LocalDateTimeline<>(segmenterSomMangler, StandardCombinators::coalesceRightHandSide);
        tidslinje = tidslinje.combine(tidslinjeHull, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        return TidslinjeUtil.tilDatoIntervallEntiteter(tidslinje.compress());
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

    private NavigableSet<DatoIntervallEntitet> perioderMedRevurderingPgaUttak(Behandling sisteBehandlingPåKandidat, BehandlingReferanse referanse) {
        if (!sisteBehandlingPåKandidat.getStatus().erFerdigbehandletStatus() && !samtidigUttakTjeneste.harKommetTilUttak(referanse)) {
            return new TreeSet<>();
        }
        return samtidigUttakTjeneste.perioderMedEndringerMedUbesluttedeData(referanse);
    }

    private NavigableSet<DatoIntervallEntitet> perioderMedRevurderesPgaNattevåkOgBeredskap(BehandlingReferanse referanse) {
        return endringUnntakEtablertTilsynTjeneste.utledRelevanteEndringerSidenBehandling(referanse.getBehandlingId(), referanse.getPleietrengendeAktørId());
    }

    private NavigableSet<DatoIntervallEntitet> perioderMedRevurderingPgaEtablertTilsyn(BehandlingReferanse referanse) {
        return TidslinjeUtil.tilDatoIntervallEntiteter(erEndringPåEtablertTilsynTjeneste.perioderMedEndringerFraEksisterendeVersjon(referanse).filterValue(Objects::nonNull));
    }

    private NavigableSet<DatoIntervallEntitet> perioderMedRevurderingSykdom(AktørId pleietrengende, Saksnummer kandidatsaksnummer, Behandling sisteBehandlingPåKandidat) {
        if (søknadsperiodeTjeneste.utledFullstendigPeriode(sisteBehandlingPåKandidat.getId()).isEmpty()) {
            return new TreeSet<>();
        }
        if (erUnderBehandlingOgIkkeKommetTilSykdom(sisteBehandlingPåKandidat)) {
            return new TreeSet<>();
        }
        var kandidatSykdomBehandling = sykdomGrunnlagRepository.hentGrunnlagForBehandling(sisteBehandlingPåKandidat.getUuid()).orElseThrow();
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

    private boolean erUnderBehandlingOgIkkeKommetTilSykdom(Behandling sisteBehandlingPåKandidat) {
        return !sisteBehandlingPåKandidat.erSaksbehandlingAvsluttet() && harIkkeKommetTilSykdom(sisteBehandlingPåKandidat);
    }

    private boolean harIkkeKommetTilSykdom(Behandling sisteBehandlingPåKandidat) {
        final BehandlingStegType steg = sisteBehandlingPåKandidat.getAktivtBehandlingSteg();
        final BehandlingModell modell = behandlingModellRepository.getModell(sisteBehandlingPåKandidat.getType(), sisteBehandlingPåKandidat.getFagsakYtelseType());
        return modell.erStegAFørStegB(steg, BehandlingStegType.VURDER_MEDISINSKE_VILKÅR);
    }

    private List<Periode> utledVurderingsperiode(Behandling behandling) {
        var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType());
        var vilkårene = vilkårResultatRepository.hent(behandling.getId());

        return perioderTilVurderingTjeneste.definerendeVilkår()
            .stream()
            .map(vilkårType -> vilkårene.getVilkår(vilkårType)
                .map(Vilkår::getPerioder))
            .flatMap(Optional::stream)
            .flatMap(Collection::stream)
            .map(VilkårPeriode::getPeriode)
            .map(it -> new Periode(it.getFomDato(), it.getTomDato()))
            .toList();
    }

}
