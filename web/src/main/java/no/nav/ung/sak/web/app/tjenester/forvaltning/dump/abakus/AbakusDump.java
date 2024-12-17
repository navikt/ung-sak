package no.nav.ung.sak.web.app.tjenester.forvaltning.dump.abakus;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import com.fasterxml.jackson.databind.ObjectWriter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.abakus.iaygrunnlag.JsonObjectMapper;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.ContainerContextRunner;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

@ApplicationScoped
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
public class AbakusDump implements DebugDumpBehandling {

    private final ObjectWriter iayMapper = JsonObjectMapper.getMapper().writerWithDefaultPrettyPrinter();
    private AbakusTjenesteAdapter tjeneste;

    AbakusDump() {
        // for proxy
    }

    @Inject
    public AbakusDump(AbakusTjenesteAdapter tjeneste) {
        this.tjeneste = tjeneste;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker, Behandling behandling, String basePath) {
        try {
            // For kall med systembruker og opprettelse av systemtoken
            var data = ContainerContextRunner.doRun(behandling, () -> tjeneste.finnGrunnlag(behandling.getId()));
            if (data.isEmpty()) {
                return;
            }
            dumpMottaker.newFile(basePath + "/abakus-iaygrunnlag.json");
            iayMapper.writeValue(dumpMottaker.getOutputStream(), data.get());
        } catch (Exception e) {
            dumpMottaker.newFile(basePath + "/abakus-iaygrunnlag-ERROR.txt");
            dumpMottaker.write(e);
        }
    }

}
