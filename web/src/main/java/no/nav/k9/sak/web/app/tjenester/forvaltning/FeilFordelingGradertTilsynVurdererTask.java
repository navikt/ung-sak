package no.nav.k9.sak.web.app.tjenester.forvaltning;

import static no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1.FraKalkulusMapper.mapArbeidsgiver;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusRestKlient;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PleiepengerSyktBarnGrunnlag;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.kodeverk.YtelseTyperKalkulusStøtterKontrakt;
import no.nav.folketrygdloven.kalkulus.request.v1.forvaltning.OppdaterYtelsesspesifiktGrunnlagForRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.forvaltning.OppdaterYtelsesspesifiktGrunnlagListeRequest;
import no.nav.folketrygdloven.kalkulus.response.v1.forvaltning.EndretPeriodeListeRespons;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.utbetalingdiff.DataDumpGrunnlag;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.utbetalingdiff.DumpSimulertUtbetalingDiff;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.utbetalingdiff.DumpSimulertUtbetalingDiffAndel;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.utbetalingdiff.DumpSimulertUtbetalingDiffPeriode;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.PleiepengerOgOpplæringspengerGrunnlagMapper;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.UttakRestKlient;
import no.nav.k9.sak.økonomi.tilkjentytelse.JsonMapper;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utbetalingsgrader;
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

