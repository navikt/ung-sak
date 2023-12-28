package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.aksjonspunkt;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.ObjectWriter;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.web.app.jackson.JacksonJsonConfig;
import no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.ContainerContextRunner;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

@ApplicationScoped
@FagsakYtelseTypeRef
public class AksjonspunktRestTjenesteDump implements DebugDumpBehandling {

    private AksjonspunktRestTjeneste restTjeneste;

    private final ObjectWriter ow = new JacksonJsonConfig().getObjectMapper().writerWithDefaultPrettyPrinter();

    private final String relativePath = "rest/aksjonpunkter";

    AksjonspunktRestTjenesteDump() {
        // for proxy
    }

    @Inject
    public AksjonspunktRestTjenesteDump(AksjonspunktRestTjeneste restTjeneste) {
        this.restTjeneste = restTjeneste;
    }

    @Override
    public List<DumpOutput> dump(Behandling behandling) {

        try {
            return ContainerContextRunner.doRun(behandling, () -> dumpAksjonspunkter(behandling));
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return List.of(new DumpOutput(relativePath + "-rest-tjenese-ERROR.txt", sw.toString()));
        }

    }

    private List<DumpOutput> dumpAksjonspunkter(Behandling behandling) {
        try (var response = restTjeneste.getAksjonspunkter(new BehandlingUuidDto(behandling.getUuid()));) {
            var entity = response.getEntity();
            if (entity != null) {
                String str = ow.writeValueAsString(entity);
                return List.of(new DumpOutput(relativePath + ".json", str));
            } else {
                return List.of();
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return List.of(new DumpOutput(relativePath + "-ERROR.txt", sw.toString()));
        }
    }

    @Override
    public void dump(DumpMottaker dumpMottaker, Behandling behandling, String basePath) {
        try {
            ContainerContextRunner.doRun(behandling, () -> dumpAksjonspunkter(dumpMottaker, behandling, basePath));
        } catch (Exception e) {
            dumpMottaker.writeExceptionToFile(basePath + "/" + relativePath + "-rest-tjenese-ERROR.txt", e);
        }
    }

    private int dumpAksjonspunkter(DumpMottaker dumpMottaker, Behandling behandling, String basePath) {
        try (var response = restTjeneste.getAksjonspunkter(new BehandlingUuidDto(behandling.getUuid()))) {
            Object entity = response.getEntity();
            if (entity != null) {
                dumpMottaker.newFile(basePath + "/" + relativePath + ".json");
                ow.writeValue(dumpMottaker.getOutputStream(), entity);
            }
        } catch (Exception e) {
            dumpMottaker.writeExceptionToFile(basePath + "/" + relativePath + "-ERROR.txt", e);
        }
        return 1;
    }
}
