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
import no.nav.k9.felles.sikkerhet.abac.AbacAttributtType;
import no.nav.k9.felles.sikkerhet.abac.BerørtePersonerForAuditlogg;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.PdpRequest;
import no.nav.k9.felles.sikkerhet.abac.PdpRequestBuilder;
import no.nav.k9.felles.sikkerhet.abac.PdpRequestMedBerørtePersonerForAuditlogg;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.ung.abac.AppAbacAttributtType;
import no.nav.ung.sak.behandlingslager.pip.PipBehandlingsData;
import no.nav.ung.sak.behandlingslager.pip.PipRepository;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
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

    @Override
    public PdpRequest lagPdpRequest(AbacAttributtSamling attributter) {
        if (BeskyttetRessursResourceType.PIP == attributter.getResourceType()) {
            //lager dummy PDP request, siden
            // 1. det er unødvendig å lage en ordentlig, da det uansett ikke kan gjøres sjekk i PDP for request mot PIP-tjenesten (siden den brukes fra PDP-en) (se også PepImpl) tilgang istedet er styrt med tilgangslister i applikasjonen
            // 2. PDP requesten brukes også til logging, men ikke for servicebrukere (og det er kun servicebrukere som får kalle PIP-tjenesten)
            // 3. å lage en ekte PDP-request tar tid siden det gjør oppslag mot databasen for å hente ut data
            return lagPdpRequest(attributter, Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
        }


        validerAttributter(attributter);

        Long behandlingId = utledBehandlingIder(attributter).orElse(null);
        PipBehandlingsData behandlingData = null;

        if (behandlingId != null) {
            LOG_CONTEXT.add("behandling", behandlingId);
            behandlingData = pipRepository.hentDataForBehandling(behandlingId).orElseThrow(() -> new UkjentBehandlingException(behandlingId));;
            LOG_CONTEXT.add("behandlingUuid", behandlingData.behandlingUuid());
        }

        Set<Saksnummer> saksnumre = utledSaksnumre(attributter, behandlingData);
        validerSamsvarBehandlingOgFagsak(behandlingData, saksnumre);
        if (saksnumre.size() == 1) {
            LOG_CONTEXT.add("saksnummer", saksnumre.iterator().next());
        }

        Set<AktørId> aktørIder = utledAktørIder(attributter, saksnumre);
        Set<AktørId> aktørIderSporingslogg = utledAktørIderForSporingslogg(attributter, saksnumre);
        Set<String> aksjonspunktType = pipRepository.hentAksjonspunktTypeForAksjonspunktKoder(attributter.getVerdier(StandardAbacAttributtType.AKSJONSPUNKT_KODE));

        PdpRequest pdpRequest = lagPdpRequest(attributter, aktørIder, aktørIderSporingslogg, aksjonspunktType);
        if (behandlingData != null) {
            AbacUtil.oversettBehandlingStatus(behandlingData.behandligStatus())
                .ifPresent(it -> pdpRequest.setBehandlingStatusEksternKode(it.getEksternKode()));
            AbacUtil.oversettFagstatus(behandlingData.fagsakStatus())
                .ifPresent(it -> pdpRequest.setFagsakStatusEksternKode(it.getEksternKode()));
            if (behandlingData.ansvarligSaksbehandler() != null) {
                pdpRequest.setAnsvarligSaksbehandler(behandlingData.ansvarligSaksbehandler());
            }
        }
        return pdpRequest;
    }


    @Override
    public boolean internAzureConsumer(String azpName) {
        var match = INTERNAL_CLUSTER_NAMESPACE.stream().anyMatch(azpName::startsWith);
        if (!match) {
            LOG.warn("App fra ikke-godkjent namespace har etterspurt tilgang: " + azpName);
        }
        return match;
    }

    private PdpRequest lagPdpRequest(AbacAttributtSamling attributter, Set<AktørId> aktørIder, Set<AktørId> aktørIderForSporingslogg, Set<String> aksjonspunktType) {
        Set<String> aktører = aktørIder.stream().map(AktørId::getId).collect(Collectors.toCollection(TreeSet::new));
        Set<String> fnrs = attributter.getVerdier(StandardAbacAttributtType.FNR);

        PdpRequestMedBerørtePersonerForAuditlogg pdpRequest = new PdpRequestMedBerørtePersonerForAuditlogg();
        pdpRequest.setActionType(attributter.getActionType());
        pdpRequest.setResourceType(attributter.getResourceType());
        pdpRequest.setAktørIderStr(aktører);
        pdpRequest.setFødselsnumreStr(fnrs);
        pdpRequest.setAksjonspunktTypeEksternKode(aksjonspunktType);
        pdpRequest.setBerørtePersonerForAuditlogg(BerørtePersonerForAuditlogg.medAktørIder(aktørIderForSporingslogg, aktørid -> new no.nav.k9.felles.sikkerhet.abac.AktørId(aktørid.getAktørId())));

        return pdpRequest;
    }


    private static void validerAttributter(AbacAttributtSamling attributter) {
        Set<AbacAttributtType> tillatteTyper = Set.of(
            AppAbacAttributtType.SAKER_MED_FNR,
            AppAbacAttributtType.DOKUMENT_ID,
            AppAbacAttributtType.OPPGAVE_ID,
            StandardAbacAttributtType.SAKSNUMMER,
            StandardAbacAttributtType.BEHANDLING_ID,
            StandardAbacAttributtType.BEHANDLING_UUID,
            StandardAbacAttributtType.FNR,
            StandardAbacAttributtType.AKTØR_ID,
            StandardAbacAttributtType.JOURNALPOST_ID,
            StandardAbacAttributtType.AKSJONSPUNKT_KODE
        );
        Set<AbacAttributtType> ulovligeTyper = attributter.keySet().stream()
            .filter(it -> !tillatteTyper.contains(it))
            .collect(Collectors.toSet());
        if (!ulovligeTyper.isEmpty()) {
            throw new IllegalArgumentException(
                "Fikk abac-attributt-typer som ikke er støttet, endre DTO eller lag støtte for typene: " + ulovligeTyper);
        }

        if (attributter.keySet().contains(AppAbacAttributtType.DOKUMENT_ID) && ! attributter.keySet().contains(StandardAbacAttributtType.JOURNALPOST_ID)) {
            //det er journalpostId som brukes for tilgangskontroll, dokumentId er bare med for å kunne havne i auditlogg
            throw new IllegalArgumentException("Ikke støttet å ha dokumentId som abac-attributt uten å samtidig ha journalpostId.");
        }
    }

    private Optional<Long> utledBehandlingIder(AbacAttributtSamling attributter) {
        Set<Long> behandlingIdFraAttributter = attributter.<Long>getVerdier(StandardAbacAttributtType.BEHANDLING_ID).stream().mapToLong(Long::valueOf).boxed().collect(Collectors.toSet());

        Set<UUID> uuids = attributter.getVerdier(StandardAbacAttributtType.BEHANDLING_UUID);
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

    private static void validerSamsvarBehandlingOgFagsak(PipBehandlingsData behandlingData, Set<Saksnummer> saksnumre) {
        if (behandlingData != null) {
            if (saksnumre.size() == 1) {
                Saksnummer saksnummer = saksnumre.iterator().next();
                if (!saksnummer.equals(behandlingData.saksnummer())) {
                    throw FeilFactory.create(PdpRequestBuilderFeil.class).ugyldigInputManglerSamsvarBehandlingFagsak(behandlingData.behandlingUuid(), saksnummer.getVerdi())
                        .toException();
                }
            } else {
                List<String> saksnumreSomString = saksnumre.stream().map(Saksnummer::getVerdi).toList();
                throw FeilFactory.create(PdpRequestBuilderFeil.class).ugyldigInputHarFlereSaksnumreMedBehandling(behandlingData.behandlingUuid(), saksnumreSomString)
                    .toException();
            }
        }
    }

    private Set<Saksnummer> utledSaksnumre(AbacAttributtSamling attributter, PipBehandlingsData behandlingData) {
        Set<Saksnummer> saksnumre = utledSaksnumre(attributter);
        if (behandlingData != null) {
            saksnumre.add(behandlingData.saksnummer());
        }
        return saksnumre;
    }

    private Set<Saksnummer> utledSaksnumre(AbacAttributtSamling attributter) {
        Set<Saksnummer> saksnumreDirekteFraAttributter = attributter.getVerdier(StandardAbacAttributtType.SAKSNUMMER);
        Set<Saksnummer> eksisterendeSaksnumreForAttributtsaksnumre = pipRepository.finnSaksnumerSomEksisterer(saksnumreDirekteFraAttributter);
        if (!eksisterendeSaksnumreForAttributtsaksnumre.containsAll(saksnumreDirekteFraAttributter)) {
            throw new UkjentFagsakException(saksnumreDirekteFraAttributter.stream().map(Saksnummer::getVerdi).toList());
        }
        Set<Saksnummer> saksnumre = new LinkedHashSet<>();
        saksnumre.addAll(attributter.getVerdier(StandardAbacAttributtType.SAKSNUMMER));
        saksnumre.addAll(pipRepository.saksnumreForSøker(tilAktørId(attributter.getVerdier(AppAbacAttributtType.SAKER_MED_FNR))));
        saksnumre.addAll(pipRepository.saksnumreForJournalpostId(attributter.getVerdier(StandardAbacAttributtType.JOURNALPOST_ID)));
        return saksnumre;
    }

    private Set<AktørId> utledAktørIder(AbacAttributtSamling attributter, Set<Saksnummer> saksnumre) {
        Set<String> aktørIdVerdier = attributter.getVerdier(StandardAbacAttributtType.AKTØR_ID);

        Set<AktørId> aktørIder = new HashSet<>();
        aktørIder.addAll(aktørIdVerdier.stream().map(AktørId::new).collect(Collectors.toSet()));
        aktørIder.addAll(pipRepository.hentAktørIdKnyttetTilFagsaker(saksnumre));
        return aktørIder;
    }

    private Set<AktørId> utledAktørIderForSporingslogg(AbacAttributtSamling attributter, Set<Saksnummer> saksnumre) {
        Set<AktørId> aktørIder = new LinkedHashSet<>();
        aktørIder.addAll(attributter.getVerdier(StandardAbacAttributtType.AKTØR_ID).stream().map(it -> new AktørId((String) it)).toList());
        aktørIder.addAll(pipRepository.hentAktørIdForSporingslogg(saksnumre)); //legger til søker og evt. pleietrengende fra aktuelle saker
        return aktørIder;
    }

    private Collection<AktørId> tilAktørId(Set<String> fnr) {
        if (fnr == null || fnr.isEmpty()) {
            return Collections.emptySet();
        }
        return aktørTjeneste.hentAktørIdForPersonIdentSet(fnr.stream().map(PersonIdent::new).collect(Collectors.toSet()));
    }
}
