package no.nav.k9.sak.web.app.tjenester.dokument;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.time.LocalDate;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository.MottattDokument899;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@ApplicationScoped
@Transactional
@Path("/dokument/forvaltning")
@Produces(MediaType.APPLICATION_JSON)
public class ForvaltningMottattDokumentRestTjeneste {

    private MottatteDokumentRepository dokumentRepository;

    ForvaltningMottattDokumentRestTjeneste() {
    }

    @Inject
    public ForvaltningMottattDokumentRestTjeneste(MottatteDokumentRepository dokumentRepository) {
        this.dokumentRepository = dokumentRepository;
    }

    @GET
    @Produces({ MediaType.TEXT_PLAIN })
    @Operation(description = "Hent aksjonspunter for saker", tags = "aksjonspunkt", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer saksnummer med ugyldig innkommende meldinger", content = @Content(mediaType = MediaType.TEXT_PLAIN))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getAksjonspunkter(@SuppressWarnings("unused") @Context Request request) { // NOSONAR

        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);

        var dtos = dokumentRepository.hentTSF_899();
        String csv = mapToCsv(dtos);
        return Response.ok(csv, MediaType.TEXT_PLAIN).cacheControl(cc).build();
    }

    private String mapToCsv(List<MottattDokument899> dtos) {
        var sb = new StringBuilder(2048);

        // quick and dirt til csv. Kan godt forbedres. Ingen av feltene trenger escaping

        // headere, pass på rekkefølge her!
        sb.append("journalpostId,saksnummer,fraværFom,fraværTom,delvisFraværDato\n");

        for (var d : dtos) {
            // ingen av feltene trenger escaping så langt - kun id og kodeverdier
            var jid = d.getJournalpostId();
            var sn = d.getSaksnummer();
            var ffom = toString(d.getFraværFom());
            var ftom = toString(d.getFraværTom());
            var ddato = toString(d.getDelvisFraværDato());

            Object[] args = new Object[] { jid, sn, ffom, ftom, ddato };
            String fmt = "%s,".repeat(args.length);
            var s = String.format(fmt.substring(0, fmt.length() - 1), args);
            sb.append(s).append('\n');
        }

        return sb.toString();
    }

    private String toString(LocalDate dato) {
        return dato == null ? "" : dato.toString();
    }
}
