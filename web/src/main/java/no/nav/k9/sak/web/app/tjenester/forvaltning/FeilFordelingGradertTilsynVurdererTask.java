package no.nav.k9.sak.web.app.tjenester.forvaltning;

import static no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1.FraKalkulusMapper.mapArbeidsgiver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import no.nav.folketrygdloven.kalkulus.request.v1.forvaltning.OppdaterYtelsesspesifiktGrunnlagForRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.forvaltning.OppdaterYtelsesspesifiktGrunnlagListeRequest;
import no.nav.folketrygdloven.kalkulus.response.v1.forvaltning.EndretPeriodeListeRespons;
import no.nav.folketrygdloven.kalkulus.response.v1.forvaltning.PeriodeDifferanse;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.utbetalingdiff.DataDumpSimulertUtbetaling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.utbetalingdiff.DumpSimulertUtbetalingDiff;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.utbetalingdiff.DumpSimulertUtbetalingDiffAndel;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.utbetalingdiff.DumpSimulertUtbetalingDiffPeriode;
import no.nav.k9.sak.økonomi.tilkjentytelse.JsonMapper;

@ApplicationScoped
@ProsessTask(FeilFordelingGradertTilsynVurdererTask.TASKTYPE)
public class FeilFordelingGradertTilsynVurdererTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "feil.fordeling.tilsyn.kandidatutleder.task";
    public static final String PERIODE_PROP_NAME = "periode";
    private EntityManager entityManager;

    private BehandlingRepository behandlingRepository;

    private FeilRefusjonVedTilsynUtrekkTjeneste feilRefusjonVedTilsynUtrekkTjeneste;


    FeilFordelingGradertTilsynVurdererTask() {
        //for cdi proxy
    }

    @Inject
    public FeilFordelingGradertTilsynVurdererTask(EntityManager entityManager,
                                                  BehandlingRepository behandlingRepository, FeilRefusjonVedTilsynUtrekkTjeneste feilRefusjonVedTilsynUtrekkTjeneste) {
        this.entityManager = entityManager;
        this.behandlingRepository = behandlingRepository;
        this.feilRefusjonVedTilsynUtrekkTjeneste = feilRefusjonVedTilsynUtrekkTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData pd) {
        var query = entityManager.createQuery("Select b.fagsak.id from DataDumpGrunnlag dd inner join Behandling b on b.id = dd.behandlingId", Long.class);
        Set<Long> alleredeKjørteFagsaker = query.getResultStream().collect(Collectors.toSet());
        String periode = pd.getPropertyValue(PERIODE_PROP_NAME);
        var fagsakIdSaksnummerMap = hentFagsakIdOgSaksnummer(new Periode(periode), alleredeKjørteFagsaker);


        List<DataDumpSimulertUtbetaling> resultater = new ArrayList<>();

        for (var fagsakIdOgSaksnummerEntry : fagsakIdSaksnummerMap.entrySet()) {
            var fagsakId = fagsakIdOgSaksnummerEntry.getKey();
            var sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId);
            if (sisteBehandling.isEmpty()) {
                continue;
            }

            finnFeilForBehandling(sisteBehandling.get())
                .ifPresent(resultater::add);

        }

        resultater.forEach(entityManager::persist);
        entityManager.flush();

    }

    private Optional<DataDumpSimulertUtbetaling> finnFeilForBehandling(Behandling sisteBehandling) {
        var kalkulusDiffRequestOgRespons = feilRefusjonVedTilsynUtrekkTjeneste.finnFeilForBehandling(sisteBehandling, false);
        return kalkulusDiffRequestOgRespons.map(it -> mapKalkulusRespons(it.respons(), it.request()))
            .map(it -> new DataDumpSimulertUtbetaling(sisteBehandling.getId(), it));
    }

    private static List<DumpSimulertUtbetalingDiff> mapKalkulusRespons(EndretPeriodeListeRespons endretPeriodeListeRespons, OppdaterYtelsesspesifiktGrunnlagListeRequest request) {
        return endretPeriodeListeRespons.getPerioderPrReferanse().stream()
            .map(r ->
            {
                var mappetPerioder = r.getPeriodedifferanser().stream().map(p ->
                    new DumpSimulertUtbetalingDiffPeriode(
                        DatoIntervallEntitet.fraOgMedTilOgMed(p.getPeriode().getFom(), p.getPeriode().getTom()),
                        p.getAndeldifferanser().stream().map(a -> new DumpSimulertUtbetalingDiffAndel(
                            a.getArbeidsgiver() != null ? mapArbeidsgiver(a.getArbeidsgiver()) : null,
                            a.getGammelDagsats().intValue(),
                            a.getNyDagsats().intValue(),
                            a.getGammelDagsatsBruker().intValue(),
                            a.getNyDagsatsBruker().intValue(),
                            a.getGammelDagsatsArbeidsgiver().intValue(),
                            a.getNyDagsatsArbeidsgiver().intValue()
                        )).toList(),
                        finnTotalFeilBruker(p),
                        finnTotalFeilArbeidsgiver(p)
                    )).toList();
                return new DumpSimulertUtbetalingDiff(
                    r.getEksternReferanse(),
                    finnRequestJson(request, r.getEksternReferanse()), mappetPerioder,
                    mappetPerioder.stream().map(DumpSimulertUtbetalingDiffPeriode::getTotalFeilutbetalingBruker).reduce(Integer::sum).orElse(0),
                    mappetPerioder.stream().map(DumpSimulertUtbetalingDiffPeriode::getTotalFeilutbetalingArbeidsgiver).reduce(Integer::sum).orElse(0)
                );
            }).toList();
    }

    private static int finnTotalFeilArbeidsgiver(PeriodeDifferanse p) {
        var feilutbetalinger = p.getAndeldifferanser().stream().map(a -> a.getGammelDagsatsArbeidsgiver() - a.getNyDagsatsArbeidsgiver()).toList();
        var positiveFeilutbetalinger = feilutbetalinger.stream().filter(f -> f > 0L)
            .reduce(Long::sum)
            .map(Long::intValue)
            .orElse(0);
        var negativeFeilutbetalinger = feilutbetalinger.stream().filter(f -> f < 0L)
            .map(f -> f * -1)
            .reduce(Long::sum)
            .map(Long::intValue)
            .orElse(0);
        var feilutbetaltSats = positiveFeilutbetalinger > negativeFeilutbetalinger ? positiveFeilutbetalinger : negativeFeilutbetalinger;
        return feilutbetaltSats * DatoIntervallEntitet.fraOgMedTilOgMed(p.getPeriode().getFom(), p.getPeriode().getTom()).antallArbeidsdager();
    }

    private static int finnTotalFeilBruker(PeriodeDifferanse p) {
        return p.getAndeldifferanser().stream().map(a -> a.getGammelDagsatsBruker() - a.getNyDagsatsBruker())
            .reduce(Long::sum)
            .map(Long::intValue)
            .map(sats -> DatoIntervallEntitet.fraOgMedTilOgMed(p.getPeriode().getFom(), p.getPeriode().getTom()).antallArbeidsdager() * sats)
            .orElse(0);
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


    public Map<Long, Saksnummer> hentFagsakIdOgSaksnummer(Periode periode, Set<Long> unntaksliste) {
        Query query;

        String sql = """
            select f.id, f.saksnummer from Fagsak f
             where f.ytelse_type = :ytelseType
               and upper(f.periode) > :fom
               and lower(f.periode) < :tom
              """;

        if (!unntaksliste.isEmpty()) {
            sql = sql + " and f.id not in (:unntaksliste)";
        }


        query = entityManager.createNativeQuery(sql); // NOSONAR

        query.setParameter("ytelseType", FagsakYtelseType.PLEIEPENGER_SYKT_BARN.getKode());
        query.setParameter("fom", periode.getFom());
        query.setParameter("tom", periode.getTom());

        if (!unntaksliste.isEmpty()) {
            query.setParameter("unntaksliste", unntaksliste);
        }


        List<Object[]> result = query.getResultList();

        return result.stream().collect(Collectors.toMap(
            o -> (Long) o[0],
            o -> new Saksnummer((String) o[1])
        ));
    }


}
