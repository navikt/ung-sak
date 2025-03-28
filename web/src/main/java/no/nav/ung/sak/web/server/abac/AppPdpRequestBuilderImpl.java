package no.nav.ung.sak.web.server.abac;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.konfigurasjon.env.Cluster;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.log.mdc.MdcExtendedLogContext;
import no.nav.k9.felles.sikkerhet.abac.AbacAttributtSamling;
import no.nav.k9.felles.sikkerhet.abac.PdpKlient;
import no.nav.k9.felles.sikkerhet.abac.PdpRequest;
import no.nav.k9.felles.sikkerhet.abac.PdpRequestBuilder;
import no.nav.ung.abac.BeskyttetRessursKoder;
import no.nav.ung.sak.behandlingslager.pip.PipBehandlingsData;
import no.nav.ung.sak.behandlingslager.pip.PipRepository;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.sikkerhet.abac.AppAbacAttributtType;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.typer.Saksnummer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Dependent
@Alternative
@Priority(2)
public class AppPdpRequestBuilderImpl implements PdpRequestBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(AppPdpRequestBuilderImpl.class);
    public static final String ABAC_DOMAIN = "k9";
    private static final Cluster CLUSTER = Environment.current().getCluster();
    private static final List<String> INTERNAL_CLUSTER_NAMESPACE = List.of(
        CLUSTER.clusterName() + ":k9saksbehandling",
        CLUSTER.DEV_GCP.clusterName() + ":omsorgspenger",
        CLUSTER.PROD_GCP.clusterName() + ":omsorgspenger",
        CLUSTER.DEV_GCP.clusterName() + ":dusseldorf",
        CLUSTER.PROD_GCP.clusterName() + ":dusseldorf"
    );
    private static final MdcExtendedLogContext LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess"); //$NON-NLS-1$
    private PipRepository pipRepository;
    private AktørTjeneste aktørTjeneste;



    public AppPdpRequestBuilderImpl() {
    }

    @Inject
    public AppPdpRequestBuilderImpl(PipRepository pipRepository, AktørTjeneste aktørTjeneste) {
        this.pipRepository = pipRepository;
        this.aktørTjeneste = aktørTjeneste;
    }

    private static void validerSamsvarBehandlingOgFagsak(Long behandlingId, Long fagsakId, Set<Long> fagsakIder) {
        List<Long> fagsakerSomIkkeErForventet = fagsakIder.stream()
            .filter(f -> !fagsakId.equals(f))
            .collect(Collectors.toList());
        if (!fagsakerSomIkkeErForventet.isEmpty()) {
            throw FeilFactory.create(PdpRequestBuilderFeil.class).ugyldigInputManglerSamsvarBehandlingFagsak(behandlingId, fagsakerSomIkkeErForventet)
                .toException();
        }
    }

    @Override
    public PdpRequest lagPdpRequest(AbacAttributtSamling attributter) {
        if (BeskyttetRessursKoder.PIP.equals(attributter.getResource())) {
            //lager dummy PDP request, siden
            // 1. det er unødvendig å lage en ordentlig, da det uansett ikke kan gjøres sjekk i PDP for request mot PIP-tjenesten (siden den brukes fra PDP-en) (se også PepImpl) tilgang istedet er styrt med tilgangslister i applikasjonen
            // 2. PDP requesten brukes også til logging, men ikke for servicebrukere (og det er kun servicebrukere som får kalle PIP-tjenesten)
            // 3. å lage en ekte PDP-request tar tid siden det gjør oppslag mot databasen for å hente ut data
            return lagPdpRequest(attributter, Collections.emptySet(), Collections.emptySet());
        }

        Optional<Long> behandlingId = utledBehandlingIder(attributter);
        Optional<PipBehandlingsData> behandlingData = behandlingId.isPresent()
            ? pipRepository.hentDataForBehandling(behandlingId.get())
            : Optional.empty();
        Set<Long> fagsakIder = behandlingData.isPresent()
            ? utledFagsakIder(attributter, behandlingData.get())
            : utledFagsakIder(attributter);

        behandlingData.ifPresent(pipBehandlingsData -> {
            validerSamsvarBehandlingOgFagsak(behandlingId.get(), pipBehandlingsData.getFagsakId(), fagsakIder);
            LOG_CONTEXT.add("behandling", behandlingId.get());
            LOG_CONTEXT.add("saksnummer", pipBehandlingsData.getSaksnummer());
        });

        Set<Saksnummer> saksnummere = attributter.getVerdier(AppAbacAttributtType.SAKSNUMMER);
        if (saksnummere != null && !saksnummere.isEmpty() && behandlingData.isEmpty()) {
            LOG_CONTEXT.add("saksnummer", saksnummere.size() == 1 ? saksnummere.iterator().next().toString() : saksnummere.toString());
        }

        if (!fagsakIder.isEmpty()) {
            LOG_CONTEXT.add("fagsak", fagsakIder.size() == 1 ? fagsakIder.iterator().next().toString() : fagsakIder.toString());
        }

        Set<AktørId> aktørIder = utledAktørIder(attributter, fagsakIder);
        Set<String> aksjonspunktType = pipRepository.hentAksjonspunktTypeForAksjonspunktKoder(attributter.getVerdier(AppAbacAttributtType.AKSJONSPUNKT_KODE));
        return behandlingData.isPresent()
            ? lagPdpRequest(attributter, aktørIder, aksjonspunktType, behandlingData.get())
            : lagPdpRequest(attributter, aktørIder, aksjonspunktType);
    }


    @Override
    public boolean internAzureConsumer(String azpName) {
        var match = INTERNAL_CLUSTER_NAMESPACE.stream().anyMatch(azpName::startsWith);
        if (!match) {
            LOG.warn("App fra ikke-godkjent namespace har etterspurt tilgang: " + azpName);
        }
        return match;
    }

    private PdpRequest lagPdpRequest(AbacAttributtSamling attributter, Set<AktørId> aktørIder, Collection<String> aksjonspunktType) {
        Set<String> aktører = aktørIder == null ? Collections.emptySet()
            : aktørIder.stream().map(AktørId::getId).collect(Collectors.toCollection(TreeSet::new));
        Set<String> fnrs = attributter.getVerdier(AppAbacAttributtType.FNR);

        PdpRequest pdpRequest = new PdpRequest();
        pdpRequest.put(AbacAttributter.RESOURCE_FELLES_DOMENE, ABAC_DOMAIN);
        pdpRequest.put(PdpKlient.ENVIRONMENT_AUTH_TOKEN, attributter.getIdToken());

        addToPdpRequest(pdpRequest, AbacAttributter.XACML_1_0_ACTION_ACTION_ID, attributter.getActionType().getEksternKode());
        addToPdpRequest(pdpRequest, AbacAttributter.RESOURCE_FELLES_RESOURCE_TYPE, attributter.getResource());

        if (!aktører.isEmpty()) {
            pdpRequest.put(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, aktører);
        }
        if (!fnrs.isEmpty()) {
            pdpRequest.put(AbacAttributter.RESOURCE_FELLES_PERSON_FNR, fnrs);
        }
        if (!aksjonspunktType.isEmpty()) {
            pdpRequest.put(AbacAttributter.RESOURCE_AKSJONSPUNKT_TYPE, aksjonspunktType);
        }
        return pdpRequest;
    }

    private void addToPdpRequest(PdpRequest pdpRequest, String key, String val) {
        String v = val == null || (val = val.trim()).isEmpty() ? null : val;
        pdpRequest.put(key, Objects.requireNonNull(v, "Fikk null eller empty verdi for : " + key));
    }

    private PdpRequest lagPdpRequest(AbacAttributtSamling attributter, Set<AktørId> aktørIder, Collection<String> aksjonspunktType,
                                     PipBehandlingsData behandlingData) {
        PdpRequest pdpRequest = lagPdpRequest(attributter, aktørIder, aksjonspunktType);
        AbacUtil.oversettBehandlingStatus(behandlingData.getBehandligStatus())
            .ifPresent(it -> pdpRequest.put(AbacAttributter.RESOURCE_BEHANDLINGSSTATUS, it.getEksternKode()));
        AbacUtil.oversettFagstatus(behandlingData.getFagsakStatus())
            .ifPresent(it -> pdpRequest.put(AbacAttributter.RESOURCE_SAKSSTATUS, it.getEksternKode()));
        behandlingData.getAnsvarligSaksbehandler()
            .ifPresent(it -> pdpRequest.put(AbacAttributter.RESOURCE_ANSVARLIG_SAKSBEHANDLER, it));
        return pdpRequest;
    }

    private Optional<Long> utledBehandlingIder(AbacAttributtSamling attributter) {
        Set<UUID> uuids = attributter.getVerdier(AppAbacAttributtType.BEHANDLING_UUID);
        Set<Long> behandlingIdVerdier = attributter.getVerdier(AppAbacAttributtType.BEHANDLING_ID);
        Set<Long> behandlingId0 = behandlingIdVerdier.stream().mapToLong(Long::valueOf).boxed().collect(Collectors.toSet());

        Set<Long> behandlingsIder = new LinkedHashSet<>(behandlingId0);
        behandlingsIder.addAll(pipRepository.behandlingsIdForUuid(uuids));
        behandlingsIder.addAll(pipRepository.behandlingsIdForOppgaveId(attributter.getVerdier(AppAbacAttributtType.OPPGAVE_ID)));

        if (behandlingsIder.isEmpty()) {
            return Optional.empty();
        } else if (behandlingsIder.size() == 1) {
            return Optional.of(behandlingsIder.iterator().next());
        }

        // Støtter p.t. kun en behandlingsid
        throw FeilFactory.create(PdpRequestBuilderFeil.class).ugyldigInputFlereBehandlingIder(behandlingsIder).toException();
    }

    private Set<Long> utledFagsakIder(AbacAttributtSamling attributter, PipBehandlingsData behandlingData) {
        Set<Long> fagsaker = utledFagsakIder(attributter);
        fagsaker.add(behandlingData.getFagsakId());
        return fagsaker;
    }

    private Set<Long> utledFagsakIder(AbacAttributtSamling attributter) {
        Set<Long> fagsakIder = new HashSet<>();
        fagsakIder.addAll(attributter.getVerdier(AppAbacAttributtType.FAGSAK_ID));

        //
        fagsakIder.addAll(pipRepository.fagsakIderForSøker(tilAktørId(attributter.getVerdier(AppAbacAttributtType.SAKER_MED_FNR))));

        // fra saksnummer
        Set<Saksnummer> saksnummere = attributter.getVerdier(AppAbacAttributtType.SAKSNUMMER);
        fagsakIder.addAll(pipRepository.fagsakIdForSaksnummer(saksnummere));

        // journalpostIder
        Set<JournalpostId> journalpostIder = attributter.getVerdier(AppAbacAttributtType.JOURNALPOST_ID);
        fagsakIder.addAll(pipRepository.fagsakIdForJournalpostId(journalpostIder));

        return fagsakIder;
    }

    private Set<AktørId> utledAktørIder(AbacAttributtSamling attributter, Set<Long> fagsakIder) {
        Set<String> aktørIdVerdier = attributter.getVerdier(AppAbacAttributtType.AKTØR_ID);

        Set<AktørId> aktørIder = new HashSet<>();
        aktørIder.addAll(aktørIdVerdier.stream().map(AktørId::new).collect(Collectors.toSet()));
        aktørIder.addAll(pipRepository.hentAktørIdKnyttetTilFagsaker(fagsakIder));
        return aktørIder;
    }

    private Collection<AktørId> tilAktørId(Set<String> fnr) {
        if (fnr == null || fnr.isEmpty()) {
            return Collections.emptySet();
        }
        return aktørTjeneste.hentAktørIdForPersonIdentSet(fnr.stream().map(PersonIdent::new).collect(Collectors.toSet()));
    }
}
