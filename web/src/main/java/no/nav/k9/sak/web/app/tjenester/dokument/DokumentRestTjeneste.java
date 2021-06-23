package no.nav.k9.sak.web.app.tjenester.dokument;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.k9.felles.exception.ManglerTilgangException;
import no.nav.k9.felles.exception.TekniskException;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.virksomhet.Virksomhet;
import no.nav.k9.sak.dokument.arkiv.ArkivDokument;
import no.nav.k9.sak.dokument.arkiv.ArkivJournalPost;
import no.nav.k9.sak.dokument.arkiv.DokumentArkivTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.k9.sak.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.dokument.DokumentDto;
import no.nav.k9.sak.kontrakt.dokument.DokumentIdDto;
import no.nav.k9.sak.kontrakt.dokument.JournalpostIdDto;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.tjenester.behandling.BehandlingDtoUtil;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@Path("")
@ApplicationScoped
@Transactional
public class DokumentRestTjeneste {

    public static final String DOKUMENTER_PATH = "/dokument/hent-dokumentliste";
    public static final String DOKUMENT_PATH = "/dokument/hent-dokument";
    public static final String DOKUMENTER_BEHANDLING_PATH = "/dokument/hent-dokument-behandling";
    public static final String SAKSNUMMER_PARAM = "saksnummer";
    public static final String JOURNALPOST_ID_PARAM = "journalpostId";
    public static final String DOKUMENT_ID_PARAM = "dokumentId";

    private DokumentArkivTjeneste dokumentArkivTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private FagsakRepository fagsakRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private VirksomhetTjeneste virksomhetTjeneste;
    private BehandlingRepository behandlingRepository;

