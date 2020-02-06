package no.nav.foreldrepenger.web.app.tjenester.dokument;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dokumentarkiv.ArkivDokument;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.dokument.DokumentDto;
import no.nav.k9.sak.kontrakt.dokument.DokumentIdDto;
import no.nav.k9.sak.kontrakt.dokument.JournalpostIdDto;
import no.nav.k9.sak.kontrakt.dokument.MottattDokumentDto;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Path("")
@ApplicationScoped
@Transactional
public class DokumentRestTjeneste {

    public static final String MOTTATT_DOKUMENTER_PATH = "/dokument/hent-mottattdokumentliste";
    public static final String DOKUMENTER_PATH = "/dokument/hent-dokumentliste";
    public static final String DOKUMENT_PATH = "/dokument/hent-dokument";

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

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path(MOTTATT_DOKUMENTER_PATH)
    @Operation(description = "Henter listen av mottatte dokumenter knyttet til en fagsak", tags = "dokument")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @Deprecated
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Collection<MottattDokumentDto> hentAlleMottatteDokumenterForBehandling(@NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingRepository.hentBehandling(behandlingId)
            : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
            
        return mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId())
            .stream()
            .map(m -> {
                var dto = new MottattDokumentDto();
                dto.setMottattDato(m.getMottattDato());
                dto.setDokumentTypeId(m.getDokumentType());
                dto.setDokumentKategori(m.getDokumentKategori());
                return dto;
            })
            .collect(Collectors.toList());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(MOTTATT_DOKUMENTER_PATH)
    @Operation(description = "Henter listen av mottatte dokumenter knyttet til en fagsak", tags = "dokument")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Collection<MottattDokumentDto> hentAlleMottatteDokumenterForBehandling(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto uuidDto) {
        return hentAlleMottatteDokumenterForBehandling(new BehandlingIdDto(uuidDto));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(DOKUMENTER_PATH)
    @Operation(description = "Henter dokumentlisten knyttet til en sak", summary = ("Oversikt over alle pdf dokumenter fra dokumentarkiv registrert for saksnummer."), tags = "dokument")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Collection<DokumentDto> hentAlleDokumenterForSak(@NotNull @QueryParam("saksnummer") @Parameter(description = "Saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto saksnummerDto) {
        try {
            Saksnummer saksnummer = new Saksnummer(saksnummerDto.getVerdi());
            final Optional<Fagsak> fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer);
            final Long fagsakId = fagsak.map(Fagsak::getId).orElse(null);
            if (fagsakId == null) {
                return new ArrayList<>();
            }

            Set<Long> åpneBehandlinger = behandlingRepository.hentBehandlingerSomIkkeErAvsluttetForFagsakId(fagsakId).stream().map(Behandling::getId)
                .collect(Collectors.toSet());

            Map<JournalpostId, List<Inntektsmelding>> inntektsMeldinger = inntektsmeldingTjeneste
                .hentAlleInntektsmeldingerForAngitteBehandlinger(åpneBehandlinger).stream()
                .collect(Collectors.groupingBy(Inntektsmelding::getJournalpostId));
            // Burde brukt map på dokumentid, men den lagres ikke i praksis.
            Map<JournalpostId, List<MottattDokument>> mottatteIMDokument = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(fagsakId).stream()
                .filter(mdok -> DokumentTypeId.INNTEKTSMELDING.getKode().equals(mdok.getDokumentType().getKode()))
                .collect(Collectors.groupingBy(MottattDokument::getJournalpostId));

            List<ArkivJournalPost> journalPostList = dokumentArkivTjeneste.hentAlleDokumenterForVisning(saksnummer);
            List<DokumentDto> dokumentResultat = new ArrayList<>();
            journalPostList.forEach(arkivJournalPost -> {
                dokumentResultat.addAll(mapFraArkivJournalPost(arkivJournalPost, mottatteIMDokument, inntektsMeldinger));
            });
            dokumentResultat.sort(Comparator.comparing(DokumentDto::getTidspunkt, Comparator.nullsFirst(Comparator.reverseOrder())));

            return dokumentResultat;
        } catch (ManglerTilgangException e) {
            throw DokumentRestTjenesteFeil.FACTORY.applikasjonHarIkkeTilgangTilHentJournalpostListeTjeneste(e).toException();
        }
    }

    @GET
    @Path(DOKUMENT_PATH)
    @Operation(description = "Søk etter dokument på JOARK-identifikatorene journalpostId og dokumentId", summary = ("Retunerer dokument som er tilknyttet saksnummer, journalpostId og dokumentId."), tags = "dokument")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentDokument(@SuppressWarnings("unused") @NotNull @QueryParam("saksnummer") @Parameter(description = "Saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto saksnummer,
                                 @NotNull @QueryParam("journalpostId") @Parameter(description = "Unik identifikator av journalposten (forsendelsenivå)") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) JournalpostIdDto journalpostId,
                                 @NotNull @QueryParam("dokumentId") @Parameter(description = "Unik identifikator av DokumentInfo/Dokumentbeskrivelse (dokumentnivå)") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) DokumentIdDto dokumentId) {
        try {
            ResponseBuilder responseBuilder = Response.ok(
                new ByteArrayInputStream(dokumentArkivTjeneste.hentDokumnet(new JournalpostId(journalpostId.getJournalpostId()), dokumentId.getDokumentId())));
            responseBuilder.type("application/pdf");
            responseBuilder.header("Content-Disposition", "filename=dokument.pdf");
            return responseBuilder.build();
        } catch (TekniskException e) {
            throw DokumentRestTjenesteFeil.FACTORY.dokumentIkkeFunnet(journalpostId.getJournalpostId(), dokumentId.getDokumentId(), e).toException();
        } catch (ManglerTilgangException e) {
            throw DokumentRestTjenesteFeil.FACTORY.applikasjonHarIkkeTilgangTilHentDokumentTjeneste(e).toException();
        }
    }

