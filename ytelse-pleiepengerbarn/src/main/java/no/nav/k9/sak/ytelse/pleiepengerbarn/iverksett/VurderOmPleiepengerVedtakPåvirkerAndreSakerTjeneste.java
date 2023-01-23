package no.nav.k9.sak.ytelse.pleiepengerbarn.iverksett;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.time.Year;
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
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingModell;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.Hjelpetidslinjer;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.hendelse.vedtak.SakMedPeriode;
import no.nav.k9.sak.hendelse.vedtak.VurderOmVedtakPåvirkerSakerTjeneste;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriode;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriodeRepository;
import no.nav.k9.sak.utsatt.UtsattPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse.feriepenger.FeriepengerAvvikTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.ErEndringPåEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlagTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.EndringUnntakEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.SamtidigUttakTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
public class VurderOmPleiepengerVedtakPåvirkerAndreSakerTjeneste implements VurderOmVedtakPåvirkerSakerTjeneste {

    private static final Logger log = LoggerFactory.getLogger(VurderOmPleiepengerVedtakPåvirkerAndreSakerTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private MedisinskGrunnlagRepository medisinskGrunnlagRepository;
    private MedisinskGrunnlagTjeneste medisinskGrunnlagTjeneste;
    private ErEndringPåEtablertTilsynTjeneste erEndringPåEtablertTilsynTjeneste;
    private EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste;
    private SamtidigUttakTjeneste samtidigUttakTjeneste;
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;
    private BehandlingModellRepository behandlingModellRepository;
    private UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository;
    private FeriepengerAvvikTjeneste feriepengerAvvikTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;
    private boolean enableFeriepengerPåTversAvSaker;

    VurderOmPleiepengerVedtakPåvirkerAndreSakerTjeneste() {
    }

    @Inject
    public VurderOmPleiepengerVedtakPåvirkerAndreSakerTjeneste(BehandlingRepository behandlingRepository,
                                                               FagsakRepository fagsakRepository,
                                                               VilkårResultatRepository vilkårResultatRepository,
                                                               MedisinskGrunnlagRepository medisinskGrunnlagRepository,
                                                               MedisinskGrunnlagTjeneste medisinskGrunnlagTjeneste,
                                                               ErEndringPåEtablertTilsynTjeneste erEndringPåEtablertTilsynTjeneste,
                                                               EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste,
                                                               SamtidigUttakTjeneste samtidigUttakTjeneste,
                                                               SøknadsperiodeTjeneste søknadsperiodeTjeneste,
                                                               BehandlingModellRepository behandlingModellRepository,
                                                               UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository,
                                                               FeriepengerAvvikTjeneste feriepengerAvvikTjeneste, @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                                               @KonfigVerdi(value = "ENABLE_FERIEPENGER_PAA_TVERS_AV_SAKER_OG_PR_AAR", defaultVerdi = "true") boolean enableFeriepengerPåTversAvSaker) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.medisinskGrunnlagRepository = medisinskGrunnlagRepository;
        this.medisinskGrunnlagTjeneste = medisinskGrunnlagTjeneste;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
        this.erEndringPåEtablertTilsynTjeneste = erEndringPåEtablertTilsynTjeneste;
        this.endringUnntakEtablertTilsynTjeneste = endringUnntakEtablertTilsynTjeneste;
        this.samtidigUttakTjeneste = samtidigUttakTjeneste;
        this.behandlingModellRepository = behandlingModellRepository;
        this.utsattBehandlingAvPeriodeRepository = utsattBehandlingAvPeriodeRepository;
        this.feriepengerAvvikTjeneste = feriepengerAvvikTjeneste;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.enableFeriepengerPåTversAvSaker = enableFeriepengerPåTversAvSaker;
    }

