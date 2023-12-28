package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.aksjonspunkt;

import com.fasterxml.jackson.databind.ObjectWriter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.web.app.jackson.JacksonJsonConfig;
import no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktRestTjeneste;
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
    public void dump(DumpMottaker dumpMottaker, Behandling behandling, String basePath) {
        try {
            ContainerContextRunner.doRun(behandling, () -> dumpAksjonspunkter(dumpMottaker, behandling, basePath));
        } catch (Exception e) {
            dumpMottaker.newFile(basePath + "/" + relativePath + "-rest-tjeneste-ERROR.txt");
            dumpMottaker.write(e);
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
            dumpMottaker.newFile(basePath + "/" + relativePath + "-ERROR.txt");
            dumpMottaker.write(e);
        }
        return 1;
    }
}
