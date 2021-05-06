package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.årskvantum;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectWriter;

import no.nav.k9.felles.integrasjon.rest.DefaultJsonMapper;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumRestKlient;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
public class ÅrskvantumDump implements DebugDumpBehandling, DebugDumpFagsak {

    private ÅrskvantumRestKlient restKlient;
    private String fileName = "årskvantum-fulluttaksplan.json";
    private ObjectWriter ow = DefaultJsonMapper.getObjectMapper().writerWithDefaultPrettyPrinter(); // samme som ÅrskvantumRestklient bruker

    ÅrskvantumDump() {
        // for proxy
    }

    @Inject
    public ÅrskvantumDump(ÅrskvantumRestKlient restKlient) {
        this.restKlient = restKlient;
    }

    @Override
    public List<DumpOutput> dump(Behandling behandling) {
        try {
            var uttaksplan = restKlient.hentFullUttaksplanForBehandling(List.of(behandling.getUuid()));
            var content = ow.writeValueAsString(uttaksplan);
            return List.of(new DumpOutput(fileName, content));
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return List.of(new DumpOutput(fileName + "-ERROR", sw.toString()));
        }
    }

    @Override
    public List<DumpOutput> dump(Fagsak fagsak) {
        try {
            var uttaksplan = restKlient.hentFullUttaksplan(fagsak.getSaksnummer());
            var content = ow.writeValueAsString(uttaksplan);
            return List.of(new DumpOutput(fileName, content));
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return List.of(new DumpOutput(fileName + "-ERROR", sw.toString()));
        }
    }

}
