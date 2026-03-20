package no.nav.ung.sak.web.app.tjenester.forvaltning.dump;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingAnsvarlig;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingAnsvarligRepository;
import no.nav.ung.sak.web.app.tjenester.forvaltning.CsvOutput;

import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.function.Function;

@ApplicationScoped
@FagsakYtelseTypeRef
public class BehandlingAnsvarligeDump implements DebugDumpBehandling {

    private BehandlingAnsvarligRepository behandlingAnsvarligRepository;

    BehandlingAnsvarligeDump() {
        //for CDI proxy
    }

    @Inject
    public BehandlingAnsvarligeDump(BehandlingAnsvarligRepository behandlingAnsvarligRepository) {
        this.behandlingAnsvarligRepository = behandlingAnsvarligRepository;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker, Behandling behandling, String basePath) {
        Optional<BehandlingAnsvarlig> ba = behandlingAnsvarligRepository.hentBehandlingAnsvarlig(behandling.getId());
        ba.ifPresent(it -> {
            final String path = "behandling-" + behandling.getId();
            final var toCsv = new LinkedHashMap<String, Function<BehandlingAnsvarlig, ?>>();

            toCsv.put("behandling_id", BehandlingAnsvarlig::getBehandlingId);
            toCsv.put("behandling_del", BehandlingAnsvarlig::getBehandlingDel);
            toCsv.put("ansvarlig_saksbehandler", BehandlingAnsvarlig::getAnsvarligSaksbehandler);
            toCsv.put("ansvarlig_beslutter", BehandlingAnsvarlig::getAnsvarligBeslutter);
            toCsv.put("behandlende_enhet", BehandlingAnsvarlig::getBehandlendeEnhet);
            toCsv.put("behandlende_enhet_årsak", BehandlingAnsvarlig::getBehandlendeEnhetÅrsak);

            String behandlingDumpOutput = CsvOutput.dumpAsCsvSingleInput(true, it, toCsv);
            dumpMottaker.newFile(path + "/behandling-ansvarlige.csv");
            dumpMottaker.write(behandlingDumpOutput);
        });
    }

}
