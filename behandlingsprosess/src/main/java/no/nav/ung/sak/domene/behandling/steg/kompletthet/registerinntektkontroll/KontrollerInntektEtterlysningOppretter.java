package no.nav.ung.sak.domene.behandling.steg.kompletthet.registerinntektkontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.sporing.BehandingprosessSporingRepository;
import no.nav.ung.sak.behandlingslager.behandling.sporing.BehandlingprosessSporing;
import no.nav.ung.sak.domene.behandling.steg.kompletthet.EtterlysningBehov;
import no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.KontrollerInntektInputMapper;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.ung.sak.etterlysning.AvbrytEtterlysningTask;
import no.nav.ung.sak.etterlysning.EtterlysningTjeneste;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Dependent
public class KontrollerInntektEtterlysningOppretter {

    private static final Logger log = LoggerFactory.getLogger(KontrollerInntektEtterlysningOppretter.class);

    private EtterlysningRepository etterlysningRepository;
    private BehandingprosessSporingRepository sporingRepository;
    private EtterlysningTjeneste etterlysningTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private KontrollerInntektInputMapper inputMapper;
    private final int akseptertDifferanse;

    @Inject
    public KontrollerInntektEtterlysningOppretter(EtterlysningRepository etterlysningRepository,
                                                  BehandingprosessSporingRepository sporingRepository,
                                                  EtterlysningTjeneste etterlysningTjeneste,
                                                  InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                  ProsessTaskTjeneste prosessTaskTjeneste,
                                                  KontrollerInntektInputMapper inputMapper,
                                                  @KonfigVerdi(value = "AKSEPTERT_DIFFERANSE_KONTROLL", defaultVerdi = "15") int akseptertDifferanse) {
        this.etterlysningRepository = etterlysningRepository;
        this.sporingRepository = sporingRepository;
        this.etterlysningTjeneste = etterlysningTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.inputMapper = inputMapper;
        this.akseptertDifferanse = akseptertDifferanse;
    }

    public void opprettEtterlysninger(BehandlingReferanse behandlingReferanse) {
        var input = inputMapper.mapInput(behandlingReferanse);
        var opprettEtterlysningResultatTidslinje = new EtterlysningutlederKontrollerInntekt(BigDecimal.valueOf(akseptertDifferanse)).utledBehovForEtterlysninger(input);
        try {
            sporingRepository.lagreSporing(new BehandlingprosessSporing(behandlingReferanse.getBehandlingId(),
                JsonObjectMapper.getJson(input),
                JsonObjectMapper.getJson(opprettEtterlysningResultatTidslinje),
                "KontrollerInntektEtterlysningOppretter"));
        } catch (IOException e) {
            // Ikke kritisk å lagre sporing for prosess
            log.warn("Kunne ikke lagre prosessporing for behandling {}: {}", behandlingReferanse.getBehandlingId(), e.getMessage(), e);
        }
        håndterPeriodisertResultat(behandlingReferanse, opprettEtterlysningResultatTidslinje);
    }

    private void håndterPeriodisertResultat(BehandlingReferanse behandlingReferanse,
                                            LocalDateTimeline<EtterlysningBehov> resultat) {
        var etterlysninger = etterlysningTjeneste.hentGjeldendeEtterlysninger(behandlingReferanse.getBehandlingId(), behandlingReferanse.getFagsakId(), EtterlysningType.UTTALELSE_KONTROLL_INNTEKT);
        List<Etterlysning> etterlysningerSomSkalAvbrytes = new ArrayList<>();
        List<Etterlysning> etterlysningerSomSkalOpprettes = new ArrayList<>();
        var grunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingReferanse.getBehandlingId());
        for (var kontrollSegment : resultat.toSegments()) {
            switch (kontrollSegment.getValue()) {
                case ERSTATT_EKSISTERENDE -> {
                    log.info("Oppretter ny etterlysning med utvidet frist for periode {}", kontrollSegment.getLocalDateInterval());
                    etterlysningerSomSkalAvbrytes.addAll(avbrytDersomEksisterendeEtterlysning(etterlysninger, kontrollSegment.getLocalDateInterval()));
                    etterlysningerSomSkalOpprettes.add(opprettNyEtterlysning(behandlingReferanse.getBehandlingId(), kontrollSegment.getLocalDateInterval(), grunnlag.orElseThrow(() -> new IllegalStateException("Forventer å finne iaygrunnlag")).getEksternReferanse()));
                }
                case NY_ETTERLYSNING_DERSOM_INGEN_FINNES -> {
                    log.info("Oppretter etterlysning hvis ikke finnes for periode {}", kontrollSegment.getLocalDateInterval());
                    if (!harEksisterendeEtterlysning(etterlysninger, kontrollSegment.getLocalDateInterval())) {
                        etterlysningerSomSkalOpprettes.add(opprettNyEtterlysning(behandlingReferanse.getBehandlingId(), kontrollSegment.getLocalDateInterval(), grunnlag.orElseThrow(() -> new IllegalStateException("Forventer å finne iaygrunnlag")).getEksternReferanse()));
                    }
                }
                case INGEN_ETTERLYSNING -> {
                    var etterlysningerForPeriode = avbrytDersomEksisterendeEtterlysning(etterlysninger, kontrollSegment.getLocalDateInterval());
                    if (!etterlysningerForPeriode.isEmpty()) {
                        log.info("Avbryter etterlysninger {}  for periode {}", etterlysningerForPeriode, kontrollSegment.getLocalDateInterval());
                        etterlysningerSomSkalAvbrytes.addAll(etterlysningerForPeriode);
                    }
                }
            }
        }

