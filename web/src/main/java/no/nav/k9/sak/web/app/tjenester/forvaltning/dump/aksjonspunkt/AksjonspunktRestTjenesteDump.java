package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.aksjonspunkt;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectWriter;

import no.nav.k9.felles.exception.TekniskException;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.web.app.jackson.JacksonJsonConfig;
import no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DumpOutput;

@ApplicationScoped
@FagsakYtelseTypeRef
public class AksjonspunktRestTjenesteDump implements DebugDumpBehandling {

    private AksjonspunktRestTjeneste restTjeneste;

    private ObjectWriter ow = new JacksonJsonConfig().getObjectMapper().writerWithDefaultPrettyPrinter();

    AksjonspunktRestTjenesteDump() {
        // for proxy
    }

    @Inject
    public AksjonspunktRestTjenesteDump(AksjonspunktRestTjeneste restTjeneste) {
        this.restTjeneste = restTjeneste;
    }

    @Override
    public List<DumpOutput> dump(Behandling behandling) {
        try (var response = restTjeneste.getAksjonspunkter(new BehandlingUuidDto(behandling.getUuid()));) {
            var entity = response.getEntity();
            String relativePath = "rest/aksjonpunkter";
            if (entity != null) {
                String str;
                try {
                    str = ow.writeValueAsString(entity);
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

}
