package no.nav.k9.sak.web.app.tjenester.forvaltning.rapportering;

import static no.nav.k9.abac.BeskyttetRessursKoder.DRIFT;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.rest.AbacEmptySupplier;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;

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

    RapporteringRestTjeneste() {
        // for proxy
    }

    @Inject
    RapporteringRestTjeneste(@Any Instance<RapportGenerator> rapportGenerators) {
        this.rapportGenerators = rapportGenerators;
    }

    @POST
    @Path("/generer")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(description = "Dumper en rapport av data", summary = ("Henter en dump av info for debugging og analyse av en sak"), tags = "rapportering")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = DRIFT)
    public Response dumpSak(@NotNull @FormParam("ytelseType") @Parameter(description = "ytelseType", required = true) @Valid @TilpassetAbacAttributt(supplierClass = AbacEmptySupplier.class) YtelseTypeKode ytelseTypeKode,
                            @NotNull @FormParam("rapport") @Parameter(description = "rapport", required = true) @Valid @TilpassetAbacAttributt(supplierClass = AbacEmptySupplier.class) RapportType rapportType,
                            @NotNull @FormParam("periode") @Parameter(description = "periode", required = true, example = "2020-01-01/2020-12-31") @Valid @TilpassetAbacAttributt(supplierClass = AbacEmptySupplier.class) Periode periode) {

        class ZipOutput {
            private void addToZip(ZipOutputStream zipOut, DumpOutput dump) {
                var zipEntry = new ZipEntry(dump.getPath());
                try {
                    zipOut.putNextEntry(zipEntry);
                    zipOut.write(dump.getContent().getBytes(Charset.forName("UTF8")));
                    zipOut.closeEntry();
                } catch (IOException e) {
                    throw new IllegalStateException("Kunne ikke zippe dump fra : " + dump, e);
                }
            }

            StreamingOutput dump(List<DumpOutput> outputs) {
                StreamingOutput streamingOutput = outputStream -> {
                    try (ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(outputStream));) {
                        outputs.forEach(dump -> addToZip(zipOut, dump));
                    } finally {
                        outputStream.flush();
                        outputStream.close();
                    }
                };
                return streamingOutput;
            }
        }

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
}
