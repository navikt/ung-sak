package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.kalkulus;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.FRISINN;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.ObjectWriter;

import no.nav.folketrygdloven.kalkulus.mappers.JsonMapper;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.ContainerContextRunner;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER)
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(FRISINN)
public class KalkulusDump implements DebugDumpBehandling {


    private final ObjectWriter objectWriter = JsonMapper.getMapper().writerWithDefaultPrettyPrinter();
    private KalkulusTjenesteAdapter tjeneste;

    KalkulusDump() {
        // for proxy
    }

    @Inject
    public KalkulusDump(KalkulusTjenesteAdapter tjeneste) {
        this.tjeneste = tjeneste;
    }

    @Override
    public List<DumpOutput> dump(Behandling behandling) {
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling);
        try {
            var data = ContainerContextRunner.doRun(behandling, () -> tjeneste.hentBeregningsgrunnlagForGui(ref));

            if (data.isEmpty()) {
                return List.of();
            }
            var content = objectWriter.writeValueAsString(data.get());
            return List.of(new DumpOutput("kalkulus-beregningsgrunnlag-for-gui.json", content));
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return List.of(new DumpOutput("kalkulus-beregningsgrunnlag-for-gui-ERROR.txt", sw.toString()));
        }
    }


}