        final var prosessTaskGruppe = new ProsessTaskGruppe();
        if (!etterlysningerSomSkalAvbrytes.isEmpty()) {
            log.info("Avbryter etterlysninger {}", etterlysningerSomSkalAvbrytes);
            etterlysningRepository.lagre(etterlysningerSomSkalAvbrytes);
            prosessTaskGruppe.addNesteSekvensiell(lagTaskForAvbrytelseAvEtterlysning(behandlingReferanse));
        }

        if (!etterlysningerSomSkalOpprettes.isEmpty()) {
            log.info("Oppretter etterlysninger {}", etterlysningerSomSkalOpprettes);
            etterlysningRepository.lagre(etterlysningerSomSkalOpprettes);
            prosessTaskGruppe.addNesteSekvensiell(lagTaskForOpprettingAvEtterlysning(behandlingReferanse));
        }

        if (!prosessTaskGruppe.getTasks().isEmpty()) {
            prosessTaskTjeneste.lagre(prosessTaskGruppe);
        }
    }

    private ProsessTaskData lagTaskForOpprettingAvEtterlysning(BehandlingReferanse behandlingReferanse) {
        var prosessTaskData = ProsessTaskData.forProsessTask(OpprettEtterlysningTask.class);
        prosessTaskData.setProperty(OpprettEtterlysningTask.ETTERLYSNING_TYPE, EtterlysningType.UTTALELSE_KONTROLL_INNTEKT.getKode());
        prosessTaskData.setBehandling(behandlingReferanse.getFagsakId(), behandlingReferanse.getBehandlingId());
        return prosessTaskData;
    }

    private ProsessTaskData lagTaskForAvbrytelseAvEtterlysning(BehandlingReferanse behandlingReferanse) {
        var prosessTaskData = ProsessTaskData.forProsessTask(AvbrytEtterlysningTask.class);
        prosessTaskData.setBehandling(behandlingReferanse.getFagsakId(), behandlingReferanse.getBehandlingId());
        return prosessTaskData;
    }


    private static boolean harEksisterendeEtterlysning(List<Etterlysning> etterlysninger, LocalDateInterval periode) {
        return etterlysninger.stream().anyMatch(e -> e.getPeriode().toLocalDateInterval().overlaps(periode));
    }

    private Etterlysning opprettNyEtterlysning(Long behandlingId, LocalDateInterval periode, UUID iayRef) {
        UUID bestillingsId = UUID.randomUUID();
        final var etterlysning = Etterlysning.forInntektKontrollUttalelse(behandlingId,
                iayRef,
                bestillingsId,
                DatoIntervallEntitet.fra(periode.getFomDato(), periode.getTomDato()));
        return etterlysning;
    }

    private List<Etterlysning> avbrytDersomEksisterendeEtterlysning(List<Etterlysning> etterlysninger, LocalDateInterval periode) {
        var etterlysningerSomSkalAvbrytes = etterlysninger.stream()
                .filter(etterlysning ->
                        etterlysning.getPeriode().toLocalDateInterval().overlaps(periode))
                .filter(e -> e.getStatus().equals(EtterlysningStatus.VENTER))
                .toList();
        etterlysningerSomSkalAvbrytes.forEach(Etterlysning::skalAvbrytes);

        return etterlysningerSomSkalAvbrytes;
    }


}
