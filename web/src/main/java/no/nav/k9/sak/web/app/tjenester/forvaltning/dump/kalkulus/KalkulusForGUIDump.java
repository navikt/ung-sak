package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.kalkulus;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.FRISINN;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectWriter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusRestKlient;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagListe;
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
public class KalkulusForGUIDump implements DebugDumpBehandling {

    private final ObjectWriter objectWriter = KalkulusRestKlient.getMapper().writerWithDefaultPrettyPrinter();
    private KalkulusTjenesteAdapter tjeneste;

    KalkulusForGUIDump() {
        // for proxy
    }

    @Inject
    public KalkulusForGUIDump(KalkulusTjenesteAdapter tjeneste) {
        this.tjeneste = tjeneste;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker, Behandling behandling, String basePath) {
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling);
        try {
            Optional<BeregningsgrunnlagListe> data = ContainerContextRunner.doRun(behandling, () -> tjeneste.hentBeregningsgrunnlagForGui(ref));
            if (data.isEmpty()) {
                return;
            }
            dumpMottaker.newFile(basePath + "/kalkulus-beregningsgrunnlag-for-gui.json");
            objectWriter.writeValue(dumpMottaker.getOutputStream(), data.get());
        } catch (Exception e) {
            dumpMottaker.newFile(basePath + "/kalkulus-beregningsgrunnlag-for-gui-ERROR.txt");
            dumpMottaker.write(e);
        }
    }

}
