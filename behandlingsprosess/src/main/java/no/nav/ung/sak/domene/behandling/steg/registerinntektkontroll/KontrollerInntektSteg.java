package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.KONTROLLER_REGISTER_INNTEKT;
import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingSteg;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.OpprettEtterlysningTask;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.uttalelse.RegisterinntektUttalelseTjeneste;
import no.nav.ung.sak.ytelse.KontrollerteInntektperioderTjeneste;
import no.nav.ung.sak.ytelse.RapportertInntektMapper;
import no.nav.ung.sak.ytelse.RapporterteInntekter;

@ApplicationScoped
@BehandlingStegRef(value = KONTROLLER_REGISTER_INNTEKT)
@BehandlingTypeRef
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
public class KontrollerInntektSteg implements BehandlingSteg {

    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;
    private RapportertInntektMapper rapportertInntektMapper;
    private RegisterinntektUttalelseTjeneste registerinntektUttalelseTjeneste;
    private KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste;
    private BehandlingRepository behandlingRepository;
    private EtterlysningRepository etterlysningRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private ProsessTaskTjeneste prosessTaskTjeneste;


    @Inject
    public KontrollerInntektSteg(ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder,
                                 RapportertInntektMapper rapportertInntektMapper,
                                 RegisterinntektUttalelseTjeneste registerinntektUttalelseTjeneste,
                                 KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste,
                                 BehandlingRepository behandlingRepository,
                                 EtterlysningRepository etterlysningRepository,
                                 InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                 ProsessTaskTjeneste prosessTaskTjeneste) {
        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
        this.rapportertInntektMapper = rapportertInntektMapper;
        this.registerinntektUttalelseTjeneste = registerinntektUttalelseTjeneste;
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
        final var rapporterteInntekterTidslinje = rapportertInntektMapper.mapAlleGjeldendeRegisterOgBrukersInntekter(behandlingId);
        final var prosessTriggerTidslinje = prosessTriggerPeriodeUtleder.utledTidslinje(behandlingId);
        final var uttalelser = registerinntektUttalelseTjeneste.hentUttalelser(behandlingId);
        final var registerinntekterForIkkeGodkjentUttalelse = rapportertInntektMapper.finnRegisterinntekterForUttalelse(behandlingId, uttalelser);
        final var kontrollResultat = KontrollerInntektTjeneste.utførKontroll(prosessTriggerTidslinje, rapporterteInntekterTidslinje, registerinntekterForIkkeGodkjentUttalelse);

//        return switch (kontrollResultat) {
//            case BRUK_INNTEKT_FRA_BRUKER -> {
//                kontrollerteInntektperioderTjeneste.opprettKontrollerteInntekterPerioderFraBruker(
//                    behandlingId,
//                    rapporterteInntekterTidslinje.mapValue(RapporterteInntekter::brukerRapporterteInntekter), prosessTriggerTidslinje);
//                yield BehandleStegResultat.utførtUtenAksjonspunkter();
//            }
//            case OPPRETT_AKSJONSPUNKT ->
//                BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.KONTROLLER_INNTEKT));
//            case SETT_PÅ_VENT_TIL_RAPPORTERINGSFRIST -> BehandleStegResultat.utførtMedAksjonspunktResultater(
//                AksjonspunktResultat.opprettForAksjonspunktMedFrist(
//                    AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_RAPPORTERINGSFRIST,
//                    Venteårsak.VENT_INNTEKT_RAPPORTERINGSFRIST,
//                    utledVentefrist(prosessTriggerTidslinje)));
//            case OPPRETT_OPPGAVE_TIL_BRUKER -> {
//
//                etterlysInntektskontrollUttalelse(behandlingId, rapporterteInntekterTidslinje);
//
//                yield BehandleStegResultat.utførtMedAksjonspunktResultater(
//                    AksjonspunktResultat.opprettForAksjonspunktMedFrist(
//                        AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE,
//                        Venteårsak.VENTER_PÅ_ETTERLYST_INNTEKT_UTTALELSE,
//                        finnEksisterendeFrist(behandlingId)));
//
//            }
//            case OPPRETT_OPPGAVE_TIL_BRUKER_MED_NY_FRIST ->
//                BehandleStegResultat.utførtMedAksjonspunktResultater(AksjonspunktResultat.opprettForAksjonspunktMedFrist(AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE, Venteårsak.VENTER_PÅ_ETTERLYST_INNTEKT_UTTALELSE, LocalDateTime.now().plusDays(14)));
//        };
        return null;

    }

    private void etterlysInntektskontrollUttalelse(Long behandlingId, LocalDateTimeline<RapporterteInntekter> rapporterteInntekterTidslinje) {

        UUID bestillingsId = UUID.randomUUID();
        Etterlysning nyEtterlysning = etterlysningRepository.lagre(
            Etterlysning.forInntektKontrollUttalelse(behandlingId,
                inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId).getEksternReferanse(),
                bestillingsId,
                DatoIntervallEntitet.fra(rapporterteInntekterTidslinje.getMinLocalDate(), rapporterteInntekterTidslinje.getMaxLocalDate())));

        var prosessTaskData = ProsessTaskData.forProsessTask(OpprettEtterlysningTask.class);
        prosessTaskData.setProperty(OpprettEtterlysningTask.ETTERLYSNING_ID, nyEtterlysning.getId().toString());
        prosessTaskTjeneste.lagre(prosessTaskData);

    }

    private LocalDateTime finnEksisterendeFrist(Long behandlingId) {
        final var behandling = behandlingRepository.hentBehandling(behandlingId);
        return behandling.getAksjonspunktFor(AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE).getFristTid();
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
