package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.vilkår;

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
import no.nav.k9.sak.web.app.tjenester.behandling.vilkår.VilkårRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.ContainerContextRunner;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;

@ApplicationScoped
@FagsakYtelseTypeRef
public class VilkårRestTjenesteDump implements DebugDumpBehandling {

    private VilkårRestTjeneste restTjeneste;

    private ObjectWriter ow = new JacksonJsonConfig().getObjectMapper().writerWithDefaultPrettyPrinter();
    private String relativePath = "rest/vilkår";

    VilkårRestTjenesteDump() {
        // for proxy
    }

    @Inject
    public VilkårRestTjenesteDump(VilkårRestTjeneste restTjeneste) {
        this.restTjeneste = restTjeneste;
    }

    @Override
    public List<DumpOutput> dump(Behandling behandling) {
        try {
            var data = ContainerContextRunner.doRun(behandling, () -> dumpVilkår(behandling));
            return data;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return List.of(new DumpOutput(relativePath + "-rest-tjeneste-ERROR.txt", sw.toString()));
        }

    }

    private List<DumpOutput> dumpVilkår(Behandling behandling) {
        try (var response = restTjeneste.getVilkårV3(new BehandlingUuidDto(behandling.getUuid()));) {
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

}
