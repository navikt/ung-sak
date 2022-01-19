package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.historikk;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.ObjectWriter;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.web.app.jackson.JacksonJsonConfig;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;

@ApplicationScoped
@FagsakYtelseTypeRef
public class HistorikkTjenesteDump implements DebugDumpFagsak {

    private HistorikkTjenesteAdapter historikkTjeneste;

    private ObjectWriter ow = new JacksonJsonConfig().getObjectMapper().writerWithDefaultPrettyPrinter();

    HistorikkTjenesteDump() {
        // for proxy
    }

    @Inject
    public HistorikkTjenesteDump(HistorikkTjenesteAdapter historikkTjeneste) {
        this.historikkTjeneste = historikkTjeneste;
    }

    @Override
    public List<DumpOutput> dump(Fagsak fagsak) {
        var saksnummer = fagsak.getSaksnummer();
        var historikkInnslag = historikkTjeneste.finnHistorikkInnslag(saksnummer);
        var dtoer = historikkTjeneste.mapTilDto(historikkInnslag, saksnummer);

        String relativePath = "historikk";
        if (dtoer != null && !dtoer.isEmpty()) {
            String str;
            try {
                str = ow.writeValueAsString(dtoer);
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                return List.of(new DumpOutput(relativePath + "-ERROR.txt", sw.toString()));
            }

            return List.of(new DumpOutput(relativePath + ".json", str));
        } else {
            return List.of();
        }

    }

}
