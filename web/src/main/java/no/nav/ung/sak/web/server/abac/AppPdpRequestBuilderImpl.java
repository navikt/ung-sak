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
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.typer.Saksnummer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
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

    private static void validerSamsvarBehandlingOgFagsak(Long behandlingId, Saksnummer fagsakId, Set<Saksnummer> fagsakIder) {
        List<Saksnummer> fagsakerSomIkkeErForventet = fagsakIder.stream()
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
        behandlingId.ifPresent(it -> LOG_CONTEXT.add("behandling", it));

        Optional<PipBehandlingsData> behandlingData = behandlingId.isPresent()
            ? pipRepository.hentDataForBehandling(behandlingId.get())
            : Optional.empty();
        if (behandlingId.isPresent() && behandlingData.isEmpty()) {
            throw new UkjentBehandlingException(behandlingId.get());
        }
        Set<Saksnummer> saksnumre = behandlingData.isPresent()
            ? utledSaksnumre(attributter, behandlingData.get())
            : utledSaksnumre(attributter);

        behandlingData.ifPresent(pipBehandlingsData -> {
            validerSamsvarBehandlingOgFagsak(behandlingId.get(), new Saksnummer(pipBehandlingsData.getSaksnummer()), saksnumre);
            LOG_CONTEXT.add("behandlingUuid", pipBehandlingsData.getBehandlingUuid());
        });

        if (saksnumre.size() == 1) {
            LOG_CONTEXT.add("saksnummer", saksnumre.iterator().next());
        }

        Set<AktørId> aktørIder = utledAktørIder(attributter, saksnumre);
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
        Set<Long> behandlingIdFraAttributter = attributter.<Long>getVerdier(AppAbacAttributtType.BEHANDLING_ID).stream().mapToLong(Long::valueOf).boxed().collect(Collectors.toSet());

        Set<UUID> uuids = attributter.getVerdier(AppAbacAttributtType.BEHANDLING_UUID);
        Set<Long> behandlingIdForUuid = pipRepository.behandlingsIdForUuid(uuids);
        if (uuids.size() != behandlingIdForUuid.size()) {
            throw new UkjentBehandlingException(uuids);
        }

        Set<Long> behandlingsIder = new LinkedHashSet<>();
        behandlingsIder.addAll(behandlingIdFraAttributter);
        behandlingsIder.addAll(behandlingIdForUuid);
        behandlingsIder.addAll(pipRepository.behandlingsIdForOppgaveId(attributter.getVerdier(AppAbacAttributtType.OPPGAVE_ID)));

        if (behandlingsIder.isEmpty()) {
            return Optional.empty();
        } else if (behandlingsIder.size() == 1) {
            return Optional.of(behandlingsIder.iterator().next());
        }

        // Støtter p.t. kun en behandlingsid
        throw FeilFactory.create(PdpRequestBuilderFeil.class).ugyldigInputFlereBehandlingIder(behandlingsIder).toException();
    }

    private Set<Saksnummer> utledSaksnumre(AbacAttributtSamling attributter, PipBehandlingsData behandlingData) {
        Set<Saksnummer> saksnumre = utledSaksnumre(attributter);
        saksnumre.add(new Saksnummer(behandlingData.getSaksnummer()));
        return saksnumre;
    }

    private Set<Saksnummer> utledSaksnumre(AbacAttributtSamling attributter) {
        Set<Saksnummer> saksnumre = new LinkedHashSet<>();
        saksnumre.addAll(attributter.getVerdier(AppAbacAttributtType.SAKSNUMMER));
        saksnumre.addAll(hentSaksnumreForFagsakIder(attributter));
        saksnumre.addAll(pipRepository.saksnumreForSøker(tilAktørId(attributter.getVerdier(AppAbacAttributtType.SAKER_MED_FNR))));
        saksnumre.addAll(pipRepository.saksnumreForJournalpostId(attributter.getVerdier(AppAbacAttributtType.JOURNALPOST_ID)));
        return saksnumre;
    }

    private Set<Saksnummer> hentSaksnumreForFagsakIder(AbacAttributtSamling attributter) {
        Set<Long> fagsakIder = attributter.getVerdier(AppAbacAttributtType.FAGSAK_ID);
        Set<Saksnummer> saksnumre = pipRepository.saksnummerForFagsakId(fagsakIder);
        if (fagsakIder.size() != saksnumre.size()) {
            throw new UkjentFagsakException(fagsakIder);
        }
        return saksnumre;
    }

    private Set<AktørId> utledAktørIder(AbacAttributtSamling attributter, Set<Saksnummer> saksnumre) {
        Set<String> aktørIdVerdier = attributter.getVerdier(AppAbacAttributtType.AKTØR_ID);

        Set<AktørId> aktørIder = new HashSet<>();
        aktørIder.addAll(aktørIdVerdier.stream().map(AktørId::new).collect(Collectors.toSet()));
        aktørIder.addAll(pipRepository.hentAktørIdKnyttetTilFagsaker(saksnumre));
        return aktørIder;
    }

    private Collection<AktørId> tilAktørId(Set<String> fnr) {
        if (fnr == null || fnr.isEmpty()) {
            return Collections.emptySet();
        }
        return aktørTjeneste.hentAktørIdForPersonIdentSet(fnr.stream().map(PersonIdent::new).collect(Collectors.toSet()));
    }
}
