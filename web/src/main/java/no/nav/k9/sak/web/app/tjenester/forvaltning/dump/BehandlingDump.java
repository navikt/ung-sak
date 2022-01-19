package no.nav.k9.sak.web.app.tjenester.forvaltning.dump;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.tjenester.forvaltning.CsvOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;

@ApplicationScoped
@FagsakYtelseTypeRef
public class BehandlingDump implements DebugDumpFagsak {

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
    public List<DumpOutput> dump(Fagsak fagsak) {
        var resultat = new ArrayList<DumpOutput>();

        var saksnummer = fagsak.getSaksnummer();
        var ytelseType = fagsak.getYtelseType();
        resultat.addAll(dumpFagsak(saksnummer));

        resultat.addAll(dumpBehandlinger(ytelseType, saksnummer));
        return Collections.unmodifiableList(resultat);
    }

    private List<DumpOutput> dumpFagsak(Saksnummer saksnummer) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer)
            .orElseThrow(() -> new IllegalArgumentException("Finner ikke fagsak: " + saksnummer));

        var toCsv = new LinkedHashMap<String, Function<Fagsak, ?>>();
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
        return List.of(CsvOutput.dumpAsCsvSingleInput(true, fagsak, "fagsak.csv", toCsv));
    }

    private List<DumpOutput> dumpBehandlinger(FagsakYtelseType ytelseType, Saksnummer saksnummer) {
        var behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(saksnummer);

        var resultat = new ArrayList<DumpOutput>();

        for (var b : behandlinger) {
            var toCsv = new LinkedHashMap<String, Function<Behandling, ?>>();
            var path = "behandling-" + b.getId();

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

            resultat.add(CsvOutput.dumpAsCsvSingleInput(true, b, path + "/behandling.csv", toCsv));

            var dumpstere = FagsakYtelseTypeRef.Lookup.list(DebugDumpBehandling.class, behandlingDumpere, ytelseType.getKode());
            for (var inst : dumpstere) {
                for (var dumper : inst) {
                    dumper.dump(b)
                        .forEach(d -> resultat.add(new DumpOutput(path + "/" + d.getPath(), d.getContent())));
                }
            }

        }

        return Collections.unmodifiableList(resultat);
    }

}
