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
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
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
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlagRepository;

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
    private UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository;
    private FeriepengerAvvikTjeneste feriepengerAvvikTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    private EndringAnnenOmsorgspersonUtleder endringAnnenOmsorgspersonUtleder;

    VurderOmPleiepengerVedtakPåvirkerAndreSakerTjeneste() {
    }

    @Inject
    public VurderOmPleiepengerVedtakPåvirkerAndreSakerTjeneste(BehandlingRepository behandlingRepository,
                                                               FagsakRepository fagsakRepository,
                                                               VilkårResultatRepository vilkårResultatRepository,
                                                               MedisinskGrunnlagRepository medisinskGrunnlagRepository,
                                                               UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository,
                                                               FeriepengerAvvikTjeneste feriepengerAvvikTjeneste, @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester, EndringAnnenOmsorgspersonUtleder endringAnnenOmsorgspersonUtleder) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.medisinskGrunnlagRepository = medisinskGrunnlagRepository;
        this.utsattBehandlingAvPeriodeRepository = utsattBehandlingAvPeriodeRepository;
        this.feriepengerAvvikTjeneste = feriepengerAvvikTjeneste;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.endringAnnenOmsorgspersonUtleder = endringAnnenOmsorgspersonUtleder;
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

        var vedtakReferanse = BehandlingReferanse.fra(vedtattBehandling);
        var datoIntervallEntitets = utledVurderingsperiode(vedtakReferanse);
        var trukkedePerioderIVedtaket = utledTrukkedePerioder(vedtakReferanse);
        var vedtattTidslinje = new LocalDateTimeline<>(datoIntervallEntitets.stream()
            .map(datoIntervall -> new LocalDateSegment<>(datoIntervall.getFomDato(), datoIntervall.getTomDato(), true))
            .toList(), StandardCombinators::alwaysTrueForMatch)
            .combine(new LocalDateTimeline<>(trukkedePerioderIVedtaket.stream()
                .map(datoIntervall -> new LocalDateSegment<>(datoIntervall.getFomDato(), datoIntervall.getTomDato(), true))
                .toList(), StandardCombinators::alwaysTrueForMatch), StandardCombinators::alwaysTrueForMatch, LocalDateTimeline.JoinStyle.CROSS_JOIN)
            .compress();

        for (Saksnummer kandidatsaksnummer : alleSaksnummer) {
            if (!kandidatsaksnummer.equals(fagsak.getSaksnummer())) {
                var kandidatFagsak = fagsakRepository.hentSakGittSaksnummer(kandidatsaksnummer, false).orElseThrow();
                var sisteBehandlingPåKandidat = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(kandidatFagsak.getId()).orElseThrow();
                var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, sisteBehandlingPåKandidat.getFagsakYtelseType(), sisteBehandlingPåKandidat.getType());
                var kandidatBehandlingReferanse = BehandlingReferanse.fra(sisteBehandlingPåKandidat);
                var endringstidslinjer = endringAnnenOmsorgspersonUtleder.utledTidslinjerForEndringSomPåvirkerSak(kandidatsaksnummer, pleietrengende, sisteBehandlingPåKandidat, vedtattTidslinje);
                var skalReberegneFeriepenger = perioderMedRevurderingPgaEndringFeriepenger(sisteBehandlingPåKandidat, kandidatBehandlingReferanse);

                if (!endringstidslinjer.harEndringSomPåvirkerSakTidslinje().isEmpty() || !skalReberegneFeriepenger.isEmpty()) {
                    if (!endringstidslinjer.harEndringSomPåvirkerSakTidslinje().isEmpty()) {
                        //Er kun støtte p.t. for å oppgi en årsak pr sak. Må velge 'endring i ytelse'-årsak da denne (i motsetning til feriepenger-årsak) gir opphav til vilkårsperioder i revurderingen. Feriepengene vil uansett blir reberegnet
                        LocalDateTimeline<Boolean> perioderSkalRevurdereYtelse = tettHull(perioderTilVurderingTjeneste, endringstidslinjer.harEndringSomPåvirkerSakTidslinje());
                        resultat.add(new SakMedPeriode(kandidatsaksnummer, kandidatFagsak.getYtelseType(), TidslinjeUtil.tilDatoIntervallEntiteter(perioderSkalRevurdereYtelse), BehandlingÅrsakType.RE_ENDRING_FRA_ANNEN_OMSORGSPERSON));
                    } else {
                        resultat.add(new SakMedPeriode(kandidatsaksnummer, kandidatFagsak.getYtelseType(), TidslinjeUtil.tilDatoIntervallEntiteter(skalReberegneFeriepenger), BehandlingÅrsakType.RE_FERIEPENGER_ENDRING_FRA_ANNEN_SAK));
                    }

                    log.info("Sak='{}' revurderes pga => sykdom={}, etablertTilsyn={}, nattevåk&beredskap={}, uttak={}, feriepenger={}", kandidatsaksnummer, !endringstidslinjer.harEndretSykdomTidslinje().isEmpty(), !endringstidslinjer.harEndretEtablertTilsynTidslinje().isEmpty(), !endringstidslinjer.harEndretNattevåkOgBeredskapTidslinje().isEmpty(), !endringstidslinjer.harEndretUttakTidslinje().isEmpty(), skalReberegneFeriepenger);
                    if (Environment.current().isDev() && !endringstidslinjer.harEndretUttakTidslinje().isEmpty()) {
                        log.info("Endringer i uttak: {}", endringstidslinjer.harEndretUttakTidslinje());
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

    private NavigableSet<DatoIntervallEntitet> utledTrukkedePerioder(BehandlingReferanse referanse) {
        var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, referanse.getFagsakYtelseType(), referanse.getBehandlingType());
        return perioderTilVurderingTjeneste.perioderSomSkalTilbakestilles(referanse.getBehandlingId());
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

    private LocalDateTimeline<Boolean> perioderMedRevurderingPgaEndringFeriepenger(Behandling sisteBehandlingPåKandidat, BehandlingReferanse referanse) {
        if (!sisteBehandlingPåKandidat.getStatus().erFerdigbehandletStatus() && !feriepengerAvvikTjeneste.harKommetTilTilkjentYtelse(referanse)) {
            return LocalDateTimeline.empty();
        }
        Set<Year> opptjeningsårFeriepengerMåReberegnes = feriepengerAvvikTjeneste.opptjeningsårFeirepengerMåReberegnes(referanse);
        return new LocalDateTimeline<>(opptjeningsårFeriepengerMåReberegnes.stream()
            .map(år -> new LocalDateSegment<>(år.atMonth(1).atDay(1), år.atMonth(12).atDay(31), true))
            .toList());
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
