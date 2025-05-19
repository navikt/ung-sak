package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.UttalelseEntitet;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.AvbrytEtterlysningTask;
import no.nav.ung.sak.etterlysning.EtterlysningTjeneste;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.uttalelse.EtterlysningInfo;
import no.nav.ung.sak.uttalelse.EtterlysningsPeriode;
import no.nav.ung.sak.ytelse.RapportertInntektMapper;
import no.nav.ung.sak.ytelse.kontroll.KontrollerteInntektperioderTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.KONTROLLER_REGISTER_INNTEKT;
import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

@ApplicationScoped
@BehandlingStegRef(value = KONTROLLER_REGISTER_INNTEKT)
@BehandlingTypeRef
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
public class KontrollerInntektSteg implements BehandlingSteg {

    private static final Logger log = LoggerFactory.getLogger(KontrollerInntektSteg.class);
    public static final BigDecimal AKSEPTERT_DIFFERANSE = BigDecimal.valueOf(1000);

    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;
    private RapportertInntektMapper rapportertInntektMapper;
    private KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste;
    private BehandlingRepository behandlingRepository;
    private EtterlysningRepository etterlysningRepository;
    private EtterlysningTjeneste etterlysningTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private ProsessTaskTjeneste prosessTaskTjeneste;


    @Inject
    public KontrollerInntektSteg(ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder,
                                 RapportertInntektMapper rapportertInntektMapper,
                                 KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste,
                                 BehandlingRepository behandlingRepository,
                                 EtterlysningRepository etterlysningRepository,
                                 EtterlysningTjeneste etterlysningTjeneste,
                                 InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                 ProsessTaskTjeneste prosessTaskTjeneste) {
        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
        this.rapportertInntektMapper = rapportertInntektMapper;
        this.kontrollerteInntektperioderTjeneste = kontrollerteInntektperioderTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.etterlysningRepository = etterlysningRepository;
        this.etterlysningTjeneste = etterlysningTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public KontrollerInntektSteg() {
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        kontrollerteInntektperioderTjeneste.ryddPerioderFritattForKontroll(behandlingId);
        var prosessTriggerTidslinje = prosessTriggerPeriodeUtleder.utledTidslinje(behandlingId);
        var rapporterteInntekterTidslinje = rapportertInntektMapper.mapAlleGjeldendeRegisterOgBrukersInntekter(behandlingId);
        var etterlysninger = etterlysningTjeneste.hentGjeldendeEtterlysninger(kontekst.getBehandlingId(), kontekst.getFagsakId(), EtterlysningType.UTTALELSE_KONTROLL_INNTEKT);

        var etterlysningsperioder = etterlysninger.stream()
            .map(it -> new EtterlysningsPeriode(
                it.getPeriode().toLocalDateInterval(),
                new EtterlysningInfo(it.getStatus(), it.getUttalelse().map(UttalelseEntitet::harGodtattEndringen).orElse(null)),
                it.getGrunnlagsreferanse())).toList();

        var registerinntekterForEtterlysninger = rapportertInntektMapper.finnRegisterinntekterForEtterlysninger(behandlingId, etterlysningsperioder);

        var kontrollResultat = new KontrollerInntektTjeneste(AKSEPTERT_DIFFERANSE).utførKontroll(prosessTriggerTidslinje, rapporterteInntekterTidslinje, registerinntekterForEtterlysninger);

        log.info("Kontrollresultat ble {}", kontrollResultat.toSegments());
        håndterPeriodisertKontrollresultat(kontekst, kontrollResultat, etterlysninger);
        return avgjørResultat(kontrollResultat, prosessTriggerTidslinje);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell,
                                   BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (!tilSteg.equals(KONTROLLER_REGISTER_INNTEKT)) {
            final var behandlingId = kontekst.getBehandlingId();
            final var behandling = behandlingRepository.hentBehandling(behandlingId);
            if (behandling.getOriginalBehandlingId().isPresent()) {
                kontrollerteInntektperioderTjeneste.gjenopprettTilOriginal(behandling.getOriginalBehandlingId().get(), behandlingId);
            }
        }
    }

    private void håndterPeriodisertKontrollresultat(BehandlingskontrollKontekst kontekst,
                                                    LocalDateTimeline<Kontrollresultat> kontrollResultat,
                                                    List<Etterlysning> etterlysninger) {
        List<Etterlysning> etterlysningerSomSkalAvbrytes = new ArrayList<>();
        List<Etterlysning> etterlysningerSomSkalOpprettes = new ArrayList<>();
        var grunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(kontekst.getBehandlingId());
        for (var kontrollSegment : kontrollResultat.toSegments()) {
            switch (kontrollSegment.getValue().type()) {
                case BRUK_GODKJENT_ELLER_RAPPORTERT_INNTEKT_FRA_BRUKER -> {
                    log.info("Bruker inntekt fra bruker eller godkjent inntekt fra register for periode {}", kontrollSegment.getLocalDateInterval());
                    etterlysningerSomSkalAvbrytes.addAll(avbrytDersomEksisterendeEtterlysning(etterlysninger, kontrollSegment.getLocalDateInterval()));
                    kontrollerteInntektperioderTjeneste.opprettKontrollerteInntekterPerioderFraBruker(
                        kontekst.getBehandlingId(),
                        kontrollSegment.getLocalDateInterval(),
                        kontrollSegment.getValue().inntektsresultat()
                    );
                }
                case OPPRETT_OPPGAVE_TIL_BRUKER_MED_NY_FRIST -> {
                    log.info("Oppretter ny etterlysning med utvidet frist for periode {}", kontrollSegment.getLocalDateInterval());
                    etterlysningerSomSkalAvbrytes.addAll(avbrytDersomEksisterendeEtterlysning(etterlysninger, kontrollSegment.getLocalDateInterval()));
                    etterlysningerSomSkalOpprettes.add(opprettNyEtterlysning(kontekst.getBehandlingId(), kontrollSegment.getLocalDateInterval(), grunnlag.orElseThrow(() -> new IllegalStateException("Forventer å finne iaygrunnlag")).getEksternReferanse()));
                }
                case OPPRETT_OPPGAVE_TIL_BRUKER -> {
                    log.info("Oppretter etterlysning hvis ikke finnes for periode {}", kontrollSegment.getLocalDateInterval());
                    if (!harEksisterendeEtterlysningPåVent(etterlysninger, kontrollSegment.getLocalDateInterval())) {
                        etterlysningerSomSkalOpprettes.add(opprettNyEtterlysning(kontekst.getBehandlingId(), kontrollSegment.getLocalDateInterval(), grunnlag.orElseThrow(() -> new IllegalStateException("Forventer å finne iaygrunnlag")).getEksternReferanse()));
                    }
                }
            }
        }

        final var prosessTaskGruppe = new ProsessTaskGruppe();
        if (!etterlysningerSomSkalAvbrytes.isEmpty()) {
            log.info("Avbryter etterlysninger {}", etterlysningerSomSkalAvbrytes);
            etterlysningRepository.lagre(etterlysningerSomSkalAvbrytes);
            prosessTaskGruppe.addNesteSekvensiell(lagTaskForAvbrytelseAvEtterlysning(kontekst));
        }

        if (!etterlysningerSomSkalOpprettes.isEmpty()) {
            log.info("Oppretter etterlysninger {}", etterlysningerSomSkalOpprettes);
            etterlysningRepository.lagre(etterlysningerSomSkalOpprettes);
            prosessTaskGruppe.addNesteSekvensiell(lagTaskForOpprettingAvEtterlysning(kontekst));
        }

        if (!prosessTaskGruppe.getTasks().isEmpty()) {
            prosessTaskTjeneste.lagre(prosessTaskGruppe);
        }

    }

    private static boolean harEksisterendeEtterlysningPåVent(List<Etterlysning> etterlysninger, LocalDateInterval periode) {
        return etterlysninger.stream().anyMatch(e -> e.getStatus().equals(EtterlysningStatus.VENTER) &&
            e.getPeriode().toLocalDateInterval().overlaps(periode));
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

    private BehandleStegResultat avgjørResultat(LocalDateTimeline<Kontrollresultat> kontrollResultat, LocalDateTimeline<Set<BehandlingÅrsakType>> prosessTriggerTidslinje) {

        final var skalVenteTilRapportering = !kontrollResultat.filterValue(it -> it.type().equals(KontrollResultatType.SETT_PÅ_VENT_TIL_RAPPORTERINGSFRIST)).isEmpty();
        if (skalVenteTilRapportering) {
            return BehandleStegResultat.utførtMedAksjonspunktResultater(AksjonspunktResultat.opprettForAksjonspunktMedFrist(
                AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_RAPPORTERINGSFRIST,
                Venteårsak.VENT_INNTEKT_RAPPORTERINGSFRIST,
                utledVentefrist(prosessTriggerTidslinje)));
        }

        final var skalOppretteAksjonspunkt = !kontrollResultat.filterValue(it -> it.type().equals(KontrollResultatType.OPPRETT_AKSJONSPUNKT)).isEmpty();
        if (skalOppretteAksjonspunkt) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.KONTROLLER_INNTEKT));
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }


