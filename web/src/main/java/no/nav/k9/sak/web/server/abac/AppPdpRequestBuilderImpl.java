package no.nav.k9.sak.web.server.abac;

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

import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.pip.PipBehandlingsData;
import no.nav.k9.sak.behandlingslager.pip.PipRepository;
import no.nav.k9.sak.domene.person.pdl.AktørTjeneste;
import no.nav.k9.sak.sikkerhet.abac.AppAbacAttributtType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.log.mdc.MdcExtendedLogContext;
import no.nav.vedtak.sikkerhet.abac.AbacAttributtSamling;
import no.nav.vedtak.sikkerhet.abac.PdpKlient;
import no.nav.vedtak.sikkerhet.abac.PdpRequest;
import no.nav.vedtak.sikkerhet.abac.PdpRequestBuilder;

@Dependent
@Alternative
@Priority(2)
public class AppPdpRequestBuilderImpl implements PdpRequestBuilder {
    public static final String ABAC_DOMAIN = "k9";
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
        Optional<Long> behandlingIder = utledBehandlingIder(attributter);
        Optional<PipBehandlingsData> behandlingData = behandlingIder.isPresent()
            ? pipRepository.hentDataForBehandling(behandlingIder.get())
            : Optional.empty();
        Set<Long> fagsakIder = behandlingData.isPresent()
            ? utledFagsakIder(attributter, behandlingData.get())
            : utledFagsakIder(attributter);

        behandlingData.ifPresent(pipBehandlingsData -> validerSamsvarBehandlingOgFagsak(behandlingIder.get(), pipBehandlingsData.getFagsakId(), fagsakIder));

        if (!fagsakIder.isEmpty()) {
            LOG_CONTEXT.add("fagsak", fagsakIder.size() == 1 ? fagsakIder.iterator().next().toString() : fagsakIder.toString());
        }
        behandlingIder.ifPresent(behId -> {
            LOG_CONTEXT.add("behandling", behId);
        });

        Set<AktørId> aktørIder = utledAktørIder(attributter, fagsakIder);
        Set<String> aksjonspunktType = pipRepository.hentAksjonspunktTypeForAksjonspunktKoder(attributter.getVerdier(AppAbacAttributtType.AKSJONSPUNKT_KODE));
        return behandlingData.isPresent()
            ? lagPdpRequest(attributter, aktørIder, aksjonspunktType, behandlingData.get())
            : lagPdpRequest(attributter, aktørIder, aksjonspunktType);
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
            pdpRequest.put(AbacAttributter.RESOURCE_K9_SAK_AKSJONSPUNKT_TYPE, aksjonspunktType);
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
            .ifPresent(it -> pdpRequest.put(AbacAttributter.RESOURCE_K9_SAK_BEHANDLINGSSTATUS, it.getEksternKode()));
        AbacUtil.oversettFagstatus(behandlingData.getFagsakStatus())
            .ifPresent(it -> pdpRequest.put(AbacAttributter.RESOURCE_K9_SAK_SAKSSTATUS, it.getEksternKode()));
        behandlingData.getAnsvarligSaksbehandler()
            .ifPresent(it -> pdpRequest.put(AbacAttributter.RESOURCE_K9_SAK_ANSVARLIG_SAKSBEHANDLER, it));
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
