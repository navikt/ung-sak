package no.nav.folketrygdloven.beregningsgrunnlag.gradering;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.typer.Saksnummer;

@ProsessTask(GraderingMotInntektKandidatUtledningTask.TASKTYPE)
@ApplicationScoped
public class GraderingMotInntektKandidatUtledningTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "gradering.kandidatUtledning";
    public static final String DRYRUN = "dryrun";
    public static final String KALKULUS_UTLEDNING = "kalkulusUtledning";
    public static final String YTELSE = "ytelse";

    private static final Logger log = LoggerFactory.getLogger(GraderingMotInntektKandidatUtledningTask.class);
    public static final LocalDate FOM_DATO_INNTEKT_GRADERING = LocalDate.of(2024, 11, 4);
    private InntektGraderingRepository inntektGraderingRepository;
    private KandidaterForInntektgraderingTjeneste kandidaterForInntektgraderingTjeneste;


    GraderingMotInntektKandidatUtledningTask() {
        // CDI
    }

    @Inject
    public GraderingMotInntektKandidatUtledningTask(InntektGraderingRepository inntektGraderingRepository, KandidaterForInntektgraderingTjeneste kandidaterForInntektgraderingTjeneste) {
        this.inntektGraderingRepository = inntektGraderingRepository;
        this.kandidaterForInntektgraderingTjeneste = kandidaterForInntektgraderingTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var dryRunValue = prosessTaskData.getPropertyValue(DRYRUN);
        var kalkulusUtledningValue = prosessTaskData.getPropertyValue(KALKULUS_UTLEDNING);
        var kalkulusUtledning = Boolean.parseBoolean(kalkulusUtledningValue);
        var ytelseType = prosessTaskData.getPropertyValue(YTELSE);
        FagsakYtelseType fagsakYtelseType = ytelseType != null ? FagsakYtelseType.fraKode(ytelseType) : FagsakYtelseType.PLEIEPENGER_SYKT_BARN;
        var dryRun = Boolean.parseBoolean(dryRunValue);
        if (dryRun) {
            var fagsaker = inntektGraderingRepository.hentFagsakIdOgSaksnummer(fagsakYtelseType, FOM_DATO_INNTEKT_GRADERING);
            log.info("DRYRUN - Fant {} kandidater til gradering mot inntekt.", fagsaker.size());
            if (kalkulusUtledning) {
                List<String> saksnummerMedAksjonspunkt = fagsaker.entrySet().stream()
                    .filter(f -> {
                        var vurderingsperioder = kandidaterForInntektgraderingTjeneste.finnGraderingMotInntektPerioder(f.getKey(), FOM_DATO_INNTEKT_GRADERING);
                        return !vurderingsperioder.isEmpty();
                    })
                    .map(Map.Entry::getValue)
                    .map(Saksnummer::getVerdi)
                    .toList();
                log.info("KALKULUSUTLEDNING - Fant følgende saksnummer som kandidater til gradering mot inntekt: {} .", saksnummerMedAksjonspunkt);
            }

        } else {
            throw new UnsupportedOperationException("Støtter kun dryRun av gradering mot inntekt foreløpig");
        }
    }

}
