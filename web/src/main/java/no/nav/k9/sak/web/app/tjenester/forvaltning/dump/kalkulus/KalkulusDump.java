package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.kalkulus;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectWriter;

import no.nav.folketrygdloven.kalkulus.mappers.JsonMapper;
import no.nav.k9.felles.exception.TekniskException;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DumpOutput;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
@FagsakYtelseTypeRef("PSB")
@FagsakYtelseTypeRef("FRISINN")
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
            var data = tjeneste.hentBeregningsgrunnlagForGui(ref);
            if (data.isEmpty()) {
                return List.of();
            }
            var content = objectWriter.writeValueAsString(data.get());
            return List.of(new DumpOutput("kalkulus-beregningsgrunnlag-for-gui.json", content));
        } catch (TekniskException | IOException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return List.of(new DumpOutput("kalkulus-beregningsgrunnlag-for-gui-ERROR.txt", sw.toString()));
        }
    }
}
