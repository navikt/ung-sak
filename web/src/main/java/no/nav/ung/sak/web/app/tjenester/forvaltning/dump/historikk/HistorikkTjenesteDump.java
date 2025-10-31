package no.nav.ung.sak.web.app.tjenester.forvaltning.dump.historikk;

import com.fasterxml.jackson.databind.ObjectWriter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.ung.sak.web.app.jackson.ObjectMapperFactory;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

@ApplicationScoped
@FagsakYtelseTypeRef
public class HistorikkTjenesteDump implements DebugDumpFagsak {

    private HistorikkTjenesteAdapter historikkTjeneste;

    private final ObjectWriter ow = ObjectMapperFactory.createBaseObjectMapperCopy().writerWithDefaultPrettyPrinter();

    HistorikkTjenesteDump() {
        // for proxy
    }

    @Inject
    public HistorikkTjenesteDump(HistorikkTjenesteAdapter historikkTjeneste) {
        this.historikkTjeneste = historikkTjeneste;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker) {
        var saksnummer = dumpMottaker.getFagsak().getSaksnummer();
        var historikkInnslag = historikkTjeneste.finnHistorikkInnslag(saksnummer);
        var dtoer = historikkTjeneste.mapTilDto(historikkInnslag, saksnummer);

        if (dtoer != null && !dtoer.isEmpty()) {
            try {
                dumpMottaker.newFile("historikk.json");
                ow.writeValue(dumpMottaker.getOutputStream(), dtoer);
            } catch (Exception e) {
                dumpMottaker.newFile("historikk-ERROR.txt");
                dumpMottaker.write(e);
            }
        }
    }
}
