package no.nav.ung.sak.web.app.tjenester.forvaltning.dump.simulering;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;
import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseOppdrag;
import no.nav.ung.sak.behandlingskontroll.BehandlingModell;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;
import no.nav.ung.sak.økonomi.simulering.klient.K9OppdragRestKlient;
import no.nav.ung.sak.økonomi.tilkjentytelse.TilkjentYtelseTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER)
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
public class SimuleringDump implements DebugDumpBehandling {

    private K9OppdragRestKlient restKlient;

    private TilkjentYtelseTjeneste tilkjentYtelseTjeneste;

    private BehandlingModellRepository behandlingModellRepository;

    private final String fileName = "simulering";

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
    public void dump(DumpMottaker dumpMottaker, Behandling behandling, String basePath) {
        if (behandling.erAvsluttet()) {
            dumpMottaker.newFile(basePath + "/" + fileName + "-NOOP");
            dumpMottaker.write("Utfører ikke dump av simulering for avsluttede behandlinger");
            return;
        }
        BehandlingModell modell = behandlingModellRepository.getModell(behandling.getType(), behandling.getFagsakYtelseType());
        if (modell.erStegAFørStegB(behandling.getAktivtBehandlingSteg(), BehandlingStegType.SIMULER_OPPDRAG)) {
            dumpMottaker.newFile(basePath + "/" + fileName + "-NOOP");
            dumpMottaker.write("kan ikke utføre simulering enda, behandlingen er i steg " + behandling.getAktivtBehandlingSteg());
            return;
        }
        try {
            TilkjentYtelseOppdrag tilkjentYtelseOppdrag = tilkjentYtelseTjeneste.hentTilkjentYtelseOppdrag(behandling);
            String content = restKlient.utførSimuleringDiagnostikk(tilkjentYtelseOppdrag);
            dumpMottaker.newFile(basePath + "/" + fileName);
            dumpMottaker.write(content);
        } catch (Exception e) {
            dumpMottaker.newFile(basePath + "/" + fileName + "-ERROR");
            dumpMottaker.write(e);
        }
    }

}
