package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse.ferietillegg;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.util.Tuple;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse.feriepenger.PleiepengerBeregnFeriepenger;

@ProsessTask(FerietilleggKandidatUtledningTask.TASKTYPE)
@ApplicationScoped
public class FerietilleggKandidatUtledningTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "ferietillegg.kandidatUtledning";
    public static final String YTELSE_TYPE = "ytelseType";
    public static final String PERIODE_FOM = "fom";
    public static final String PERIODE_TOM = "tom";
    public static final String DRYRUN = "dryrun";

    private static final Logger log = LoggerFactory.getLogger(FerietilleggKandidatUtledningTask.class);
    private BeregningsresultatRepository beregningsresultatRepository;
    private PleiepengerBeregnFeriepenger beregnFeriepengerTjeneste;


    FerietilleggKandidatUtledningTask() {
        // CDI
    }

    @Inject
    public FerietilleggKandidatUtledningTask(BeregningsresultatRepository beregningsresultatRepository,
                                             @FagsakYtelseTypeRef(FagsakYtelseType.PLEIEPENGER_SYKT_BARN) PleiepengerBeregnFeriepenger beregnFeriepengerTjeneste) {
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.beregnFeriepengerTjeneste = beregnFeriepengerTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var ytelseType = FagsakYtelseType.fromString(prosessTaskData.getPropertyValue(YTELSE_TYPE));
        var fomValue = prosessTaskData.getPropertyValue(PERIODE_FOM);
        var fom = LocalDate.parse(fomValue);
        var tomValue = prosessTaskData.getPropertyValue(PERIODE_TOM);
        var tom = LocalDate.parse(tomValue);
        var dryRunValue = prosessTaskData.getPropertyValue(DRYRUN);
        var dryRun = Boolean.parseBoolean(dryRunValue);

        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);


        var behandlinger = beregningsresultatRepository.hentSisteBehandlingerMedUtbetalingForDagpenger(ytelseType, fom, tom);


        var medBeregnetFerietillegg = behandlinger.stream().filter(r -> {
            var resultat = beregningsresultatRepository.hentBgBeregningsresultat(r.getId());
            if (resultat.isEmpty()) {
                return false;
            }
            if (harBeregnetFerietillegg(resultat.get())) {
                return false;
            }
            var feriepengeOppsummering = beregnFeriepengerTjeneste.beregnFeriepengerOppsummering(BehandlingReferanse.fra(r), resultat.get());
            return feriepengeOppsummering.harFerietillegg();
        }).toList();


        if (dryRun) {
            var saksnummer = medBeregnetFerietillegg.stream().map(Behandling::getFagsak).map(Fagsak::getSaksnummer).collect(Collectors.toSet());
            log.info("DRYRUN - Følgende kandidater har uberegnet ferietillegg for '{}' og perioden '{}: {}'.", ytelseType, periode, saksnummer);
        } else {
            throw new UnsupportedOperationException("Kun dryrun er støttet");
        }
    }

    private boolean harBeregnetFerietillegg(BeregningsresultatEntitet beregningsresultatEntitet) {
        return beregningsresultatEntitet.getBeregningsresultatPerioder().stream().flatMap(p -> p.getBeregningsresultatAndelList().stream())
            .anyMatch(a -> a.getInntektskategori().equals(Inntektskategori.DAGPENGER) && a.getFeriepengerÅrsbeløp() != null && !a.getFeriepengerÅrsbeløp().erNullEllerNulltall());
    }
}
