package no.nav.k9.sak.innsyn.hendelse;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.innsyn.InnsynHendelse;
import no.nav.k9.innsyn.sak.Aksjonspunkt;
import no.nav.k9.innsyn.sak.BehandlingResultat;
import no.nav.k9.innsyn.sak.SøknadInfo;
import no.nav.k9.innsyn.sak.SøknadStatus;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandling.saksbehandlingstid.SaksbehandlingsfristUtleder;
import no.nav.k9.sak.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.k9.sak.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.person.personopplysning.UtlandVurdererTjeneste;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapperKodeverdiSerializer;
import no.nav.k9.sak.innsyn.BrukerdialoginnsynMeldingProducer;
import no.nav.k9.søknad.felles.Kildesystem;

@ApplicationScoped
public class InnsynEventObserver {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private ProsessTaskTjeneste prosessTaskRepository;
    private BehandlingRepository behandlingRepository;
    private BrukerdialoginnsynMeldingProducer producer;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private Instance<SaksbehandlingsfristUtleder> fristUtledere;
    private boolean enable;
    private UtlandVurdererTjeneste utlandVurdererTjeneste;

    public InnsynEventObserver() {
    }

    @Inject
    public InnsynEventObserver(ProsessTaskTjeneste prosessTaskRepository,
                               BehandlingRepository behandlingRepository,
                               Instance<SaksbehandlingsfristUtleder> fristUtledere,
                               BrukerdialoginnsynMeldingProducer producer,
                               MottatteDokumentRepository mottatteDokumentRepository,
                               @KonfigVerdi(value = "ENABLE_INNSYN_OBSERVER", defaultVerdi = "false") boolean enable,
                               UtlandVurdererTjeneste utlandVurdererTjeneste) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlingRepository = behandlingRepository;
        this.producer = producer;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.fristUtledere = fristUtledere;
        this.enable = enable;
        this.utlandVurdererTjeneste = utlandVurdererTjeneste;
    }


    public void observerBehandlingStartet(@Observes BehandlingStatusEvent event) {
        if (!enable) {
            return;
        }

        if ((event.getGammelStatus() == BehandlingStatus.OPPRETTET || event.getGammelStatus() == null)
            && event.getNyStatus() == BehandlingStatus.UTREDES) {
            log.info("Publiserer melding til brukerdialog for behandling startet");
            notifyInnsyn(event.getBehandlingId());
        }
    }

    public void observerBehandlingAvsluttetEvent(@Observes BehandlingStatusEvent.BehandlingAvsluttetEvent event)  {
        log.info("Publiserer melding til brukerdialog for behandling avsluttet");
        notifyInnsyn(event.getBehandlingId());
    }

    public void observerAksjonspunkterFunnetEvent(@Observes AksjonspunktStatusEvent event) {
        log.info("Publiserer melding til brukerdialog for aksjonspunkt");
        notifyInnsyn(event.getBehandlingId());
    }



    private void debugObservasjon(AksjonspunktStatusEvent event) {
        var collect = event.getAksjonspunkter().stream()
            .filter(it -> it.erÅpentAksjonspunkt())
            .map(it -> it.getAksjonspunktDefinisjon().getKode() +":"+ it.getVenteårsak().getKode())
            .collect(Collectors.toSet());
        String join = String.join(", ", collect);
        log.info("################ -> Steg {} aksjonspunkter {}", event.getBehandlingStegType(), join);
    }

    private void notifyInnsyn(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        Fagsak fagsak = behandling.getFagsak();
        if (fagsak.getYtelseType() != FagsakYtelseType.PSB) {
            return;
        }



        String saksnummer = fagsak.getSaksnummer().getVerdi();
        Set<Aksjonspunkt> aksjonspunkter = mapAksjonspunkter(behandling);
        var behandlingInnsyn = new no.nav.k9.innsyn.sak.Behandling(
            behandling.getUuid(),
            behandling.getOpprettetDato().atZone(ZoneId.systemDefault()),
            Optional.ofNullable(behandling.getAvsluttetDato()).map(it -> it.atZone(ZoneId.systemDefault())).orElse(null),
            mapBehandlingResultat(behandling.getBehandlingResultatType()),
            mapBehandingStatus(behandling, aksjonspunkter),
            mapSøknader(behandling),
            aksjonspunkter,
            utlandVurdererTjeneste.erUtenlandssak(behandling),
            mapFagsak(fagsak)
        );

        String json = deserialiser(new InnsynHendelse<>(ZonedDateTime.now(), behandlingInnsyn));

        producer.send(saksnummer, json);
    }


    private Set<Aksjonspunkt> mapAksjonspunkter(Behandling b) {
        return b.getÅpneAksjonspunkter().stream()
            .map(this::mapRelevanteAksjonspunkterPåVent)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());
    }


    private Optional<Aksjonspunkt> mapRelevanteAksjonspunkterPåVent(no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt it) {
        var venteårsak = it.getVenteårsak();
        if (venteårsak == null) {
            return Optional.empty();
        }

        var frist = it.getFristTid() != null ? it.getFristTid().atZone(ZoneId.systemDefault()) : null;

        return switch (venteårsak) {
            case LEGEERKLÆRING, MEDISINSKE_OPPLYSNINGER -> lagAksjonspunktForInnsyn(frist, Aksjonspunkt.Venteårsak.MEDISINSK_DOKUMENTASJON);
            case INNTEKTSMELDING, VENTER_PÅ_ETTERLYST_INNTEKTSMELDINGER, VENTER_PÅ_ETTERLYST_INNTEKTSMELDINGER_MED_VARSEL ->
                lagAksjonspunktForInnsyn(frist, Aksjonspunkt.Venteårsak.INNTEKTSMELDING);
            case FOR_TIDLIG_SOKNAD -> lagAksjonspunktForInnsyn(frist, Aksjonspunkt.Venteårsak.FOR_TIDLIG_SOKNAD);
            case VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT -> lagAksjonspunktForInnsyn(frist, Aksjonspunkt.Venteårsak.MELDEKORT);
            default -> Optional.empty();
        };
    }

    private static Optional<Aksjonspunkt> lagAksjonspunktForInnsyn(ZonedDateTime frist, Aksjonspunkt.Venteårsak venteårsak) {
        return Optional.of(new Aksjonspunkt(venteårsak, frist));
    }


    private no.nav.k9.innsyn.sak.Fagsak mapFagsak(Fagsak fagsak) {
        return new no.nav.k9.innsyn.sak.Fagsak(fagsak.getSaksnummer(), fagsak.getAktørId(), fagsak.getPleietrengendeAktørId(), fagsak.getYtelseType());
    }

    private Set<SøknadInfo> mapSøknader(Behandling b) {
        List<MottattDokument> mottattDokuments = mottatteDokumentRepository.hentMottatteDokumentForBehandling(b.getFagsakId(), b.getId(), List.of(Brevkode.PLEIEPENGER_BARN_SOKNAD), false, DokumentStatus.BEHANDLER, DokumentStatus.GYLDIG, DokumentStatus.MOTTATT);
        return mottattDokuments.stream()
            .map(it -> new SøknadInfo(
                SøknadStatus.MOTTATT,
                it.getJournalpostId().getVerdi(),
                it.getMottattTidspunkt().atZone(ZoneId.systemDefault()),
                Optional.ofNullable(it.getKildesystem()).map(Kildesystem::of).orElse(null)
                ))
            .collect(Collectors.toSet());
    }

    private no.nav.k9.innsyn.sak.BehandlingStatus mapBehandingStatus(Behandling behandling, Set<Aksjonspunkt> aksjonspunkter) {
        if (!aksjonspunkter.isEmpty()) {
            return no.nav.k9.innsyn.sak.BehandlingStatus.PÅ_VENT;
        }
        if (behandling.getStatus() == BehandlingStatus.AVSLUTTET) {
            return no.nav.k9.innsyn.sak.BehandlingStatus.AVSLUTTET;
        }
        return no.nav.k9.innsyn.sak.BehandlingStatus.UNDER_BEHANDLING;
    }
    private BehandlingResultat mapBehandlingResultat(BehandlingResultatType behandlingResultatType) {
        if (behandlingResultatType.erHenleggelse()) {
            return BehandlingResultat.HENLAGT;
        }

        if (behandlingResultatType == BehandlingResultatType.AVSLÅTT) {
            return BehandlingResultat.AVSLÅTT;
        }

        if (behandlingResultatType == BehandlingResultatType.INNVILGET) {
            return BehandlingResultat.INNVILGET;
        }


        if (behandlingResultatType == BehandlingResultatType.DELVIS_INNVILGET) {
            return BehandlingResultat.DELVIS_INNVILGET;
        }

        return null;
    }

    private static String deserialiser(InnsynHendelse<?> behandling) {
        String json;
        try {
            json = JsonObjectMapperKodeverdiSerializer.getJson(behandling);
        } catch (IOException e) {
            throw new IllegalArgumentException("Feilet ved deserialisering", e);
        }
        return json;
    }


}