    public DokumentRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public DokumentRestTjeneste(DokumentArkivTjeneste dokumentArkivTjeneste,
                                InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                FagsakRepository fagsakRepository,
                                MottatteDokumentRepository mottatteDokumentRepository,
                                VirksomhetTjeneste virksomhetTjeneste,
                                BehandlingRepository behandlingRepository) {
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.virksomhetTjeneste = virksomhetTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(DOKUMENTER_PATH)
    @Operation(description = "Henter dokumentlisten knyttet til en sak", summary = ("Oversikt over alle pdf dokumenter fra dokumentarkiv registrert for saksnummer."), tags = "dokument")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Collection<DokumentDto> hentAlleDokumenterForSak(@NotNull @QueryParam("saksnummer") @Parameter(description = "Saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto saksnummerDto) {
        try {
            Saksnummer saksnummer = saksnummerDto.getVerdi();
            final Optional<Fagsak> fagsakOpt = fagsakRepository.hentSakGittSaksnummer(saksnummer);
            final Long fagsakId = fagsakOpt.map(Fagsak::getId).orElse(null);
            if (fagsakId == null) {
                return new ArrayList<>();
            }

            Fagsak fagsak = fagsakOpt.get();

            Set<Long> åpneBehandlinger = behandlingRepository.hentBehandlingerSomIkkeErAvsluttetForFagsakId(fagsakId).stream().map(Behandling::getId)
                .collect(Collectors.toSet());

            // Burde brukt map på dokumentid, men den lagres ikke i praksis.
            Map<JournalpostId, List<MottattDokument>> mottattedokumenter = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(fagsakId).stream()
                .collect(Collectors.groupingBy(MottattDokument::getJournalpostId));

            List<ArkivJournalPost> journalPostList = dokumentArkivTjeneste.hentAlleDokumenterForVisning(saksnummer);
            List<DokumentDto> dokumentResultat = new ArrayList<>();

            Map<JournalpostId, List<Inntektsmelding>> inntektsmeldinger = finnInntektsmeldinger(fagsak, åpneBehandlinger);

            journalPostList.forEach(arkivJournalPost -> {
                dokumentResultat.addAll(mapFraArkivJournalPost(saksnummer, arkivJournalPost, mottattedokumenter, inntektsmeldinger));
            });
            dokumentResultat.sort(Comparator.comparing(DokumentDto::getTidspunkt, Comparator.nullsFirst(Comparator.reverseOrder())));

            return dokumentResultat;
        } catch (ManglerTilgangException e) {
            throw DokumentRestTjenesteFeil.FACTORY.applikasjonHarIkkeTilgangTilHentJournalpostListeTjeneste(e).toException();
        }
    }

    private Map<JournalpostId, List<Inntektsmelding>> finnInntektsmeldinger(Fagsak fagsak, Set<Long> åpneBehandlinger) {
        var ytelseType = fagsak.getYtelseType();
        boolean harInntektsmeldinger = !ytelseType.erRammevedtak();
        return !harInntektsmeldinger
            ? Collections.emptyMap()
            : inntektsmeldingTjeneste.hentAlleInntektsmeldingerForAngitteBehandlinger(åpneBehandlinger).stream()
            .collect(Collectors.groupingBy(Inntektsmelding::getJournalpostId));
    }

    @GET
    @Path(DOKUMENT_PATH)
    @Operation(description = "Søk etter dokument på JOARK-identifikatorene journalpostId og dokumentId", summary = ("Retunerer dokument som er tilknyttet saksnummer, journalpostId og dokumentId."), tags = "dokument")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentDokument(@SuppressWarnings("unused") @NotNull @QueryParam(SAKSNUMMER_PARAM) @Parameter(description = "Saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto saksnummer,
                                 @NotNull @QueryParam(JOURNALPOST_ID_PARAM) @Parameter(description = "Unik identifikator av journalposten (forsendelsenivå)") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) JournalpostIdDto journalpostId,
                                 @NotNull @QueryParam(DOKUMENT_ID_PARAM) @Parameter(description = "Unik identifikator av DokumentInfo/Dokumentbeskrivelse (dokumentnivå)") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) DokumentIdDto dokumentId) {
        try {
            ResponseBuilder responseBuilder = Response.ok(
                new ByteArrayInputStream(dokumentArkivTjeneste.hentDokumnet(journalpostId.getJournalpostId(), dokumentId.getDokumentId())));
            responseBuilder.type("application/pdf");
            responseBuilder.header("Content-Disposition", "filename=dokument.pdf");
            return responseBuilder.build();
        } catch (TekniskException e) {
            throw DokumentRestTjenesteFeil.FACTORY.dokumentIkkeFunnet(journalpostId.getJournalpostId(), dokumentId.getDokumentId(), e).toException();
        } catch (ManglerTilgangException e) {
            throw DokumentRestTjenesteFeil.FACTORY.applikasjonHarIkkeTilgangTilHentDokumentTjeneste(e).toException();
        }
    }

    private List<DokumentDto> mapFraArkivJournalPost(Saksnummer saksnummer, ArkivJournalPost arkivJournalPost, Map<JournalpostId, List<MottattDokument>> mottatteDokument,
                                                     Map<JournalpostId, List<Inntektsmelding>> inntektsMeldinger) {
        List<DokumentDto> dokumentForJP = new ArrayList<>();
        if (arkivJournalPost.getHovedDokument() != null) {
            dokumentForJP.add(mapFraArkivDokument(saksnummer, arkivJournalPost, arkivJournalPost.getHovedDokument(), mottatteDokument, inntektsMeldinger));
        }
        if (arkivJournalPost.getAndreDokument() != null) {
            arkivJournalPost.getAndreDokument().forEach(dok -> {
                dokumentForJP.add(mapFraArkivDokument(saksnummer, arkivJournalPost, dok, mottatteDokument, inntektsMeldinger));
            });
        }
        return dokumentForJP;
    }

    private DokumentDto mapFraArkivDokument(Saksnummer saksnummer, ArkivJournalPost arkivJournalPost, ArkivDokument arkivDokument,
                                            Map<JournalpostId, List<MottattDokument>> mottatteDokument,
                                            Map<JournalpostId, List<Inntektsmelding>> inntektsmeldinger) {
        var dto = new DokumentDto(byggApiPath(saksnummer));
        dto.setJournalpostId(arkivJournalPost.getJournalpostId());
        dto.setDokumentId(arkivDokument.getDokumentId());
        dto.setTittel(arkivDokument.getTittel());
        dto.setKommunikasjonsretning(arkivJournalPost.getKommunikasjonsretning());
        dto.setTidspunkt(arkivJournalPost.getTidspunkt());

        if (mottatteDokument.containsKey(arkivJournalPost.getJournalpostId())) {
            JournalpostId journalpostId = dto.getJournalpostId();
            List<Long> behandlinger = mottatteDokument.get(journalpostId).stream()
                .filter(imdok -> inntektsmeldinger.containsKey(journalpostId))
                .map(MottattDokument::getBehandlingId)
                .collect(Collectors.toList());
            dto.setBehandlinger(behandlinger);

            var imForJournalpost = inntektsmeldinger.getOrDefault(journalpostId, Collections.emptyList());
            Optional<String> navn = hentGjelderFor(imForJournalpost);
            navn.ifPresent(dto::setGjelderFor);
        }
        return dto;
    }

    private String byggApiPath(Saksnummer saksnummer) {
        return BehandlingDtoUtil.getApiPath(DokumentRestTjeneste.DOKUMENT_PATH + "?" + SAKSNUMMER_PARAM + "=" + saksnummer.getVerdi());
    }

    private Optional<String> hentGjelderFor(List<Inntektsmelding> inntektsmeldinger) {
        Optional<String> navn = inntektsmeldinger
            .stream()
            .map(im -> {
                var t = im.getArbeidsgiver();
                if (t.getErVirksomhet()) {
                    Optional<Virksomhet> virksomhet = virksomhetTjeneste.finnOrganisasjon(t.getOrgnr());
                    return virksomhet.orElseThrow(() -> new IllegalArgumentException("Kunne ikke hente virksomhet for orgNummer: " + t.getOrgnr()))
                        .getNavn();
                } else {
                    return "Privatperson";
                }
            })// TODO slå opp navnet på privatpersonen?
            .findFirst();
        return navn;
    }
}