    private List<DokumentDto> mapFraArkivJournalPost(ArkivJournalPost arkivJournalPost, Map<JournalpostId, List<MottattDokument>> mottatteIMDokument,
                                                     Map<JournalpostId, List<Inntektsmelding>> inntektsMeldinger) {
        List<DokumentDto> dokumentForJP = new ArrayList<>();
        if (arkivJournalPost.getHovedDokument() != null) {
            dokumentForJP.add(mapFraArkivDokument(arkivJournalPost, arkivJournalPost.getHovedDokument(), mottatteIMDokument, inntektsMeldinger));
        }
        if (arkivJournalPost.getAndreDokument() != null) {
            arkivJournalPost.getAndreDokument().forEach(dok -> {
                dokumentForJP.add(mapFraArkivDokument(arkivJournalPost, dok, mottatteIMDokument, inntektsMeldinger));
            });
        }
        return dokumentForJP;
    }

    private DokumentDto mapFraArkivDokument(ArkivJournalPost arkivJournalPost, ArkivDokument arkivDokument,
                                            Map<JournalpostId, List<MottattDokument>> mottatteIMDokument,
                                            Map<JournalpostId, List<Inntektsmelding>> inntektsMeldinger) {
        var dto = new DokumentDto();
        dto.setJournalpostId(arkivJournalPost.getJournalpostId());
        dto.setDokumentId(arkivDokument.getDokumentId());
        dto.setTittel(arkivDokument.getTittel());
        dto.setKommunikasjonsretning(arkivJournalPost.getKommunikasjonsretning());
        dto.setTidspunkt(arkivJournalPost.getTidspunkt());

        if (DokumentTypeId.INNTEKTSMELDING.equals(arkivDokument.getDokumentType()) && mottatteIMDokument.containsKey(arkivJournalPost.getJournalpostId())) {
            List<Long> behandlinger = mottatteIMDokument.get(dto.getJournalpostId()).stream()
                .filter(imdok -> inntektsMeldinger.containsKey(dto.getJournalpostId()))
                .map(MottattDokument::getBehandlingId)
                .collect(Collectors.toList());
            dto.setBehandlinger(behandlinger);

            Optional<String> navn = inntektsMeldinger.getOrDefault(dto.getJournalpostId(), Collections.emptyList())
                .stream()
                .map((Inntektsmelding inn) -> {
                    var t = inn.getArbeidsgiver();
                    if (t.getErVirksomhet()) {
                        Optional<Virksomhet> virksomhet = virksomhetTjeneste.hentVirksomhet(t.getOrgnr());
                        return virksomhet.orElseThrow(() -> new IllegalArgumentException("Kunne ikke hente virksomhet for orgNummer: " + t.getOrgnr()))
                            .getNavn();
                    } else {
                        return "Privatperson";
                    }
                })// TODO slå opp navnet på privatpersonen?
                .findFirst();
            navn.ifPresent(dto::setGjelderFor);
        }
        return dto;
    }
}
