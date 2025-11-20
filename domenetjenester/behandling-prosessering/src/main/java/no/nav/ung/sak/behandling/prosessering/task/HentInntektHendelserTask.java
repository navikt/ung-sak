package no.nav.ung.sak.behandling.prosessering.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.domene.registerinnhenting.InntektAbonnentTjeneste;
import no.nav.ung.sak.typer.AktørId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
@ProsessTask(value = HentInntektHendelserTask.TASKTYPE)
public class HentInntektHendelserTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "registerinnhenting.hentInntektHendelser";
    public static final String SEKVENSNUMMER_KEY = "sekvensnummer";


    private static final Logger log = LoggerFactory.getLogger(HentInntektHendelserTask.class);

    private InntektAbonnentTjeneste inntektAbonnentTjeneste;
    private EntityManager entityManager;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private boolean hentInntektHendelserEnabled;
    private boolean oppfriskKontrollbehandlingEnabled;
    private Duration ventetidFørNesteKjøring;
    private

    public HentInntektHendelserTask() {
        // For CDI
    }

    @Inject
    public HentInntektHendelserTask(InntektAbonnentTjeneste inntektAbonnentTjeneste,
                                    EntityManager entityManager,
                                    ProsessTaskTjeneste prosessTaskTjeneste,
                                    @KonfigVerdi(value = "HENT_INNTEKT_HENDELSER_ENABLED", required = false, defaultVerdi = "false") boolean hentInntektHendelserEnabled,
                                    @KonfigVerdi(value = "HENT_INNTEKT_HENDElSER_INTERVALl", required = false, defaultVerdi = "PT1M") String ventetidFørNesteKjøring,
                                    @KonfigVerdi(value = "OPPFRISK_KONTROLLBEHANDLING_ENABLED", required = false, defaultVerdi = "false") boolean oppfriskKontrollbehandlingEnabled){
        this.inntektAbonnentTjeneste = inntektAbonnentTjeneste;
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.hentInntektHendelserEnabled = hentInntektHendelserEnabled;
        this.oppfriskKontrollbehandlingEnabled = oppfriskKontrollbehandlingEnabled;
        this.ventetidFørNesteKjøring = Duration.parse(ventetidFørNesteKjøring);
    }


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        if (!hentInntektHendelserEnabled) {
            log.info("Henting av inntektshendelser er deaktivert. Hopper over task.");
            return;
        }

        if (oppfriskKontrollbehandlingEnabled) {
            log.info("Oppfrisk Task av kontrollbehandlinger er aktivert. Hopper over task for henting av inntektshendelser.");
            return;
        }

        long sekvensnummer = hentEllerInitialiserSekvensnummer(prosessTaskData);
        log.info("Starter henting av inntektshendelser fra sekvensnummer={}", sekvensnummer);

        var nyeInntektHendelser = inntektAbonnentTjeneste.hentNyeInntektHendelser(sekvensnummer).toList();

        if (nyeInntektHendelser.isEmpty()) {
            log.info("Ingen nye inntektshendelser funnet");
            opprettNesteTask(sekvensnummer + 1);
            return;
        }

        log.info("Hentet {} nye inntektshendelser", nyeInntektHendelser.size());

        var unikeAktørIder = nyeInntektHendelser.stream()
            .map(hendelse -> hendelse.aktørId())
            .collect(Collectors.toSet());

        log.info("Fant {} unike aktørIder i hendelsene", unikeAktørIder.size());

        opprettOppfriskTaskerForAktører(unikeAktørIder);

        var sisteSekvensnummer = nyeInntektHendelser.get(nyeInntektHendelser.size() - 1).sekvensnummer();
        opprettNesteTask(sisteSekvensnummer + 1);
    }

    private long hentEllerInitialiserSekvensnummer(ProsessTaskData prosessTaskData) {
        String sekvensnummerVerdi = prosessTaskData.getPropertyValue(SEKVENSNUMMER_KEY);

        if (sekvensnummerVerdi == null || sekvensnummerVerdi.equals("-1")) {
            log.info("Første kjøring av task for henting av inntektshendelser. Henter første sekvensnummer fra inntektskomponenten.");
            return inntektAbonnentTjeneste.hentFørsteSekvensnummer();
        }

        return Long.parseLong(sekvensnummerVerdi);
    }

    private void opprettOppfriskTaskerForAktører(Set<AktørId> aktørIder) {
        var oppfriskTasker = new ArrayList<ProsessTaskData>();

        for (AktørId aktørId : aktørIder) {
            var behandlinger = finnBehandlingerSomVenterPåInntektUttalelse(aktørId);

            if (behandlinger.isEmpty()) {
                continue;
            }

            for (Behandling behandling : behandlinger) {
                log.info("Oppretter oppfrisk-task for behandling={} saksnummer={}",
                    behandling.getId(), behandling.getFagsak().getSaksnummer().getVerdi());
                oppfriskTasker.add(OppfriskTask.create(behandling, true));
            }
        }

        if (oppfriskTasker.isEmpty()) {
            log.info("Ingen behandlinger funnet som skal oppfriskes");
            return;
        }

        var gruppe = new ProsessTaskGruppe();
        gruppe.addNesteParallell(oppfriskTasker);
        String gruppeId = prosessTaskTjeneste.lagre(gruppe);
        log.info("Lagret {} oppfrisk-tasker i taskgruppe [{}]", oppfriskTasker.size(), gruppeId);
    }

    private List<Behandling> finnBehandlingerSomVenterPåInntektUttalelse(AktørId aktørId) {
        TypedQuery<Behandling> query = entityManager.createQuery(
            "SELECT DISTINCT b FROM Behandling b " +
                "JOIN b.behandlingÅrsaker ba " +
                "JOIN b.aksjonspunkter ap " +
                "WHERE b.fagsak.brukerAktørId = :aktørId " +
                "AND b.status = :status " +
                "AND ba.behandlingÅrsakType = :behandlingArsakType " +
                "AND ap.aksjonspunktDefinisjon = :aksjonspunktDef " +
                "AND ap.status = :aksjonspunktStatus " +
                "AND ap.venteårsak = :ventearsak",
            Behandling.class);

        query.setParameter("aktørId", aktørId);
        query.setParameter("status", BehandlingStatus.UTREDES);
        query.setParameter("behandlingArsakType", BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT);
        query.setParameter("aksjonspunktDef", AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE);
        query.setParameter("aksjonspunktStatus", AksjonspunktStatus.OPPRETTET);
        query.setParameter("ventearsak", Venteårsak.VENTER_PÅ_ETTERLYST_INNTEKT_UTTALELSE);

        return query.getResultList();
    }

    private void opprettNesteTask(long nesteSekvensnummer) {
        var nesteTask = ProsessTaskData.forProsessTask(HentInntektHendelserTask.class);
        nesteTask.setNesteKjøringEtter(LocalDateTime.now().plus(ventetidFørNesteKjøring));
        nesteTask.setProperty(SEKVENSNUMMER_KEY, String.valueOf(nesteSekvensnummer));
        prosessTaskTjeneste.lagre(nesteTask);
        log.info("Opprettet neste task med sekvensnummer={}", nesteSekvensnummer);
    }
}
