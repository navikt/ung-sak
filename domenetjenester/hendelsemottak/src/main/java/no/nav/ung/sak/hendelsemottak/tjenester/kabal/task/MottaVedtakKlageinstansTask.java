package no.nav.ung.sak.hendelsemottak.tjenester.kabal.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.klage.KlageMedholdÅrsak;
import no.nav.ung.kodeverk.klage.KlageVurderingOmgjør;
import no.nav.ung.kodeverk.klage.KlageVurderingType;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageRepository;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageVurderingAdapter;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.hendelsemottak.tjenester.kabal.kontrakt.KabalBehandlingEvent;
import no.nav.ung.sak.kontrakt.klage.KlageresultatEndretEvent;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveAnkebehandlingAvsluttetTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;


@ApplicationScoped
@ProsessTask(MottaVedtakKlageinstansTask.TASKNAME)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class MottaVedtakKlageinstansTask implements ProsessTaskHandler {

    public static final String TASKNAME = "hendelsemottak.mottaVedtakKlageinstans";
    public static final String UTFALL = "utfall";
    public static final String KABAL_EVENTTYPE = "eventType";
    public static final String KABAL_REFERANSE = "kabalReferanse";
    public static final String FEILREGISTRERING_BEGRUNNELSE = "feilregistreringBegrunnelse";
    private static final Logger log = LoggerFactory.getLogger(MottaVedtakKlageinstansTask.class);

    private BehandlingRepository behandlingRepository;
    private KlageRepository klageRepository;
    private ProsessTaskTjeneste prosessTaskRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private Event<KlageresultatEndretEvent> klageresultatEndretEvent;

    MottaVedtakKlageinstansTask() {
    }

    @Inject
    public MottaVedtakKlageinstansTask(BehandlingRepository behandlingRepository,
                                       KlageRepository klageRepository,
                                       ProsessTaskTjeneste prosessTaskRepository,
                                       BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                       Event<KlageresultatEndretEvent> klageresultatEndretEvent) {
        this.behandlingRepository = behandlingRepository;
        this.klageRepository = klageRepository;
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.klageresultatEndretEvent = klageresultatEndretEvent;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        log.info("Kjører task: {}", TASKNAME);

        var behandlingsId = prosessTaskData.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingsId);

        if (prosessTaskData.getPropertyValue(KABAL_EVENTTYPE).equals(KabalBehandlingEvent.Eventtyper.ANKEBEHANDLING_AVSLUTTET)) {
            if(!behandling.erAvsluttet()) {
                log.warn("Ankebehandling avsluttet for behandling {} som ikke er avsluttet", behandlingsId);
            }
            opprettAnkebehandlingAvsluttetTask(behandling, prosessTaskData.getPropertyValue(UTFALL));
        } else {
            if (behandling.getAktivtBehandlingSteg() != BehandlingStegType.OVERFØRT_NK) {
                log.warn("Mottok hendelse fra kabal for vedtak mens behandling står i steg={}, forventet steg={}",
                    behandling.getAktivtBehandlingSteg(), BehandlingStegType.OVERFØRT_NK);
                return;
            }

            var klageUtredning = klageRepository.hentKlageUtredning(behandling.getId());
            var klagevurderingNK = klageUtredning.hentKlagevurdering(KlageVurdertAv.KLAGEINSTANS);
            if (klagevurderingNK.isPresent()) {
                throw new IllegalStateException("Forsokte oppdatere klageresultat fra kabal men klageresultatet var allerede satt till: "
                    + klageUtredning.getKlageVurderingType(KlageVurdertAv.KLAGEINSTANS).map(KlageVurderingType::getNavn).get());
            }

            log.info("Tar behandling som er overført til NK av vent");
            KlageVurderingAdapter klageVurderingAdapter = lagKlagevurdering(prosessTaskData);
            klageUtredning.setKlagevurdering(klageVurderingAdapter);

            klageRepository.lagre(klageUtredning);
            klageresultatEndretEvent.fire(new KlageresultatEndretEvent(behandling.getId()));

            behandlingProsesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling, false);
        }
    }

    private KlageVurderingAdapter lagKlagevurdering(ProsessTaskData prosessTaskData) {
        String utfall = prosessTaskData.getPropertyValue(UTFALL);
        String kabalReferanse = prosessTaskData.getPropertyValue(KABAL_REFERANSE);

        if (utfall != null) {
            var klageVurdering = mapKabalUtfallTilVurdering(utfall);
            var klageVurderingOmjør = mapKabalUtfallTilVurderingOmgjør(utfall);

            return new KlageVurderingAdapter(
                klageVurdering,
                KlageMedholdÅrsak.UDEFINERT,
                klageVurderingOmjør,
                "Mottatt av Kabal",
                null,
                null,
                kabalReferanse,
                KlageVurdertAv.KLAGEINSTANS);
        }

        String eventType = prosessTaskData.getPropertyValue(KABAL_EVENTTYPE);
        if (eventType.equals(KabalBehandlingEvent.Eventtyper.BEHANDLING_FEILREGISTRERT)) {
            var begrunnelse = prosessTaskData.getPropertyValue(FEILREGISTRERING_BEGRUNNELSE);
            Objects.requireNonNull(begrunnelse, "begrunnelse for feilregistrering må være satt");

            return new KlageVurderingAdapter(
                KlageVurderingType.FEILREGISTRERT,
                KlageMedholdÅrsak.UDEFINERT,
                KlageVurderingOmgjør.UDEFINERT,
                begrunnelse,
                null,
                null,
                kabalReferanse,
                KlageVurdertAv.KLAGEINSTANS);

        }

        throw new IllegalStateException("Event fra kabal kunne ikke mappes til en klagevurdering. Eventtype=%s".formatted(eventType));
    }

    private KlageVurderingType mapKabalUtfallTilVurdering(String utfall) {
        return switch (utfall) {
            case "RETUR" -> KlageVurderingType.HJEMSENDE_UTEN_Å_OPPHEVE;
            case "TRUKKET" -> KlageVurderingType.TRUKKET;
            case "OPPHEVET" -> KlageVurderingType.OPPHEVE_YTELSESVEDTAK;
            case "MEDHOLD", "UGUNST", "DELVIS_MEDHOLD" -> KlageVurderingType.MEDHOLD_I_KLAGE;
            case "OPPRETTHOLDT", "STADFESTELSE" -> KlageVurderingType.STADFESTE_YTELSESVEDTAK;
            case "AVVIST" -> KlageVurderingType.AVVIS_KLAGE;
            default -> throw new IllegalArgumentException("Kabal publiserte ukjent utfall=" + utfall);
        };
    }

    private KlageVurderingOmgjør mapKabalUtfallTilVurderingOmgjør(String utfall) {
        return switch (utfall) {
            case "DELVIS_MEDHOLD" -> KlageVurderingOmgjør.DELVIS_MEDHOLD_I_KLAGE;
            case "MEDHOLD" -> KlageVurderingOmgjør.GUNST_MEDHOLD_I_KLAGE;
            case "UGUNST" -> KlageVurderingOmgjør.UGUNST_MEDHOLD_I_KLAGE;
            default -> KlageVurderingOmgjør.UDEFINERT;
        };
    }

    private void opprettAnkebehandlingAvsluttetTask(no.nav.ung.sak.behandlingslager.behandling.Behandling behandling, String utfall) {
        ProsessTaskData opprettOppgaveAnkebehandlingAvsluttetTask = ProsessTaskData.forProsessTask(OpprettOppgaveAnkebehandlingAvsluttetTask.class);
        opprettOppgaveAnkebehandlingAvsluttetTask.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        opprettOppgaveAnkebehandlingAvsluttetTask.setSekvens("1");
        opprettOppgaveAnkebehandlingAvsluttetTask.setPrioritet(100);
        opprettOppgaveAnkebehandlingAvsluttetTask.setProperty(OpprettOppgaveAnkebehandlingAvsluttetTask.UTFALL, utfall);

        opprettOppgaveAnkebehandlingAvsluttetTask.setCallIdFraEksisterende();

        prosessTaskRepository.lagre(opprettOppgaveAnkebehandlingAvsluttetTask);
    }

}
