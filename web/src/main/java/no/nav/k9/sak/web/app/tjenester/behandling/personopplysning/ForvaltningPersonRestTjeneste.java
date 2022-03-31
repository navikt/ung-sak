package no.nav.k9.sak.web.app.tjenester.behandling.personopplysning;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import no.nav.k9.felles.exception.ManglerTilgangException;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.AbacDto;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.kontrakt.person.AktørIdOgFnrDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;


@ApplicationScoped
@Transactional
@Path("/forvaltning/person")
public class ForvaltningPersonRestTjeneste {

    private TpsTjeneste tpsTjeneste;

    public ForvaltningPersonRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public ForvaltningPersonRestTjeneste(TpsTjeneste tpsTjeneste) {
        this.tpsTjeneste = tpsTjeneste;
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

        String path = "";

        var output = dumpResultSetToCsv(path, results).orElseThrow();
        return Response.ok(output.getContent(), MediaType.TEXT_PLAIN_TYPE).build();
    }

    private static Optional<DumpOutput> dumpResultSetToCsv(String path, List<AktørIdOgFnrDto> results) {
        if (results.isEmpty()) {
            return Optional.empty();
        }
        var toCsv = new LinkedHashMap<String, Function<AktørIdOgFnrDto, ?>>();
        toCsv.put("aktørId", AktørIdOgFnrDto::getAktørIdString);
        toCsv.put("fnr", AktørIdOgFnrDto::getFnr);
        return Optional.of(dumpAsCsv(true, results, path, toCsv));
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
