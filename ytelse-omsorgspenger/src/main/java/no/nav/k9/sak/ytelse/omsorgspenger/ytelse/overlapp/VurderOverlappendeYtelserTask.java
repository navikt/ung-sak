package no.nav.k9.sak.ytelse.omsorgspenger.ytelse.overlapp;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.aarskvantum.kontrakter.Aktivitet;
import no.nav.k9.aarskvantum.kontrakter.Utfall;
import no.nav.k9.aarskvantum.kontrakter.Uttaksperiode;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.Etterkontroll;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.KontrollType;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktørYtelse;
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(VurderOverlappendeYtelserTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class VurderOverlappendeYtelserTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "iverksetteVedtak.vurderOverlappendeYtelser";
    private static final Logger logger = LoggerFactory.getLogger(VurderOverlappendeYtelserTask.class);
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BehandlingRepository behandlingRepository;
    private EtterkontrollRepository etterkontrollRepository;
    private ÅrskvantumTjeneste årskvantumTjeneste;

    VurderOverlappendeYtelserTask() {
        // CDI
    }

    @Inject
    public VurderOverlappendeYtelserTask(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                         BehandlingRepository behandlingRepository,
                                         EtterkontrollRepository etterkontrollRepository,
                                         ÅrskvantumTjeneste årskvantumTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.etterkontrollRepository = etterkontrollRepository;
        this.årskvantumTjeneste = årskvantumTjeneste;
    }


    boolean harOverlappendeYtelse(LocalDateTimeline<Boolean> vilkårTimeline, AktørYtelse aktørYtelse, UUID uuid) {
        Map<FagsakYtelseType, NavigableSet<LocalDateInterval>> overlapp = new TreeMap<>();
        if (!vilkårTimeline.isEmpty()) {

            var ytelseFilter = new YtelseFilter(aktørYtelse).filter(yt -> !FagsakYtelseType.OMSORGSPENGER.equals(yt.getYtelseType())); // ser bort fra omsorgspenger
            for (var yt : ytelseFilter.getFiltrertYtelser()) {
                var ytp = yt.getPeriode();
                var overlappPeriode = innvilgelseOverlapperMedAnnenYtelse(vilkårTimeline, ytp);
                if (!overlappPeriode.isEmpty()) {
                    if (yt.getYtelseAnvist().isEmpty()) {
                        // er under behandling. flagger hele perioden med overlapp
                        overlapp.put(yt.getYtelseType(), overlappPeriode);
                    } else {
                        var anvistSegmenter = yt.getYtelseAnvist().stream()
                            .map(ya -> new LocalDateSegment<>(ya.getAnvistFOM(), ya.getAnvistTOM(), Boolean.TRUE))
                            .sorted()
                            .collect(Collectors.toCollection(LinkedHashSet::new));

                        var anvistTimeline = new LocalDateTimeline<>(anvistSegmenter, StandardCombinators::alwaysTrueForMatch);
                        var intersection = anvistTimeline.intersection(vilkårTimeline);
                        if (!intersection.isEmpty()) {
                            overlapp.put(yt.getYtelseType(), intersection.getDatoIntervaller());
                        }
                    }
                }
            }
        }

        var overlappendeYtelser = !overlapp.isEmpty();
        if (overlappendeYtelser) {
            logger.info("Behandling '{}' har overlappende ytelser '{}'", uuid, overlapp.keySet());
        }
        return overlappendeYtelser;
    }

    private NavigableSet<LocalDateInterval> innvilgelseOverlapperMedAnnenYtelse(LocalDateTimeline<Boolean> vilkårPeriode, DatoIntervallEntitet ytp) {
        return vilkårPeriode.getDatoIntervaller()
            .stream()
            .map(it -> it.overlap(new LocalDateInterval(ytp.getFomDato(), ytp.getTomDato())))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Long behandlingId = Long.valueOf(prosessTaskData.getBehandlingId());
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        AktørId aktørId = new AktørId(prosessTaskData.getAktørId());
        var aktørYtelse = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId)
            .getAktørYtelseFraRegister(aktørId);

        var vilkårPerioder = årskvantumTjeneste.hentFullUttaksplan(behandling.getFagsak().getSaksnummer())
            .getAktiviteter()
            .stream()
            .map(Aktivitet::getUttaksperioder)
            .flatMap(Collection::stream)
            .filter(it -> it.getUtfall().equals(Utfall.INNVILGET))
            .map(Uttaksperiode::getPeriode)
            .map(v -> new LocalDateSegment<>(v.getFom(), v.getTom(), Boolean.TRUE))
            .collect(Collectors.toCollection(TreeSet::new));

        if (aktørYtelse.isPresent() && !vilkårPerioder.isEmpty()) {
            var innvilgetTimeline = new LocalDateTimeline<Boolean>(List.of());
            for (LocalDateSegment<Boolean> periode : vilkårPerioder) {
                innvilgetTimeline = innvilgetTimeline.combine(new LocalDateTimeline<>(List.of(periode)), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
            }
            innvilgetTimeline = innvilgetTimeline.compress();
            var harOverlappendeYtelse = harOverlappendeYtelse(innvilgetTimeline, aktørYtelse.get(), behandling.getUuid());

            var erIkkeMarkertFraFør = etterkontrollRepository.finnEtterkontrollForFagsak(behandling.getFagsakId(), KontrollType.OVERLAPPENDE_YTELSE).isEmpty();
            if (harOverlappendeYtelse && erIkkeMarkertFraFør) {
                etterkontrollRepository.lagre(new Etterkontroll.Builder(prosessTaskData.getFagsakId())
                    .medErBehandlet(true)
                    .medKontrollTidspunkt(LocalDateTime.now())
                    .medKontrollType(KontrollType.OVERLAPPENDE_YTELSE)
                    .build());
            }
        }
    }
}
