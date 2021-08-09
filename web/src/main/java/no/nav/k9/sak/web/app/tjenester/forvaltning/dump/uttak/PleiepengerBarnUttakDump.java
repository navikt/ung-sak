package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.uttak;

import com.fasterxml.jackson.databind.ObjectWriter;
import no.nav.k9.felles.integrasjon.rest.DefaultJsonMapper;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.UttakRestKlient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
public class PleiepengerBarnUttakDump implements DebugDumpBehandling, DebugDumpFagsak {

    private UttakRestKlient restKlient;
    private BehandlingRepository behandlingRepository;
    private final String fileNameBehandlingPrefix = "pleiepenger-barn-uttaksplan-";
    private final String fileNameBehandlingPosfix = ".json";
    private final ObjectWriter ow = DefaultJsonMapper.getObjectMapper().writerWithDefaultPrettyPrinter();

    PleiepengerBarnUttakDump() {
        // for proxy
    }

    @Inject
    public PleiepengerBarnUttakDump(UttakRestKlient restKlient, BehandlingRepository behandlingRepository) {
        this.restKlient = restKlient;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public List<DumpOutput> dump(Behandling behandling) {
        try {
            var uttaksplan = restKlient.hentUttaksplan(behandling.getUuid());
            var content = ow.writeValueAsString(uttaksplan);
            return List.of(new DumpOutput(fileNameBehandlingPrefix + behandling.getUuid().toString() + fileNameBehandlingPosfix, content));
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return List.of(new DumpOutput(fileNameBehandlingPrefix + "-ERROR", sw.toString()));
        }
    }

    @Override
    public List<DumpOutput> dump(Fagsak fagsak) {
        var behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsak.getId());
        var outputListe = new ArrayList<DumpOutput>();
        for (var behandling : behandlinger) {
            var dumpFileName = fileNameBehandlingPrefix + behandling.getUuid().toString() + fileNameBehandlingPosfix;
            try {
                var uttaksplan = restKlient.hentUttaksplan(behandling.getUuid());
                var content = ow.writeValueAsString(uttaksplan);
                outputListe.add(new DumpOutput(dumpFileName, content));
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                outputListe.add(new DumpOutput(dumpFileName + "-ERROR", sw.toString()));
            }
        }
        return outputListe;
    }

}
