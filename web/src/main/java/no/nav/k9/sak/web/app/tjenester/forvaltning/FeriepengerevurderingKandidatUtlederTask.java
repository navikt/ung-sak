package no.nav.k9.sak.web.app.tjenester.forvaltning;

import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.Hjelpetidslinjer;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.typer.Saksnummer;

@ApplicationScoped
@ProsessTask(FeriepengerevurderingKandidatUtlederTask.TASKTYPE)
public class FeriepengerevurderingKandidatUtlederTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "feriepenger.revurdering.kandidatutleder.task";
    private static final Logger logger = LoggerFactory.getLogger(FeriepengerevurderingKandidatUtlederTask.class);

    private FagsakRepository fagsakRepository;

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;

    FeriepengerevurderingKandidatUtlederTask() {
        //for cdi proxy
    }

    @Inject
    public FeriepengerevurderingKandidatUtlederTask(FagsakRepository fagsakRepository, BehandlingRepository behandlingRepository, BeregningsresultatRepository beregningsresultatRepository) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
    }

    @Override
    public void doTask(ProsessTaskData pd) {
        String saksnummer = Objects.requireNonNull(pd.getPropertyValue("saksnummer"));
        Fagsak fagsak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(saksnummer)).orElseThrow();
        Behandling sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).orElseThrow();
        if (!sisteBehandling.erAvsluttet()) {
            logger.info("Siste behandling er åpen for {}, avbryter", saksnummer);
        } else if (sisteBehandling.getAvsluttetDato().isAfter(LocalDate.of(2022, 11, 19).atStartOfDay())) {
            logger.info("Siste behandling for {} ble avsluttet så sent som {}, avbryter", saksnummer, sisteBehandling.getAvsluttetDato());
        } else {
            BeregningsresultatEntitet beregningsresultat = beregningsresultatRepository.hentEndeligBeregningsresultat(sisteBehandling.getId()).orElseThrow();
            LocalDateTimeline<Boolean> feriepengeopptjeningTidslinje = beregningsresultat.getBeregningsresultatAndelTimeline()
                .filterValue(andeler -> andeler.stream().anyMatch(andel -> andel.getDagsats() != 0 && Set.of(Inntektskategori.ARBEIDSTAKER, Inntektskategori.SJØMANN).contains(andel.getInntektskategori())))
                .mapValue(v -> true);
            if (feriepengeopptjeningTidslinje.isEmpty()){
                logger.info("Sak {} ikke kandidat for revurdering - ikke noe grunnlag. ", saksnummer);
            } else {
                long antallDager = tellDager(Hjelpetidslinjer.fjernHelger(feriepengeopptjeningTidslinje));
                NavigableMap<Year, LocalDateTimeline<Boolean>> prÅr = TidslinjeUtil.splittOgGruperPåÅrstall(feriepengeopptjeningTidslinje);
                boolean flereÅr = prÅr.size() > 1;
                if (antallDager > 60 && flereÅr) {
                    logger.info("Sak {} kandidat for revurdering. Opptjening i {}", saksnummer, prÅr.keySet());
                } else {
                    logger.info("Sak {} ikke kandidat for revurdering. {} dager flereÅr {}", saksnummer, antallDager, flereÅr);
                }
            }
        }
    }

    private static long tellDager(LocalDateTimeline<Boolean> feriepengeopptjeningTidslinje) {
        return feriepengeopptjeningTidslinje.stream().mapToLong(segment -> ChronoUnit.DAYS.between(segment.getFom(), segment.getTom()) + 1).sum();
    }


}
