package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.kalkulus;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import com.fasterxml.jackson.databind.ObjectWriter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.kalkulus.mappers.JsonMapper;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.ContainerContextRunner;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER)
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
public class PerioderTilVurderingIBeregningDump implements DebugDumpBehandling {

    private final ObjectWriter objectWriter = JsonMapper.getMapper().writerWithDefaultPrettyPrinter();
    private KalkulusTjenesteAdapter tjeneste;

    PerioderTilVurderingIBeregningDump() {
        // for proxy
    }

    @Inject
    public PerioderTilVurderingIBeregningDump(KalkulusTjenesteAdapter tjeneste) {
        this.tjeneste = tjeneste;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker, Behandling behandling, String basePath) {
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling);
        try {
            var data = ContainerContextRunner.doRun(behandling, () -> tjeneste.hentKoblingerForPerioderTilVurdering(ref));
            dumpMottaker.newFile(basePath + "/perioder-til-vurdering-beregning.json");
            objectWriter.writeValue(dumpMottaker.getOutputStream(), data);
        } catch (Exception e) {
            dumpMottaker.newFile(basePath + "/perioder-til-vurdering-beregning-ERROR.txt");
            dumpMottaker.write(e);
        }
    }

}