@ApplicationScoped
@ProsessTask(FeilFordelingGradertTilsynVurdererTask.TASKTYPE)
public class FeilFordelingGradertTilsynVurdererTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "feil.fordeling.tilsyn.kandidatutleder.task";
    public static final String PERIODE_PROP_NAME = "periode";
    private EntityManager entityManager;

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;

    private UttakRestKlient uttakRestKlient;
    private VilkårResultatRepository vilkårResultatRepository;

    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;

    private KalkulusRestKlient kalkulusRestKlient;


    FeilFordelingGradertTilsynVurdererTask() {
        //for cdi proxy
    }

    @Inject
    public FeilFordelingGradertTilsynVurdererTask(FagsakRepository fagsakRepository,
                                                  EntityManager entityManager,
                                                  BehandlingRepository behandlingRepository,
                                                  BeregningsresultatRepository beregningsresultatRepository,
                                                  UttakRestKlient uttakRestKlient,
                                                  VilkårResultatRepository vilkårResultatRepository,
                                                  BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                                  KalkulusRestKlient kalkulusRestKlient) {
        this.entityManager = entityManager;
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.uttakRestKlient = uttakRestKlient;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.kalkulusRestKlient = kalkulusRestKlient;
    }

    @Override
    public void doTask(ProsessTaskData pd) {
        String periode = pd.getPropertyValue(PERIODE_PROP_NAME);
        var fagsakIdSaksnummerMap = hentFagsakIdOgSaksnummer(new Periode(periode));


        List<DataDumpGrunnlag> resultater = new ArrayList<>();

        for (var fagsakIdOgSaksnummerEntry : fagsakIdSaksnummerMap.entrySet()) {
            var fagsakId = fagsakIdOgSaksnummerEntry.getKey();
            var sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId).orElseThrow();
            var beregningsresultatEntitet = beregningsresultatRepository.hentEndeligBeregningsresultat(sisteBehandling.getId());
            var uttaksplan = uttakRestKlient.hentUttaksplan(sisteBehandling.getUuid(), true);
            var perioderMedForventetEndring = finnPerioderMedForventetEndring(beregningsresultatEntitet, uttaksplan);
            if (!perioderMedForventetEndring.isEmpty()) {
                var dataDumpGrunnlag = kallKalkulusOgMapDiff(sisteBehandling, uttaksplan, perioderMedForventetEndring);
                resultater.add(dataDumpGrunnlag);
            }
        }

        resultater.forEach(entityManager::persist);

    }

    private DataDumpGrunnlag kallKalkulusOgMapDiff(Behandling sisteBehandling, Uttaksplan uttaksplan, List<DatoIntervallEntitet> perioderMedForventetEndring) {
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(sisteBehandling.getId());

        var vilkårsperioder = vilkårene.flatMap(it -> it.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .stream()
            .flatMap(it -> it.getPerioder().stream())
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toSet());

        var vilkårsperioderSomMåSjekkes = vilkårsperioder.stream().filter(it -> perioderMedForventetEndring.stream().anyMatch(p -> p.overlapper(it))).collect(Collectors.toSet());

        var beregningsgrunnlagPerioderGrunnlag = beregningPerioderGrunnlagRepository.hentGrunnlag(sisteBehandling.getId());

        var bgReferanserSomMåSjekkes = beregningsgrunnlagPerioderGrunnlag
            .stream().flatMap(it -> it.getGrunnlagPerioder().stream())
            .filter(it -> vilkårsperioderSomMåSjekkes.stream().anyMatch(p -> p.getFomDato().equals(it.getSkjæringstidspunkt())))
            .collect(Collectors.toSet());

        var requesterPrReferanse = bgReferanserSomMåSjekkes.stream().map(ref -> {
            var vilkårsperiode = vilkårsperioderSomMåSjekkes.stream().filter(p -> p.getFomDato().equals(ref.getSkjæringstidspunkt())).findFirst().orElseThrow();
            var perioderMedEndring = perioderMedForventetEndring.stream().filter(p -> p.overlapper(vilkårsperiode)).toList();
            return new OppdaterYtelsesspesifiktGrunnlagForRequest(ref.getEksternReferanse(),
                new PleiepengerSyktBarnGrunnlag(PleiepengerOgOpplæringspengerGrunnlagMapper.finnUtbetalingsgrader(perioderMedEndring, uttaksplan), null));
        }).toList();

        var request = new OppdaterYtelsesspesifiktGrunnlagListeRequest(sisteBehandling.getFagsak().getSaksnummer().getVerdi(),
            sisteBehandling.getUuid(),
            new AktørIdPersonident(sisteBehandling.getAktørId().getAktørId()),
            YtelseTyperKalkulusStøtterKontrakt.PLEIEPENGER_SYKT_BARN,
            requesterPrReferanse);


        var endretPeriodeListeRespons = kalkulusRestKlient.simulerFastsettMedOppdatertUttak(request);
        var mappetSimulertDiff = mapKalkulusRespons(endretPeriodeListeRespons, request);
        return new DataDumpGrunnlag(sisteBehandling.getId(), mappetSimulertDiff);
    }

    private static List<DumpSimulertUtbetalingDiff> mapKalkulusRespons(EndretPeriodeListeRespons endretPeriodeListeRespons, OppdaterYtelsesspesifiktGrunnlagListeRequest request) {
        return endretPeriodeListeRespons.getPerioderPrReferanse().stream().map(r ->
            new DumpSimulertUtbetalingDiff(
                r.getEksternReferanse(),
                finnRequestJson(request, r.getEksternReferanse()), r.getPeriodedifferanser().stream().map(p ->
                new DumpSimulertUtbetalingDiffPeriode(
                    DatoIntervallEntitet.fraOgMedTilOgMed(p.getPeriode().getFom(), p.getPeriode().getTom()),
                    p.getAndeldifferanser().stream().map(a -> new DumpSimulertUtbetalingDiffAndel(
                        mapArbeidsgiver(a.getArbeidsgiver()),
                        a.getGammelDagsats().intValue(),
                        a.getNyDagsats().intValue(),
                        a.getGammelDagsatsBruker().intValue(),
                        a.getNyDagsatsBruker().intValue(),
                        a.getGammelDagsatsArbeidsgiver().intValue(),
                        a.getNyDagsatsArbeidsgiver().intValue()
                    )).toList()
                )).toList()
            )).toList();
    }

    private static String finnRequestJson(OppdaterYtelsesspesifiktGrunnlagListeRequest request, UUID eksternReferanse) {
        return request.getYtelsespesifiktGrunnlagListe().stream().filter(it -> it.getEksternReferanse().equals(eksternReferanse))
            .map(OppdaterYtelsesspesifiktGrunnlagForRequest::getYtelsespesifiktGrunnlag)
            .map(it -> {
                try {
                    return JsonMapper.getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(it);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }).findFirst().orElseThrow(() -> new IllegalStateException("Forventer å finne request for response for ekstern referanse " + eksternReferanse));
    }

    private List<DatoIntervallEntitet> finnPerioderMedForventetEndring(Optional<BeregningsresultatEntitet> beregningsresultatEntitet, Uttaksplan uttaksplan) {

        if (uttaksplan == null) {
            return Collections.emptyList();
        }

        var perioderMedUtbetalingTilArbeidsgiver = beregningsresultatEntitet.stream().flatMap(it -> it.getBeregningsresultatPerioder().stream())
            .filter(p -> p.getBeregningsresultatAndelList().stream().anyMatch(a -> !a.erBrukerMottaker() && a.getDagsats() > 0))
            .filter(p -> harMerEnnEnArbeidsgiverMedRefusjon(p.getBeregningsresultatAndelList()) || harUtbetalingTilBådeArbeidsgiverOgBruker(p.getBeregningsresultatAndelList()))
            .toList();

        if (perioderMedUtbetalingTilArbeidsgiver.isEmpty()) {
            return Collections.emptyList();
        }


        var perioderMedDiff = uttaksplan.getPerioder()
            .entrySet()
            .stream()
            .map(e -> new Uttaksperiode(
                DatoIntervallEntitet.fraOgMedTilOgMed(e.getKey().getFom(), e.getKey().getTom()),
                lagArbeidsforholdMedDiffListe(e.getValue())))
            .filter(p -> !p.arbeidsforholdMedDiff().isEmpty())
            .toList();

        if (perioderMedDiff.isEmpty()) {
            return Collections.emptyList();
        }

        var utbetalingTidslinje = new LocalDateTimeline<>(lagSegmenterMedUtbetalingTilArbeidsgiver(perioderMedUtbetalingTilArbeidsgiver));
        var uttakTidslinje = new LocalDateTimeline<>(perioderMedDiff.stream().map(it -> new LocalDateSegment<>(it.periode().getFomDato(), it.periode().getTomDato(), it.arbeidsforholdMedDiff())).toList());
        var påvirketTidslinje = uttakTidslinje.intersection(utbetalingTidslinje, StandardCombinators::alwaysTrueForMatch);
        return påvirketTidslinje.toSegments().stream().map(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it.getFom(), it.getTom())).toList();
    }

    private static boolean harUtbetalingTilBådeArbeidsgiverOgBruker(List<BeregningsresultatAndel> beregningsresultatAndelList) {
        return beregningsresultatAndelList.stream().anyMatch(a -> !a.erBrukerMottaker() && a.getDagsats() > 0) &&
            beregningsresultatAndelList.stream().anyMatch(a -> a.erBrukerMottaker() && a.getDagsats() > 0);
    }

    private static boolean harMerEnnEnArbeidsgiverMedRefusjon(List<BeregningsresultatAndel> beregningsresultatAndelList) {
        return beregningsresultatAndelList.stream().filter(a -> a.getArbeidsgiver().isPresent() && !a.erBrukerMottaker() && a.getDagsats() > 0)
            .map(BeregningsresultatAndel::getArbeidsgiver)
            .map(Optional::get)
            .distinct()
            .count() > 1;
    }

    private static List<LocalDateSegment<List<BeregningsresultatAndel>>> lagSegmenterMedUtbetalingTilArbeidsgiver(List<BeregningsresultatPeriode> perioderMedUtbetalingTilArbeidsgiver) {
        return perioderMedUtbetalingTilArbeidsgiver.stream().map(
                it -> new LocalDateSegment<>(it.getBeregningsresultatPeriodeFom(), it.getBeregningsresultatPeriodeTom(),
                    it.getBeregningsresultatAndelList()))
            .toList();
    }

    private List<Arbeidsforhold> lagArbeidsforholdMedDiffListe(UttaksperiodeInfo plan) {
        return plan.getUtbetalingsgrader()
            .stream()
            .filter(a -> a.getArbeidsforhold().getOrganisasjonsnummer() != null)
            .map(a -> new Arbeidsforhold(a.getArbeidsforhold().getOrganisasjonsnummer(),
                hentAktivitetsgrad(a), a.getUtbetalingsgrad()))
            .filter(a -> a.aktivitetsgrad().compareTo(a.utbetalingsgrad()) != 0)
            .toList();
    }

    private BigDecimal hentAktivitetsgrad(Utbetalingsgrader utbetalingsgrader) {
        if (utbetalingsgrader.getNormalArbeidstid().isZero()) {
            return new BigDecimal(100).subtract(utbetalingsgrader.getUtbetalingsgrad());
        }

        final Duration faktiskArbeidstid;
        if (utbetalingsgrader.getFaktiskArbeidstid() != null) {
            faktiskArbeidstid = utbetalingsgrader.getFaktiskArbeidstid();
        } else {
            faktiskArbeidstid = Duration.ofHours(0L);
        }

        final BigDecimal HUNDRE_PROSENT = new BigDecimal(100);


        final BigDecimal aktivitetsgrad = new BigDecimal(faktiskArbeidstid.toMillis()).setScale(2, RoundingMode.HALF_DOWN)
            .divide(new BigDecimal(utbetalingsgrader.getNormalArbeidstid().toMillis()), 2, RoundingMode.HALF_DOWN)
            .multiply(HUNDRE_PROSENT);

        if (aktivitetsgrad.compareTo(HUNDRE_PROSENT) >= 0) {
            return HUNDRE_PROSENT;
        }

        return aktivitetsgrad;
    }

    public Map<Long, Saksnummer> hentFagsakIdOgSaksnummer(Periode periode) {
        Query query;

        String sql = """
            select f.id, f.saksnummer from Fagsak f
             where f.ytelse_type = :ytelseType
               and upper(f.periode) > :fom
               and lower(f.periode) < :tom)
              """;

        query = entityManager.createNativeQuery(sql); // NOSONAR

        query.setParameter("ytelseType", FagsakYtelseType.PLEIEPENGER_SYKT_BARN.getKode());
        query.setParameter("fom", periode.getFom());
        query.setParameter("tom", periode.getTom());

        List<Object[]> result = query.getResultList();

        return result.stream().collect(Collectors.toMap(
            o -> (Long) o[0],
            o -> new Saksnummer((String) o[1])
        ));
    }

    record Arbeidsforhold(String orgnr, BigDecimal aktivitetsgrad, BigDecimal utbetalingsgrad) {
    }

    record Uttaksperiode(DatoIntervallEntitet periode, List<Arbeidsforhold> arbeidsforholdMedDiff) {
    }

}
