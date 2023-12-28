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
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.UttakRestKlient;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
public class PleiepengerBarnUttakDump implements DebugDumpBehandling {

    private UttakRestKlient restKlient;
    private final ObjectWriter ow = DefaultJsonMapper.getObjectMapper().writerWithDefaultPrettyPrinter();

    PleiepengerBarnUttakDump() {
        // for proxy
    }

    @Inject
    public PleiepengerBarnUttakDump(UttakRestKlient restKlient) {
        this.restKlient = restKlient;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker, Behandling behandling, String basePath) {
        try {
            var uttaksplan = restKlient.hentUttaksplan(behandling.getUuid(), false);
            dumpMottaker.newFile(basePath + "/pleiepenger-uttaksplan.json");
            ow.writeValue(dumpMottaker.getOutputStream(), uttaksplan);
        } catch (Exception e) {
            dumpMottaker.newFile(basePath + "/pleiepenger-uttaksplan-ERROR.txt");
            dumpMottaker.write(e);
        }
    }
}
