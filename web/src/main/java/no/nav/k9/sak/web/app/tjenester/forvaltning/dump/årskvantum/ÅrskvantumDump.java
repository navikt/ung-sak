package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.årskvantum;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectWriter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.rest.DefaultJsonMapper;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumRestKlient;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER)
public class ÅrskvantumDump implements DebugDumpBehandling, DebugDumpFagsak {

    private ÅrskvantumRestKlient restKlient;
    private ÅrskvantumTjeneste årskvantumTjeneste;
    private final String fileNameUttaksplan = "årskvantum-fulluttaksplan.json";
    private final String fileNameRequest = "årskvantum-rammevedtak-request.json";
    private final ObjectWriter ow = DefaultJsonMapper.getObjectMapper().writerWithDefaultPrettyPrinter(); // samme som ÅrskvantumRestklient bruker

    ÅrskvantumDump() {
        // for proxy
    }

    @Inject
    public ÅrskvantumDump(ÅrskvantumRestKlient restKlient, ÅrskvantumTjeneste årskvantumTjeneste) {
        this.restKlient = restKlient;
        this.årskvantumTjeneste = årskvantumTjeneste;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker, Behandling behandling, String basePath) {
        try {
            var uttaksplan = restKlient.hentFullUttaksplanForBehandling(List.of(behandling.getUuid()));
            dumpMottaker.newFile(basePath + "/" + fileNameUttaksplan);
            ow.writeValue(dumpMottaker.getOutputStream(), uttaksplan);
        } catch (Exception e) {
            dumpMottaker.newFile(basePath + "/" + fileNameUttaksplan + "-ERROR");
            dumpMottaker.write(e);
        }
        if (!behandling.erAvsluttet()) {
            try {
                var request = årskvantumTjeneste.lagRammevedtakV2Request(behandling);
                dumpMottaker.newFile(basePath + "/" + fileNameRequest);
                ow.writeValue(dumpMottaker.getOutputStream(), request);
            } catch (Exception e) {
                dumpMottaker.newFile(basePath + "/" + fileNameRequest + "-ERROR");
                dumpMottaker.write(e);
            }
        }
    }

    @Override
    public void dump(DumpMottaker dumpMottaker) {
        try {
            var uttaksplan = restKlient.hentFullUttaksplan(dumpMottaker.getFagsak().getSaksnummer());
            dumpMottaker.newFile(fileNameUttaksplan);
            ow.writeValue(dumpMottaker.getOutputStream(), uttaksplan);
        } catch (Exception e) {
            dumpMottaker.newFile(fileNameUttaksplan + "-ERROR");
            dumpMottaker.write(e);
        }
    }
}
