package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.årskvantum;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.ObjectWriter;

import no.nav.k9.felles.integrasjon.rest.DefaultJsonMapper;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumRestKlient;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER)
public class ÅrskvantumDump implements DebugDumpBehandling, DebugDumpFagsak {

    private ÅrskvantumRestKlient restKlient;
    private final String fileName = "årskvantum-fulluttaksplan.json";
    private final ObjectWriter ow = DefaultJsonMapper.getObjectMapper().writerWithDefaultPrettyPrinter(); // samme som ÅrskvantumRestklient bruker

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
    public void dump(DumpMottaker dumpMottaker, Behandling behandling) {
        try {
            var uttaksplan = restKlient.hentFullUttaksplanForBehandling(List.of(behandling.getUuid()));
            dumpMottaker.newFile(fileName);
            ow.writeValue(dumpMottaker.getOutputStream(), uttaksplan);
        } catch (Exception e) {
            dumpMottaker.writeExceptionToFile(fileName + "-ERROR", e);
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

    @Override
    public void dump(DumpMottaker dumpMottaker) {
        try {
            var uttaksplan = restKlient.hentFullUttaksplan(dumpMottaker.getFagsak().getSaksnummer());
            dumpMottaker.newFile(fileName);
            ow.writeValue(dumpMottaker.getOutputStream(), uttaksplan);
        } catch (Exception e) {
            dumpMottaker.writeExceptionToFile(fileName + "-ERROR", e);
        }
    }
}
