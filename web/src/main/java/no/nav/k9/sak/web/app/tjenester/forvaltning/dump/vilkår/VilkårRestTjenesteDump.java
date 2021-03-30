package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.vilkår;

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
import no.nav.k9.sak.web.app.tjenester.behandling.vilkår.VilkårRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DumpOutput;

@ApplicationScoped
@FagsakYtelseTypeRef
public class VilkårRestTjenesteDump implements DebugDumpBehandling {

    private VilkårRestTjeneste restTjeneste;

    private ObjectWriter ow = new JacksonJsonConfig().getObjectMapper().writerWithDefaultPrettyPrinter();

    VilkårRestTjenesteDump() {
        // for proxy
    }

    @Inject
    public VilkårRestTjenesteDump(VilkårRestTjeneste restTjeneste) {
        this.restTjeneste = restTjeneste;
    }

    @Override
    public List<DumpOutput> dump(Behandling behandling) {
        try (var response = restTjeneste.getVilkårV3(new BehandlingUuidDto(behandling.getUuid()));) {
            var entity = response.getEntity();
            String relativePath = "rest/vilkår";
            if (entity != null) {
                String str;
                try {
                    str = ow.writeValueAsString(entity);
                } catch (TekniskException | IOException e) {
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
