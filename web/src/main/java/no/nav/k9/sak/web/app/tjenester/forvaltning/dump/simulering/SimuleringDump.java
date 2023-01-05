package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.simulering;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseOppdrag;
import no.nav.k9.sak.behandlingskontroll.BehandlingModell;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;
import no.nav.k9.sak.økonomi.simulering.klient.K9OppdragRestKlient;
import no.nav.k9.sak.økonomi.tilkjentytelse.TilkjentYtelseTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER)
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
public class SimuleringDump implements DebugDumpBehandling {

    private K9OppdragRestKlient restKlient;

    private TilkjentYtelseTjeneste tilkjentYtelseTjeneste;

    private BehandlingModellRepository behandlingModellRepository;

    private String fileName = "simulering";

    SimuleringDump() {
        // for proxy
    }

    @Inject
    public SimuleringDump(K9OppdragRestKlient restKlient, TilkjentYtelseTjeneste tilkjentYtelseTjeneste, BehandlingModellRepository behandlingModellRepository) {
        this.restKlient = restKlient;
        this.tilkjentYtelseTjeneste = tilkjentYtelseTjeneste;
        this.behandlingModellRepository = behandlingModellRepository;
    }

    @Override
    public List<DumpOutput> dump(Behandling behandling) {
        if (behandling.erAvsluttet()) {
            return List.of(new DumpOutput(fileName + "-NOOP", "Utfører ikke dump av simulering for avsluttede behandlinger"));
        }
        BehandlingModell modell = behandlingModellRepository.getModell(behandling.getType(), behandling.getFagsakYtelseType());
        if (modell.erStegAFørStegB(behandling.getAktivtBehandlingSteg(), BehandlingStegType.SIMULER_OPPDRAG)) {
            return List.of(new DumpOutput(fileName + "-NOOP", "kan ikke utføre simulering enda, behandlingen er i steg " + behandling.getAktivtBehandlingSteg()));
        }
        try {
            TilkjentYtelseOppdrag tilkjentYtelseOppdrag = tilkjentYtelseTjeneste.hentTilkjentYtelseOppdrag(behandling);
            var content = restKlient.utførSimuleringDiagnostikk(tilkjentYtelseOppdrag);
            return List.of(new DumpOutput(fileName, content));
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return List.of(new DumpOutput(fileName + "-ERROR", sw.toString()));
        }
    }


}