    private ProsessTaskData lagTaskForOpprettingAvEtterlysning(BehandlingskontrollKontekst kontekst) {
        var prosessTaskData = ProsessTaskData.forProsessTask(OpprettEtterlysningTask.class);
        prosessTaskData.setProperty(OpprettEtterlysningTask.ETTERLYSNING_TYPE, EtterlysningType.UTTALELSE_KONTROLL_INNTEKT.getKode());
        prosessTaskData.setBehandling(kontekst.getFagsakId(), kontekst.getBehandlingId());
        return prosessTaskData;
    }

    private ProsessTaskData lagTaskForAvbrytelseAvEtterlysning(BehandlingskontrollKontekst kontekst) {
        var prosessTaskData = ProsessTaskData.forProsessTask(AvbrytEtterlysningTask.class);
        prosessTaskData.setBehandling(kontekst.getFagsakId(), kontekst.getBehandlingId());
        return prosessTaskData;
    }

    private LocalDateTime utledVentefrist(LocalDateTimeline<Set<BehandlingÅrsakType>> prosessTriggerTidslinje) {
        final var tidslinjeRelevanteÅrsaker = prosessTriggerTidslinje.filterValue(it -> it.contains(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT) || it.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));
        final var harIkkePassertRapporteringsfrist = tidslinjeRelevanteÅrsaker.filterValue(it -> !it.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));
        final var sisteDatoForRapportertInntekt = harIkkePassertRapporteringsfrist.getMaxLocalDate();
        if (sisteDatoForRapportertInntekt.getDayOfMonth() < 7) {
            return sisteDatoForRapportertInntekt.withDayOfMonth(7).atStartOfDay();
        } else {
            return sisteDatoForRapportertInntekt.plusMonths(1).withDayOfMonth(7).atStartOfDay();
        }
    }

}
