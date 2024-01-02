package no.nav.k9.sak.web.app.tjenester.forvaltning.dump;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.web.app.tjenester.forvaltning.CsvOutput;

@ApplicationScoped
@FagsakYtelseTypeRef
public class BehandlingDump implements DebugDumpFagsak {

    private static final Logger logger = LoggerFactory.getLogger(BehandlingDump.class);

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;

    @SuppressWarnings("unused")
    private EntityManager entityManager;

    private Instance<DebugDumpBehandling> behandlingDumpere;

    protected BehandlingDump() {
    }

    @Inject
    protected BehandlingDump(BehandlingRepository behandlingRepository,
                             @Any Instance<DebugDumpBehandling> behandlingDumpere,
                             FagsakRepository fagsakRepository, EntityManager entityManager) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.entityManager = entityManager;
        this.behandlingDumpere = behandlingDumpere;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker) {
        dumpFagsak(dumpMottaker);
        dumpBehandlinger(dumpMottaker);
    }

    private void dumpFagsak(DumpMottaker dumpMottaker) {
        final Fagsak fagsak = fagsakRepository.hentSakGittSaksnummer(dumpMottaker.getFagsak().getSaksnummer())
            .orElseThrow(() -> new IllegalArgumentException("Finner ikke fagsak: " + dumpMottaker.getFagsak().getSaksnummer()));

        final var toCsv = new LinkedHashMap<String, Function<Fagsak, ?>>();
        toCsv.put("id", Fagsak::getId);
        toCsv.put("saksnummer", Fagsak::getSaksnummer);
        toCsv.put("aktoer_id", Fagsak::getAktørId);
        toCsv.put("pleietrengende_aktoer_id", Fagsak::getPleietrengendeAktørId);
        toCsv.put("relatert_person_aktoer_id", Fagsak::getRelatertPersonAktørId);
        toCsv.put("ytelse_type", Fagsak::getYtelseType);
        toCsv.put("periode", Fagsak::getPeriode);
        toCsv.put("status", Fagsak::getStatus);
        toCsv.put("opprettet_tid", Fagsak::getOpprettetTidspunkt);
        toCsv.put("endret_tid", Fagsak::getEndretTidspunkt);
        String output = CsvOutput.dumpAsCsvSingleInput(true, fagsak, toCsv);
        dumpMottaker.newFile("fagsak.csv");
        dumpMottaker.write(output);
    }

    private void dumpBehandlinger(DumpMottaker dumpMottaker) {
        final Fagsak fagsak = dumpMottaker.getFagsak();
        final List<Behandling> behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(fagsak.getSaksnummer());

        for (Behandling behandling : behandlinger) {
            final String path = "behandling-" + behandling.getId();
            final var toCsv = new LinkedHashMap<String, Function<Behandling, ?>>();

            toCsv.put("id", Behandling::getId);
            toCsv.put("uuid", Behandling::getUuid);
            toCsv.put("fagsak_id", Behandling::getFagsakId);
            toCsv.put("aktoer_id", Behandling::getAktørId);
            toCsv.put("behandling_status", Behandling::getStatus);
            toCsv.put("startpunkt", Behandling::getStartpunkt);
            toCsv.put("behandling_resultat_type", Behandling::getBehandlingResultatType);
            toCsv.put("ansvarlig_beslutter", Behandling::getAnsvarligBeslutter);
            toCsv.put("ansvarlig_saksbehandler", Behandling::getAnsvarligSaksbehandler);
            toCsv.put("behandlende_enhet", Behandling::getBehandlendeEnhet);
            toCsv.put("behandlende_enhet_årsak", Behandling::getBehandlendeEnhetÅrsak);
            toCsv.put("behandling_steg", Behandling::getAktivtBehandlingSteg);
            toCsv.put("behandling_steg_status", Behandling::getBehandlingStegStatus);
            toCsv.put("behandling_steg_tilstand", Behandling::getBehandlingStegTilstand);
            toCsv.put("aksjonspunkter", Behandling::getAksjonspunkter);
            toCsv.put("aksjonspunkter_behandlet", Behandling::getBehandledeAksjonspunkter);
            toCsv.put("aksjonspunkter_totrinn", Behandling::getAksjonspunkterMedTotrinnskontroll);
            toCsv.put("aksjonspunkt_på_vent", Behandling::getBehandlingPåVentAksjonspunktDefinisjon);
            toCsv.put("behandling_årsaker", Behandling::getBehandlingÅrsakerTyper);
            toCsv.put("behandling_frist", Behandling::getBehandlingstidFrist);
            toCsv.put("behandling_avsluttet", Behandling::getAvsluttetDato);
            toCsv.put("original_behandling", Behandling::getOriginalBehandlingId);
            toCsv.put("opprettet_tid", Behandling::getOpprettetTidspunkt);
            toCsv.put("endret_tid", Behandling::getEndretTidspunkt);

            String behandlingDumpOutput = CsvOutput.dumpAsCsvSingleInput(true, behandling, toCsv);
            dumpMottaker.newFile(path + "/behandling.csv");
            dumpMottaker.write(behandlingDumpOutput);

            var behandlingDumpstere = FagsakYtelseTypeRef.Lookup.list(DebugDumpBehandling.class, behandlingDumpere, fagsak.getYtelseType());
            for (var inst : behandlingDumpstere) {
                for (var dumper : inst) {
                    logger.info("Dumper fra {} for behandling {}", dumper.getClass().getName(), behandling.getUuid());
                    try {
                        dumper.dump(dumpMottaker, behandling, path);
                    } catch (Exception e) {
                        dumpMottaker.newFile(path + "/" + dumper.getClass().getSimpleName() + "-ERROR.txt");
                        dumpMottaker.write(e);
                    }
                }
            }
        }
    }
}
