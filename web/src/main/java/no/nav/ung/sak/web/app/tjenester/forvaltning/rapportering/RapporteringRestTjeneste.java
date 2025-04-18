package no.nav.ung.sak.web.app.tjenester.forvaltning.rapportering;

import static no.nav.ung.abac.BeskyttetRessursKoder.DRIFT;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.rest.AbacEmptySupplier;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.web.app.tjenester.forvaltning.DumpOutput;
import no.nav.ung.sak.web.server.abac.AbacAttributtEmptySupplier;

@Path(RapporteringRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class RapporteringRestTjeneste {

    private static final Logger log = LoggerFactory.getLogger(RapporteringRestTjeneste.class);

    private static final DateTimeFormatter DT_FORMAT = new DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ISO_LOCAL_DATE)
        .appendLiteral('T')
        .appendPattern("HHmmss")
        .toFormatter();

    static final String BASE_PATH = "/rapportering";

    private Instance<RapportGenerator> rapportGenerators;

    private TmpAktoerIdRepository tmpAktoerIdRepository;

    RapporteringRestTjeneste() {
        // for proxy
    }

    @Inject
    RapporteringRestTjeneste(TmpAktoerIdRepository tmpAktoerIdRepository,
                             @Any Instance<RapportGenerator> rapportGenerators) {
        this.tmpAktoerIdRepository = tmpAktoerIdRepository;
        this.rapportGenerators = rapportGenerators;
    }

    @POST
    @Path("/generer")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(description = "Dumper en rapport av data", summary = ("Henter en dump av info for debugging og analyse av en sak"), tags = "rapportering")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = DRIFT)
    public Response genererRapportForYtelse(@NotNull @FormParam("ytelseType") @Parameter(description = "ytelseType", required = true) @Valid @TilpassetAbacAttributt(supplierClass = AbacEmptySupplier.class) YtelseTypeKode ytelseTypeKode,
                                            @NotNull @FormParam("rapport") @Parameter(description = "rapport", required = true) @Valid @TilpassetAbacAttributt(supplierClass = AbacEmptySupplier.class) RapportType rapportType,
                                            @NotNull @FormParam("periode") @Parameter(description = "periode", required = true, example = "2020-01-01/2020-12-31") @Valid @TilpassetAbacAttributt(supplierClass = AbacEmptySupplier.class) Periode periode) {

        FagsakYtelseType ytelseType = FagsakYtelseType.fraKode(ytelseTypeKode.name());
        rapportType.valider(ytelseType);

        var generators = RapportTypeRef.Lookup.list(RapportGenerator.class, rapportGenerators, rapportType);

        List<DumpOutput> outputListe = new ArrayList<>();
        for (var generator : generators) {
            RapportGenerator g = generator.get();
            log.info("RapportGenerator [{}]({}), ytelse: {}", g.getClass().getName(), rapportType, ytelseType);
            var output = g.generer(ytelseType, DatoIntervallEntitet.fra(periode));
            outputListe.addAll(output);
        }

        var streamingOutput = new ZipOutput().dump(outputListe);

        return Response.ok(streamingOutput)
            .type(MediaType.APPLICATION_OCTET_STREAM)
            .header("Content-Disposition", String.format("attachment; filename=\"%s-%s-%s.zip\"", rapportType.name(), ytelseType.getKode(), LocalDateTime.now().format(DT_FORMAT)))
            .build();

    }

    @POST
    @Path("/innhent-fnr")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(description = "Cache fnr for aktørid for rapporteringsformål", summary = ("Cache fnr for aktørid for rapporteringsformålg"), tags = "forvaltning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = DRIFT)
    public Response innhentFnr(@NotNull @FormParam("restart") @Parameter(description = "restart innhenting", allowEmptyValue = false, required = true) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) Boolean restart) {
        if (restart) {
            tmpAktoerIdRepository.resetAktørIdCache();
        }

        tmpAktoerIdRepository.startInnhenting();
        return Response.ok().build();

    }

    @POST
    @Path("/dump-fnr")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(description = "Dump Cache fnr ", summary = ("Dump Cache fnr "), tags = "forvaltning")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = DRIFT)
    public Response dumpFnrCache() {
        tmpAktoerIdRepository.resetAktørIdCache();
        return Response.ok().build();

    }
}
