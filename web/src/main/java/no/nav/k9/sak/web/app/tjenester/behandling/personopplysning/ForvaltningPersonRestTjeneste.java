package no.nav.k9.sak.web.app.tjenester.behandling.personopplysning;

import static no.nav.k9.abac.BeskyttetRessursKoder.DRIFT;
import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;
import static no.nav.k9.sak.web.app.tjenester.forvaltning.CsvOutput.dumpAsCsv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import no.nav.k9.felles.exception.ManglerTilgangException;
import no.nav.k9.felles.sikkerhet.abac.AbacAttributtType;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.AbacDto;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.felles.util.InputValideringRegex;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.kontrakt.person.AktørIdDto;
import no.nav.k9.sak.kontrakt.person.AktørIdOgFnrDto;
import no.nav.k9.sak.kontrakt.person.AktørInfoDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;


@ApplicationScoped
@Transactional
@Path("/forvaltning/person")
public class ForvaltningPersonRestTjeneste {

    private AktørIdSplittTjeneste aktørIdSplittTjeneste;
    private TpsTjeneste tpsTjeneste;
    private FagsakRepository fagsakRepository;
    private FinnUnikeAktører finnUnikeAktører;
    private EntityManager entityManager;

    public ForvaltningPersonRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public ForvaltningPersonRestTjeneste(AktørIdSplittTjeneste aktørIdSplittTjeneste, TpsTjeneste tpsTjeneste, FagsakRepository fagsakRepository, FinnUnikeAktører finnUnikeAktører, EntityManager entityManager) {
        this.aktørIdSplittTjeneste = aktørIdSplittTjeneste;
        this.tpsTjeneste = tpsTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.finnUnikeAktører = finnUnikeAktører;
        this.entityManager = entityManager;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/finnAktørerKobletTilSammeSakSomAktør")
    @Operation(description = "Henter aktører pr sak relatert til oppgitt aktørid", tags = "forvaltning - person", responses = {
        @ApiResponse(responseCode = "200",
            description = "Aktører for pr sak",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response finnAktørerKobletTilSammeSakSomAktør(@Parameter(description = "AktørId") @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) @Valid AktørIdDto aktørId) {
        var fagsakQuery = entityManager.createNativeQuery("select * from fagsak where bruker_aktoer_id = :aktørId", Fagsak.class)
            .setParameter("aktørId", aktørId.getAktorId());
        List<Fagsak>  fagsaker = fagsakQuery.getResultList();
        var aktørerForSak = fagsaker.stream().map(finnUnikeAktører::finnUnikeAktørerMedDokumenter).toList();
        return Response.ok(aktørerForSak, MediaType.APPLICATION_JSON).build();
    }


    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/hentFnr")
    @Operation(description = "Hent fnr for aktørid", tags = "forvaltning - person", responses = {
        @ApiResponse(responseCode = "200",
            description = "Hent fnr for aktørid",
            content = @Content(mediaType = MediaType.TEXT_PLAIN))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getFnrForAktørId(@Parameter(description = "AktørIder (skilt med mellomrom eller linjeskift)") @Valid HentFnr hentFnr) {
        var alleAktørIder = Objects.requireNonNull(hentFnr.getAktørIder(), "aktørIder");
        var aktørIder = new LinkedHashSet<>(Arrays.asList(alleAktørIder.split("\\s+")));
        var results = aktørIder.stream().flatMap(a -> {
            var aktørId = new AktørId(a);
            try {
                var fnr = tpsTjeneste.hentFnr(aktørId);
                return fnr.map(f -> {
                    var dto = new AktørIdOgFnrDto();
                    dto.setFnr(f.getIdent());
                    dto.setAktørId(aktørId);
                    return dto;
                }).stream();
            } catch (ManglerTilgangException e) {
                return Stream.empty();
            }
        }).toList();

        String output = dumpResultSetToCsv(results).orElseThrow();
        return Response.ok(output, MediaType.TEXT_PLAIN_TYPE).build();
    }


    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/oppdater-aktoer-bruker")
    @Operation(description = "Oppdater aktørId for bruker på sak med ugyldig aktørId pga aktørSplitt/merge", tags = "forvaltning - person", responses = {
        @ApiResponse(responseCode = "200",
            description = "Fiks ugyldig aktørId",
            content = @Content(mediaType = MediaType.TEXT_PLAIN))
    })
    @BeskyttetRessurs(action = UPDATE, resource = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response oppdaterAktørIdBruker(@Valid @NotNull OppdaterAktørIdDto dto) {
        aktørIdSplittTjeneste.patchBrukerAktørId(dto.getGyldigAktørId(), dto.getUtgåttAktørId(), dto.getBegrunnelse(), "/forvaltning/person/oppdater-aktoer-bruker");
        return Response.ok().build();
    }

    static class OppdaterAktørIdDto implements AbacDto {

        @Valid
        @NotNull
        private Saksnummer saksnummer;
        @Valid
        @NotNull
        private AktørId gyldigAktørId;

        @Valid
        @NotNull
        private AktørId utgåttAktørId;

        @Valid
        @NotNull
        @Size(max = 1000)
        @Pattern(regexp = InputValideringRegex.FRITEKST)
        private String begrunnelse;

        public Saksnummer getSaksnummer() {
            return saksnummer;
        }

        public void setSaksnummer(Saksnummer saksnummer) {
            this.saksnummer = saksnummer;
        }

        public AktørId getGyldigAktørId() {
            return gyldigAktørId;
        }

        public void setGyldigAktørId(AktørId gyldigAktørId) {
            this.gyldigAktørId = gyldigAktørId;
        }

        public String getBegrunnelse() {
            return begrunnelse;
        }

        public void setBegrunnelse(String begrunnelse) {
            this.begrunnelse = begrunnelse;
        }

        public AktørId getUtgåttAktørId() {
            return utgåttAktørId;
        }

        public void setUtgåttAktørId(AktørId utgåttAktørId) {
            this.utgåttAktørId = utgåttAktørId;
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            //ikke mulig med reell tilgangskontroll, siden aktørId på saken er ugyldig
            return AbacDataAttributter.opprett().leggTil(StandardAbacAttributtType.AKTØR_ID, gyldigAktørId);
        }
    }

    @GET
    @Operation(description = "Henter saksnumre for en person. Kan for eksempel brukes for å finne ut om k9 er påvirket av 'aktør-splitt'", tags = "aktoer", responses = {
        @ApiResponse(responseCode = "200", description = "Liste av fagsaker i k9-sak personen er del av.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AktørInfoDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = DRIFT)
    @Path("/saksnumre-for-person")
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<Saksnummer> getAktoerInfo(@NotNull @QueryParam("aktoerId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) AktørIdDto aktørIdDto) {
        var aktørId = aktørIdDto.getAktørId();
        List<Fagsak> fagsaker = fagsakRepository.hentSakerHvorBrukerHarMinstEnRolle(aktørId);
        return fagsaker.stream().map(Fagsak::getSaksnummer).distinct().toList();
    }

    private static Optional<String> dumpResultSetToCsv(List<AktørIdOgFnrDto> results) {
        if (results.isEmpty()) {
            return Optional.empty();
        }
        var toCsv = new LinkedHashMap<String, Function<AktørIdOgFnrDto, ?>>();
        toCsv.put("aktørId", AktørIdOgFnrDto::getAktørIdString);
        toCsv.put("fnr", AktørIdOgFnrDto::getFnr);
        return Optional.of(dumpAsCsv(true, results, toCsv));
    }


    public static class HentFnr implements AbacDto {

        @NotNull
        @Pattern(regexp = "^[\\p{Alnum}\\s]+$", message = "HentFnr [${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
        private String aktørIder;

        public HentFnr() {
            // empty ctor
        }

        public HentFnr(@NotNull String aktørIder) {
            this.aktørIder = aktørIder;
        }

        @NotNull
        public String getAktørIder() {
            return aktørIder;
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            var aktørIder = new LinkedHashSet<>(Arrays.asList(getAktørIder().split("\\s+")));
            var abacDataAttributter = AbacDataAttributter.opprett();
            aktørIder.forEach(it -> abacDataAttributter.leggTil(StandardAbacAttributtType.AKTØR_ID, it));
            return abacDataAttributter;
        }

        @Provider
        public static class HentFnrMessageBodyReader implements MessageBodyReader<HentFnr> {

            @Override
            public boolean isReadable(Class<?> type, Type genericType,
                                      Annotation[] annotations, MediaType mediaType) {
                return (type == HentFnr.class);
            }

            @Override
            public HentFnr readFrom(Class<HentFnr> type, Type genericType,
                                    Annotation[] annotations, MediaType mediaType,
                                    MultivaluedMap<String, String> httpHeaders,
                                    InputStream inputStream)
                throws IOException, WebApplicationException {
                var sb = new StringBuilder(200);
                try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(inputStream))) {
                    sb.append(br.readLine()).append('\n');
                }

                return new HentFnr(sb.toString());

            }
        }
    }


}
