package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.AvbrytEtterlysningTask;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.uttalelse.EtterlysningInfo;
import no.nav.ung.sak.uttalelse.EtterlysningsPeriode;
import no.nav.ung.sak.ytelse.KontrollerteInntektperioderTjeneste;
import no.nav.ung.sak.ytelse.RapportertInntektMapper;
import no.nav.ung.sak.ytelse.RapporterteInntekter;

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

    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;
    private RapportertInntektMapper rapportertInntektMapper;
    private KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste;
    private BehandlingRepository behandlingRepository;
    private EtterlysningRepository etterlysningRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public KontrollerInntektSteg(ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder,
                                 RapportertInntektMapper rapportertInntektMapper,
                                 KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste,
                                 BehandlingRepository behandlingRepository,
                                 EtterlysningRepository etterlysningRepository,
                                 InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                 ProsessTaskTjeneste prosessTaskTjeneste) {
        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
        this.rapportertInntektMapper = rapportertInntektMapper;
        this.kontrollerteInntektperioderTjeneste = kontrollerteInntektperioderTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.etterlysningRepository = etterlysningRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public KontrollerInntektSteg() {
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        var rapporterteInntekterTidslinje = rapportertInntektMapper.mapAlleGjeldendeRegisterOgBrukersInntekter(behandlingId);
        var prosessTriggerTidslinje = prosessTriggerPeriodeUtleder.utledTidslinje(behandlingId);
        var etterlysninger = etterlysningRepository.hentEtterlysninger(kontekst.getBehandlingId(), EtterlysningType.UTTALELSE_KONTROLL_INNTEKT);

        //TODO fix erEndringGodkjent
        var etterlysningsperioder = etterlysninger.stream().map(it -> new EtterlysningsPeriode(it.getPeriode().toLocalDateInterval(), new EtterlysningInfo(it.getStatus(), it.getUttalelse() != null), it.getGrunnlagsreferanse())).toList();
        var registerinntekterForEtterlysninger = rapportertInntektMapper.finnRegisterinntekterForEtterlysninger(behandlingId, etterlysningsperioder);

        var kontrollResultat = KontrollerInntektTjeneste.utførKontroll(prosessTriggerTidslinje, rapporterteInntekterTidslinje, registerinntekterForEtterlysninger);
        håndterPeriodisertKontrollresultat(kontekst, kontrollResultat, rapporterteInntekterTidslinje, prosessTriggerTidslinje, etterlysninger);
        return avgjørResultat(behandlingId, kontrollResultat, prosessTriggerTidslinje);
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
                                                    LocalDateTimeline<KontrollResultat> kontrollResultat,
                                                    LocalDateTimeline<RapporterteInntekter> rapporterteInntekterTidslinje,
                                                    LocalDateTimeline<Set<BehandlingÅrsakType>> prosessTriggerTidslinje,
                                                    List<Etterlysning> etterlysninger) {
        List<Etterlysning> etterlysningerSomSkalAvbrytes = new ArrayList<>();
        List<Etterlysning> etterlysningerSomSkalOpprettes = new ArrayList<>();
        var grunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(kontekst.getBehandlingId());
        for (var segment : kontrollResultat.toSegments()) {
            switch (segment.getValue()) {
                case BRUK_INNTEKT_FRA_BRUKER -> {
                    etterlysningerSomSkalAvbrytes.addAll(avbrytDersomEksisterendeEtterlysning(etterlysninger, segment));
                    kontrollerteInntektperioderTjeneste.opprettKontrollerteInntekterPerioderFraBruker(
                        kontekst.getBehandlingId(),
                        rapporterteInntekterTidslinje.mapValue(RapporterteInntekter::brukerRapporterteInntekter).intersection(segment.getLocalDateInterval()),
                        prosessTriggerTidslinje);
                }
                case OPPRETT_OPPGAVE_TIL_BRUKER_MED_NY_FRIST -> {
                    etterlysningerSomSkalAvbrytes.addAll(avbrytDersomEksisterendeEtterlysning(etterlysninger, segment));
                    etterlysningerSomSkalOpprettes.add(opprettNyEtterlysning(kontekst.getBehandlingId(), segment, grunnlag.getEksternReferanse()));
                }
                case OPPRETT_OPPGAVE_TIL_BRUKER -> {
                    if (!harEksisterendeEtterlysningPåVent(segment, etterlysninger)) {
                        etterlysningerSomSkalOpprettes.add(opprettNyEtterlysning(kontekst.getBehandlingId(), segment, grunnlag.getEksternReferanse()));
                    }
                }
            }
        }

        if (!etterlysningerSomSkalOpprettes.isEmpty()) {
            etterlysningRepository.lagre(etterlysningerSomSkalOpprettes);
            lagTaskForOpprettingAvEtterlysning(kontekst);
        }

        if (!etterlysningerSomSkalAvbrytes.isEmpty()) {
            etterlysningRepository.lagre(etterlysningerSomSkalAvbrytes);
            lagTaskForAvbrytelseAvEtterlysning(kontekst);
        }
    }

    private static boolean harEksisterendeEtterlysningPåVent(LocalDateSegment<KontrollResultat> segment, List<Etterlysning> etterlysninger) {
        return etterlysninger.stream().anyMatch(e -> e.getStatus().equals(EtterlysningStatus.VENTER) &&
            e.getPeriode().toLocalDateInterval().overlaps(segment.getLocalDateInterval()));
    }

    private Etterlysning opprettNyEtterlysning(Long behandlingId, LocalDateSegment<KontrollResultat> segment, UUID iayRef) {
        UUID bestillingsId = UUID.randomUUID();
        final var etterlysning = Etterlysning.forInntektKontrollUttalelse(behandlingId,
            iayRef,
            bestillingsId,
            DatoIntervallEntitet.fra(segment.getFom(), segment.getTom()));
        return etterlysning;
    }

    private List<Etterlysning> avbrytDersomEksisterendeEtterlysning(List<Etterlysning> etterlysninger, LocalDateSegment<KontrollResultat> segment) {
        var etterlysningerSomSkalAvbrytes = etterlysninger.stream()
            .filter(etterlysning ->
                etterlysning.getPeriode().toLocalDateInterval().overlaps(segment.getLocalDateInterval()))
            .filter(e -> e.getStatus().equals(EtterlysningStatus.VENTER))
            .toList();
        etterlysningerSomSkalAvbrytes.forEach(Etterlysning::skalAvbrytes);

        return etterlysningerSomSkalAvbrytes;
    }

    private BehandleStegResultat avgjørResultat(Long behandlingId, LocalDateTimeline<KontrollResultat> kontrollResultat, LocalDateTimeline<Set<BehandlingÅrsakType>> prosessTriggerTidslinje) {

        final var skalVenteTilRapportering = !kontrollResultat.filterValue(it -> it.equals(KontrollResultat.SETT_PÅ_VENT_TIL_RAPPORTERINGSFRIST)).isEmpty();
        if (skalVenteTilRapportering) {
            return BehandleStegResultat.utførtMedAksjonspunktResultater(AksjonspunktResultat.opprettForAksjonspunktMedFrist(
                AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_RAPPORTERINGSFRIST,
                Venteårsak.VENT_INNTEKT_RAPPORTERINGSFRIST,
                utledVentefrist(prosessTriggerTidslinje)));
        }

        final var skalVenteGrunnetNyEtterlysning = !kontrollResultat.filterValue(it -> it.equals(KontrollResultat.OPPRETT_OPPGAVE_TIL_BRUKER_MED_NY_FRIST)).isEmpty();
        if (skalVenteGrunnetNyEtterlysning) {
            return BehandleStegResultat.utførtMedAksjonspunktResultater(AksjonspunktResultat.opprettForAksjonspunktMedFrist(AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE, Venteårsak.VENTER_PÅ_ETTERLYST_INNTEKT_UTTALELSE, LocalDateTime.now().plusDays(14)));
        }

        final var skalVentePåEksisterendeEtterlysning = !kontrollResultat.filterValue(it -> it.equals(KontrollResultat.OPPRETT_OPPGAVE_TIL_BRUKER)).isEmpty();
        if (skalVentePåEksisterendeEtterlysning) {
            return BehandleStegResultat.utførtMedAksjonspunktResultater(
                AksjonspunktResultat.opprettForAksjonspunktMedFrist(
                    AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE,
                    Venteårsak.VENTER_PÅ_ETTERLYST_INNTEKT_UTTALELSE,
                    finnEksisterendeFrist(behandlingId)));
        }

        final var skalOppretteAksjonspunkt = !kontrollResultat.filterValue(it -> it.equals(KontrollResultat.OPPRETT_AKSJONSPUNKT)).isEmpty();
        if (skalOppretteAksjonspunkt) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.KONTROLLER_INNTEKT));
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }


    private void lagTaskForOpprettingAvEtterlysning(BehandlingskontrollKontekst kontekst) {
        var prosessTaskData = ProsessTaskData.forProsessTask(OpprettEtterlysningTask.class);
        prosessTaskData.setProperty(OpprettEtterlysningTask.ETTERLYSNING_TYPE, EtterlysningType.UTTALELSE_KONTROLL_INNTEKT.getKode());
        prosessTaskData.setBehandling(kontekst.getFagsakId(), kontekst.getBehandlingId());
        prosessTaskTjeneste.lagre(prosessTaskData);
    }

    private void lagTaskForAvbrytelseAvEtterlysning(BehandlingskontrollKontekst kontekst) {
        var prosessTaskData = ProsessTaskData.forProsessTask(AvbrytEtterlysningTask.class);
        prosessTaskData.setBehandling(kontekst.getFagsakId(), kontekst.getBehandlingId());
        prosessTaskTjeneste.lagre(prosessTaskData);
    }


    private LocalDateTime finnEksisterendeFrist(Long behandlingId) {
        final var behandling = behandlingRepository.hentBehandling(behandlingId);
        return behandling.getAksjonspunktForHvisFinnes(AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE.getKode()).map(Aksjonspunkt::getFristTid).orElse(LocalDateTime.now().plusDays(14));
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
