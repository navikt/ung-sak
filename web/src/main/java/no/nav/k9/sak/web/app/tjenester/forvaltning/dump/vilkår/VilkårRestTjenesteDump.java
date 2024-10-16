package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.vilkår;

import com.fasterxml.jackson.databind.ObjectWriter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.web.app.jackson.ObjectMapperFactory;
import no.nav.k9.sak.web.app.tjenester.behandling.vilkår.VilkårRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.ContainerContextRunner;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

@ApplicationScoped
@FagsakYtelseTypeRef
public class VilkårRestTjenesteDump implements DebugDumpBehandling {

    private VilkårRestTjeneste restTjeneste;

    private final ObjectWriter ow = ObjectMapperFactory.createBaseObjectMapper().writerWithDefaultPrettyPrinter();
    private final String relativePath = "rest/vilkår";

    VilkårRestTjenesteDump() {
        // for proxy
    }

    @Inject
    public VilkårRestTjenesteDump(VilkårRestTjeneste restTjeneste) {
        this.restTjeneste = restTjeneste;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker, Behandling behandling, String basePath) {
        try {
            ContainerContextRunner.doRun(behandling, () -> dumpVilkår(dumpMottaker, behandling, basePath));
        } catch (Exception e) {
            dumpMottaker.newFile(basePath + "/" + relativePath + "-rest-tjeneste-ERROR.txt");
            dumpMottaker.write(e);
        }
    }

    private int dumpVilkår(DumpMottaker dumpMottaker, Behandling behandling, String basePath) {
        try (var response = restTjeneste.getVilkårV3(new BehandlingUuidDto(behandling.getUuid()))) {
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
