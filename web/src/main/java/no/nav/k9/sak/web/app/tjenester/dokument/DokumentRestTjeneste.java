package no.nav.k9.sak.web.app.tjenester.dokument;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.k9.felles.exception.ManglerTilgangException;
import no.nav.k9.felles.exception.TekniskException;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.dokument.Kommunikasjonsretning;
import no.nav.k9.kodeverk.uttak.Tid;
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
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
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
    public static final String SAKSNUMMER_PARAM = "saksnummer";
    public static final String JOURNALPOST_ID_PARAM = "journalpostId";
    public static final String DOKUMENT_ID_PARAM = "dokumentId";

    private DokumentArkivTjeneste dokumentArkivTjeneste;
    private InntektArbeidYtelseTjeneste inntektsmeldingTjeneste;
    private FagsakRepository fagsakRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private VirksomhetTjeneste virksomhetTjeneste;
    private BehandlingRepository behandlingRepository;
    private Boolean enablePsbHenleggelse;

    public DokumentRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public DokumentRestTjeneste(DokumentArkivTjeneste dokumentArkivTjeneste,
                                InntektArbeidYtelseTjeneste inntektsmeldingTjeneste,
                                FagsakRepository fagsakRepository,
                                MottatteDokumentRepository mottatteDokumentRepository,
                                VirksomhetTjeneste virksomhetTjeneste,
                                BehandlingRepository behandlingRepository,
                                @KonfigVerdi(value = "BEHANDLINGID_DOKUMENTLIST_ENABLET", defaultVerdi = "false") Boolean enablePsbHenleggelse) {
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.virksomhetTjeneste = virksomhetTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.enablePsbHenleggelse = enablePsbHenleggelse;
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

            var behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsakId);

            // Burde brukt map på dokumentid, men den lagres ikke i praksis.
            Map<JournalpostId, List<MottattDokument>> mottattedokumenter = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(fagsakId).stream()
                .collect(Collectors.groupingBy(MottattDokument::getJournalpostId));

            List<ArkivJournalPost> journalPostList = dokumentArkivTjeneste.hentAlleDokumenterForVisning(saksnummer);
            List<DokumentDto> dokumentResultat = new ArrayList<>();

            Map<JournalpostId, List<Inntektsmelding>> inntektsmeldinger = finnInntektsmeldinger(fagsak);

            journalPostList.forEach(arkivJournalPost -> {
                dokumentResultat.addAll(mapFraArkivJournalPost(saksnummer, arkivJournalPost, mottattedokumenter, inntektsmeldinger));
            });

            return dokumentResultat.stream()
                .peek(it -> leggTilBehandling(it, behandlinger))
                .sorted(Comparator.comparing(DokumentDto::getTidspunkt, Comparator.nullsFirst(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        } catch (ManglerTilgangException e) {
            throw DokumentRestTjenesteFeil.FACTORY.applikasjonHarIkkeTilgangTilHentJournalpostListeTjeneste(e).toException();
        }
    }

    private void leggTilBehandling(DokumentDto dto, List<Behandling> behandlinger) {
        if (!enablePsbHenleggelse) {
            return;
        }
        var behandlingTidslinje = opprettTidslinje(behandlinger);
        var behandlingId = utledBehandling(dto, behandlingTidslinje);

        if (behandlingId != null) {
            var result = new ArrayList<Long>();
            result.add(behandlingId);

            dto.setBehandlinger(result);
        }
    }

    Long utledBehandling(DokumentDto dto, NavigableSet<BehandlingPeriode> behandlingTidslinje) {
        if (Objects.equals(dto.getKommunikasjonsretning(), Kommunikasjonsretning.UT)) {
            // finn nærmeste TOM
            return behandlingTidslinje.stream()
                .min(Comparator.comparing(it -> distanseTilTom(dto.getTidspunkt(), it)))
                .map(BehandlingPeriode::getBehandlingId)
                .orElse(null);
        }

        return behandlingTidslinje.stream()
            .filter(it -> it.getTom().isAfter(dto.getTidspunkt()))
            .min(Comparator.comparing(it -> distanseTilTom(dto.getTidspunkt(), it)))
            .map(BehandlingPeriode::getBehandlingId)
            .orElse(null);
    }

    private Long distanseTilTom(LocalDateTime dato, BehandlingPeriode periode) {
        return Math.abs(ChronoUnit.MILLIS.between(dato, periode.getTom()));
    }

    private NavigableSet<BehandlingPeriode> opprettTidslinje(List<Behandling> behandlinger) {
        var tidslinje = new TreeSet<BehandlingPeriode>();

        var sorted = new ArrayList<>(behandlinger);
        sorted.sort(Comparator.comparing(Behandling::getAvsluttetDato, Comparator.nullsLast(Comparator.naturalOrder())));

        LocalDateTime fom = Tid.TIDENES_BEGYNNELSE.atStartOfDay();
        for (Behandling behandling : sorted) {
            var tom = behandling.getAvsluttetDato() != null ? behandling.getAvsluttetDato() : Tid.TIDENES_ENDE.atStartOfDay();
            tidslinje.add(new BehandlingPeriode(fom, tom, behandling.getId()));
            fom = tom.plusNanos(1);
        }

        return tidslinje;
    }

    private Map<JournalpostId, List<Inntektsmelding>> finnInntektsmeldinger(Fagsak fagsak) {
        var ytelseType = fagsak.getYtelseType();
        boolean harInntektsmeldinger = !ytelseType.erRammevedtak();
        return !harInntektsmeldinger
            ? Collections.emptyMap()
            : inntektsmeldingTjeneste.hentUnikeInntektsmeldingerForSak(fagsak.getSaksnummer()).stream()
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
        dto.setBrevkode(arkivDokument.getBrevkode());
        dto.setKommunikasjonsretning(arkivJournalPost.getKommunikasjonsretning());
        dto.setTidspunkt(arkivJournalPost.getTidspunkt());

        if (mottatteDokument.containsKey(arkivJournalPost.getJournalpostId())) {
            JournalpostId journalpostId = dto.getJournalpostId();
            dto.setBehandlinger(List.of());

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
