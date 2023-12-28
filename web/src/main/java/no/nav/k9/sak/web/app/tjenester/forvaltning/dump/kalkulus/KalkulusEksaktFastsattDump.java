package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.kalkulus;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.FRISINN;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectWriter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
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
@FagsakYtelseTypeRef(FRISINN)
public class KalkulusEksaktFastsattDump implements DebugDumpBehandling {


    private final ObjectWriter objectWriter = JsonMapper.getMapper().writerWithDefaultPrettyPrinter();
    private KalkulusTjenesteAdapter tjeneste;

    KalkulusEksaktFastsattDump() {
        // for proxy
    }

    @Inject
    public KalkulusEksaktFastsattDump(KalkulusTjenesteAdapter tjeneste) {
        this.tjeneste = tjeneste;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker, Behandling behandling, String basePath) {
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling);
        try {
            List<Beregningsgrunnlag> data = ContainerContextRunner.doRun(behandling, () -> tjeneste.hentBeregningsgrunnlagFastsatt(ref));
            if (data.isEmpty()) {
                return;
            }
            dumpMottaker.newFile(basePath + "/kalkulus-beregningsgrunnlag-fastsatt.json");
            objectWriter.writeValue(dumpMottaker.getOutputStream(), data);
        } catch (Exception e) {
            dumpMottaker.writeExceptionToFile(basePath + "/kalkulus-beregningsgrunnlag-fastsatt-ERROR.txt", e);
        }
    }

}