    @Override
    public List<SakMedPeriode> utledSakerMedPerioderSomErKanVærePåvirket(Ytelse vedtakHendelse) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(vedtakHendelse.getSaksnummer())).orElseThrow();
        Behandling vedtattBehandling = behandlingRepository.hentBehandling(((YtelseV1) vedtakHendelse).getVedtakReferanse());

        AktørId pleietrengende = vedtattBehandling.getFagsak().getPleietrengendeAktørId();
        List<Saksnummer> alleSaksnummer = medisinskGrunnlagRepository.hentAlleSaksnummer(pleietrengende)
            .stream()
            .filter(it -> ytelsesSpesifiktFilter(fagsak.getYtelseType(), it))
            .toList(); // Denne henter på tvers av saker, og kan trigge
        // Bør her filtrere ut PPN for OLP / PSB
        // Bør her filtrere ut OLP + PSB for PPN

        List<SakMedPeriode> resultat = new ArrayList<>();

        var datoIntervallEntitets = utledVurderingsperiode(BehandlingReferanse.fra(vedtattBehandling));
        var vedtattTidslinje = new LocalDateTimeline<>(datoIntervallEntitets.stream()
            .map(datoIntervall -> new LocalDateSegment<>(datoIntervall.getFomDato(), datoIntervall.getTomDato(), true))
            .toList(), StandardCombinators::alwaysTrueForMatch)
            .compress();

        for (Saksnummer kandidatsaksnummer : alleSaksnummer) {
            if (!kandidatsaksnummer.equals(fagsak.getSaksnummer())) {
                var kandidatFagsak = fagsakRepository.hentSakGittSaksnummer(kandidatsaksnummer, false).orElseThrow();
                var sisteBehandlingPåKandidat = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(kandidatFagsak.getId()).orElseThrow();
                var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, sisteBehandlingPåKandidat.getFagsakYtelseType(), sisteBehandlingPåKandidat.getType());
                var kandidatBehandlingReferanse = BehandlingReferanse.fra(sisteBehandlingPåKandidat);
                var skalRevurderesPgaSykdom = perioderMedRevurderingSykdom(pleietrengende, kandidatsaksnummer, sisteBehandlingPåKandidat);
                var skalRevurderesPgaEtablertTilsyn = perioderMedRevurderingPgaEtablertTilsyn(kandidatBehandlingReferanse);
                var skalRevurderesPgaNattevåkOgBeredskap = perioderMedRevurderesPgaNattevåkOgBeredskap(kandidatBehandlingReferanse);
                var skalRevurderesPgaEndretUttak = perioderMedRevurderingPgaUttak(kandidatBehandlingReferanse);

                var perioderMedInnvilgetInngangsvilkår = periodermedInnvilgetInngangsvilkår(kandidatBehandlingReferanse);

                var skalRevurdereYtelsePgaEndringAnnenSak = skalRevurderesPgaEtablertTilsyn
                    .union(skalRevurderesPgaNattevåkOgBeredskap, StandardCombinators::alwaysTrueForMatch)
                    .union(skalRevurderesPgaEndretUttak, StandardCombinators::alwaysTrueForMatch)
                    .union(skalRevurderesPgaSykdom, StandardCombinators::alwaysTrueForMatch)
                    .intersection(perioderMedInnvilgetInngangsvilkår)
                    .intersection(vedtattTidslinje);
                var skalReberegneFeriepenger = enableFeriepengerPåTversAvSaker ? perioderMedRevurderingPgaEndringFeriepenger(sisteBehandlingPåKandidat, kandidatBehandlingReferanse) : LocalDateTimeline.empty();

                if (!skalRevurdereYtelsePgaEndringAnnenSak.isEmpty() || !skalReberegneFeriepenger.isEmpty()) {
                    if (!skalRevurdereYtelsePgaEndringAnnenSak.isEmpty()) {
                        //Er kun støtte p.t. for å oppgi en årsak pr sak. Må velge 'endring i ytelse'-årsak da denne (i motsetning til feriepenger-årsak) gir opphav til vilkårsperioder i revurderingen. Feriepengene vil uansett blir reberegnet
                        LocalDateTimeline<Boolean> perioderSkalRevurdereYtelse = tettHull(perioderTilVurderingTjeneste, skalRevurdereYtelsePgaEndringAnnenSak);
                        resultat.add(new SakMedPeriode(kandidatsaksnummer, kandidatFagsak.getYtelseType(), TidslinjeUtil.tilDatoIntervallEntiteter(perioderSkalRevurdereYtelse), BehandlingÅrsakType.RE_ENDRING_FRA_ANNEN_OMSORGSPERSON));
                    } else {
                        resultat.add(new SakMedPeriode(kandidatsaksnummer, kandidatFagsak.getYtelseType(), TidslinjeUtil.tilDatoIntervallEntiteter(skalReberegneFeriepenger), BehandlingÅrsakType.RE_FERIEPENGER_ENDRING_FRA_ANNEN_SAK));
                    }
                    if (enableFeriepengerPåTversAvSaker) {
                        log.info("Sak='{}' revurderes pga => sykdom={}, etablertTilsyn={}, nattevåk&beredskap={}, uttak={}, feriepenger={}", kandidatsaksnummer, !skalRevurderesPgaSykdom.isEmpty(), !skalRevurderesPgaEtablertTilsyn.isEmpty(), !skalRevurderesPgaNattevåkOgBeredskap.isEmpty(), !skalRevurderesPgaEndretUttak.isEmpty(), skalReberegneFeriepenger);
                        if (Environment.current().isDev() && !skalRevurderesPgaEndretUttak.isEmpty()) {
                            log.info("Endringer i uttak: {}", skalRevurderesPgaEndretUttak);
                        }
                    } else {
                        log.info("Sak='{}' revurderes pga => sykdom={}, etablertTilsyn={}, nattevåk&beredskap={}, uttak={}", kandidatsaksnummer, !skalRevurderesPgaSykdom.isEmpty(), !skalRevurderesPgaEtablertTilsyn.isEmpty(), !skalRevurderesPgaNattevåkOgBeredskap.isEmpty(), !skalRevurderesPgaEndretUttak.isEmpty());
                    }
                }
            }
        }
        var utsattBehandlingAvPeriode = utsattBehandlingAvPeriodeRepository.hentGrunnlag(vedtattBehandling.getId()).map(UtsattBehandlingAvPeriode::getPerioder).orElse(Set.of());

        if (!utsattBehandlingAvPeriode.isEmpty()) {
            var perioderSomErUtsatt = utsattBehandlingAvPeriode.stream().map(UtsattPeriode::getPeriode).collect(Collectors.toCollection(TreeSet::new));
            resultat.add(new SakMedPeriode(fagsak.getSaksnummer(), fagsak.getYtelseType(), perioderSomErUtsatt, BehandlingÅrsakType.RE_GJENOPPTAR_UTSATT_BEHANDLING));
            log.info("Sak='{}' har utsatte perioder som må behandles", fagsak.getSaksnummer());
        }

        return resultat;
    }

    private LocalDateTimeline<Boolean> periodermedInnvilgetInngangsvilkår(BehandlingReferanse kandidatBehandlingReferanse) {
        LocalDateTimeline<Boolean> result = LocalDateTimeline.empty();

        if (!kandidatBehandlingReferanse.getBehandlingStatus().erFerdigbehandletStatus()) {
            // Ved åpen behandling trenger vi ikke begrense hvilke
            result = TidslinjeUtil.tilTidslinjeKomprimert(utledVurderingsperiode(kandidatBehandlingReferanse));
        }
        var vilkårene = vilkårResultatRepository.hent(kandidatBehandlingReferanse.getBehandlingId());
        var vilkårTidslinjer = vilkårene.getVilkårene()
            .stream()
            .filter(vilkår -> !Objects.equals(vilkår.getVilkårType(), VilkårType.SØKNADSFRIST))
            .map(v -> new LocalDateTimeline<>(v.getPerioder()
                .stream()
                .map(vp -> new LocalDateSegment<>(vp.getPeriode().toLocalDateInterval(), Objects.equals(Utfall.OPPFYLT, vp.getGjeldendeUtfall())))
                .toList()))
            .toList();

        LocalDateTimeline<Boolean> vilkårstidslinje = LocalDateTimeline.empty();

        for (LocalDateTimeline<Boolean> tidslinje : vilkårTidslinjer) {
            vilkårstidslinje = vilkårstidslinje.combine(tidslinje, this::mergeVilkårsSegmenter, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }


        return vilkårstidslinje.combine(result, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN).compress();
    }

    private LocalDateSegment<Boolean> mergeVilkårsSegmenter(LocalDateInterval localDateInterval, LocalDateSegment<Boolean> lhs, LocalDateSegment<Boolean> rhs) {
        if (lhs == null || lhs.getValue() == null) {
            return rhs;
        }
        if (rhs == null || rhs.getValue() == null) {
            return lhs;
        }
        if (!rhs.getValue()) {
            return new LocalDateSegment<>(localDateInterval, rhs.getValue());
        }
        return new LocalDateSegment<>(localDateInterval, lhs.getValue());
    }

    private boolean ytelsesSpesifiktFilter(FagsakYtelseType ytelseType, Saksnummer kandidatsaksnummer) {
        if (Set.of(OPPLÆRINGSPENGER, PLEIEPENGER_SYKT_BARN).contains(ytelseType)) {
            var fagsak = fagsakRepository.hentSakGittSaksnummer(kandidatsaksnummer, false).orElseThrow();

            return Set.of(OPPLÆRINGSPENGER, PLEIEPENGER_SYKT_BARN).contains(fagsak.getYtelseType());
        } else if (Objects.equals(PLEIEPENGER_NÆRSTÅENDE, ytelseType)) {
            var fagsak = fagsakRepository.hentSakGittSaksnummer(kandidatsaksnummer, false).orElseThrow();

            return Objects.equals(PLEIEPENGER_NÆRSTÅENDE, fagsak.getYtelseType());
        }
        return true;
    }

    private LocalDateTimeline<Boolean> tettHull(VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste, LocalDateTimeline<Boolean> skalRevurderes) {
        var kantIKantVurderer = perioderTilVurderingTjeneste.getKantIKantVurderer();
        var tidslinjeHull = Hjelpetidslinjer.utledHullSomMåTettes(skalRevurderes, kantIKantVurderer);
        return skalRevurderes.crossJoin(tidslinjeHull, StandardCombinators::coalesceRightHandSide);
    }

    private LocalDateTimeline<Boolean> perioderMedRevurderingPgaUttak(BehandlingReferanse referanse) {
        if (!referanse.getBehandlingStatus().erFerdigbehandletStatus() && !samtidigUttakTjeneste.harKommetTilUttak(referanse)) {
            return LocalDateTimeline.empty();
        }
        return TidslinjeUtil.tilTidslinjeKomprimert(samtidigUttakTjeneste.perioderMedEndringerMedUbesluttedeData(referanse));
    }

    private LocalDateTimeline<Boolean> perioderMedRevurderingPgaEndringFeriepenger(Behandling sisteBehandlingPåKandidat, BehandlingReferanse referanse) {
        if (!sisteBehandlingPåKandidat.getStatus().erFerdigbehandletStatus() && !feriepengerAvvikTjeneste.harKommetTilTilkjentYtelse(referanse)) {
            return LocalDateTimeline.empty();
        }
        Set<Year> opptjeningsårFeriepengerMåReberegnes = feriepengerAvvikTjeneste.opptjeningsårFeirepengerMåReberegnes(referanse);
        return new LocalDateTimeline<>(opptjeningsårFeriepengerMåReberegnes.stream()
            .map(år -> new LocalDateSegment<>(år.atMonth(1).atDay(1), år.atMonth(12).atDay(31), true))
            .toList());
    }

    private LocalDateTimeline<Boolean> perioderMedRevurderesPgaNattevåkOgBeredskap(BehandlingReferanse referanse) {
        return TidslinjeUtil.tilTidslinjeKomprimert(endringUnntakEtablertTilsynTjeneste.utledRelevanteEndringerSidenBehandling(referanse.getBehandlingId(), referanse.getPleietrengendeAktørId()));
    }

    private LocalDateTimeline<Boolean> perioderMedRevurderingPgaEtablertTilsyn(BehandlingReferanse referanse) {
        return erEndringPåEtablertTilsynTjeneste.perioderMedEndringerFraEksisterendeVersjon(referanse)
            .filterValue(Objects::nonNull);
    }

    private LocalDateTimeline<Boolean> perioderMedRevurderingSykdom(AktørId pleietrengende, Saksnummer kandidatsaksnummer, Behandling sisteBehandlingPåKandidat) {
        if (søknadsperiodeTjeneste.utledFullstendigPeriode(sisteBehandlingPåKandidat.getId()).isEmpty()) {
            return LocalDateTimeline.empty();
        }
        if (erUnderBehandlingOgIkkeKommetTilSykdom(sisteBehandlingPåKandidat)) {
            return LocalDateTimeline.empty();
        }
        var kandidatSykdomBehandling = medisinskGrunnlagRepository.hentGrunnlagForBehandling(sisteBehandlingPåKandidat.getUuid()).orElseThrow();
        var behandling = behandlingRepository.hentBehandlingHvisFinnes(kandidatSykdomBehandling.getBehandlingUuid()).orElseThrow();
        var vurderingsperioder = utledVurderingsperiode(BehandlingReferanse.fra(behandling));
        var manglendeOmsorgenForPerioder = medisinskGrunnlagTjeneste.hentManglendeOmsorgenForPerioder(behandling.getId());
        var utledetGrunnlag = medisinskGrunnlagRepository.utledGrunnlag(kandidatsaksnummer, kandidatSykdomBehandling.getBehandlingUuid(), pleietrengende, vurderingsperioder, manglendeOmsorgenForPerioder);
        final LocalDateTimeline<Boolean> endringerISøktePerioder = medisinskGrunnlagTjeneste.sammenlignGrunnlag(Optional.of(kandidatSykdomBehandling.getGrunnlagsdata()), utledetGrunnlag).getDiffPerioder();

        return endringerISøktePerioder.compress()
            .filterValue(Objects::nonNull);
    }

    private boolean erUnderBehandlingOgIkkeKommetTilSykdom(Behandling sisteBehandlingPåKandidat) {
        return !sisteBehandlingPåKandidat.erSaksbehandlingAvsluttet() && harIkkeKommetTilSykdom(sisteBehandlingPåKandidat);
    }

    private boolean harIkkeKommetTilSykdom(Behandling sisteBehandlingPåKandidat) {
        final BehandlingStegType steg = sisteBehandlingPåKandidat.getAktivtBehandlingSteg();
        final BehandlingModell modell = behandlingModellRepository.getModell(sisteBehandlingPåKandidat.getType(), sisteBehandlingPåKandidat.getFagsakYtelseType());
        return modell.erStegAFørStegB(steg, BehandlingStegType.VURDER_MEDISINSKE_VILKÅR);
    }

    private NavigableSet<DatoIntervallEntitet> utledVurderingsperiode(BehandlingReferanse referanse) {
        var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, referanse.getFagsakYtelseType(), referanse.getBehandlingType());
        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());

        return perioderTilVurderingTjeneste.definerendeVilkår()
            .stream()
            .map(vilkårType -> vilkårene.getVilkår(vilkårType)
                .map(Vilkår::getPerioder))
            .flatMap(Optional::stream)
            .flatMap(Collection::stream)
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new));
    }

}
