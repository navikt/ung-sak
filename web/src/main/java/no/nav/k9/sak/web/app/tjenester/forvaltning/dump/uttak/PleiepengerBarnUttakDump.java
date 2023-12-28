package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.uttak;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import com.fasterxml.jackson.databind.ObjectWriter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.rest.DefaultJsonMapper;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.UttakRestKlient;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
public class PleiepengerBarnUttakDump implements DebugDumpBehandling, DebugDumpFagsak {

    private UttakRestKlient restKlient;
    private BehandlingRepository behandlingRepository;
    private final String fileNameBehandlingPrefix = "pleiepenger-uttaksplan-";
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
    public void dump(DumpMottaker dumpMottaker, Behandling behandling, String basePath) {
        try {
            var uttaksplan = restKlient.hentUttaksplan(behandling.getUuid(), false);
            dumpMottaker.newFile(basePath + "/" + fileNameBehandlingPrefix + behandling.getUuid().toString() + fileNameBehandlingPosfix);
            ow.writeValue(dumpMottaker.getOutputStream(), uttaksplan);
        } catch (Exception e) {
            dumpMottaker.writeExceptionToFile(basePath + "/" + fileNameBehandlingPrefix + "-ERROR", e);
        }
    }

    //TODO er denne unødvendig?
    @Override
    public void dump(DumpMottaker dumpMottaker) {
        var behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(dumpMottaker.getFagsak().getId());
        for (var behandling : behandlinger) {
            final String dumpFileName = fileNameBehandlingPrefix + behandling.getUuid().toString() + fileNameBehandlingPosfix;
            try {
                var uttaksplan = restKlient.hentUttaksplan(behandling.getUuid(), false);
                dumpMottaker.newFile(dumpFileName);
                ow.writeValue(dumpMottaker.getOutputStream(), uttaksplan);
            } catch (Exception e) {
                dumpMottaker.writeExceptionToFile(dumpFileName + "-ERROR", e);
            }
        }
    }
}
