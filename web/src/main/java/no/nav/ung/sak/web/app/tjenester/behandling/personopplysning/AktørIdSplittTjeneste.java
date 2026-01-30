package no.nav.ung.sak.web.app.tjenester.behandling.personopplysning;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.domene.abakus.AbakusTjeneste;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.logg.DiagnostikkFagsakLogg;
import no.nav.ung.sak.økonomi.simulering.klient.K9OppdragRestKlient;
import no.nav.ung.sak.økonomi.tilbakekreving.klient.K9TilbakeRestKlient;

@Dependent
public class AktørIdSplittTjeneste {

    public static final String GAMMEL = "gammel_aktoer_id";
    private static final Logger logger = LoggerFactory.getLogger(AktørIdSplittTjeneste.class);
    public static final String GJELDENDE = "ny_aktoer_id";

    private final AktørTjeneste aktørTjeneste;
    private final EntityManager entityManager;
    private final K9OppdragRestKlient oppdragRestKlient;
    private final AbakusTjeneste abakusTjeneste;
    private final AktørBytteFordelKlient fordelKlient;
    private final K9TilbakeRestKlient k9TilbakeRestKlient;

    @Inject
    public AktørIdSplittTjeneste(AktørTjeneste aktørTjeneste,
                                 EntityManager entityManager,
                                 K9OppdragRestKlient oppdragRestKlient,
                                 AbakusTjeneste abakusTjeneste,
                                 AktørBytteFordelKlient fordelKlient,
                                 K9TilbakeRestKlient k9TilbakeRestKlient) {
        this.aktørTjeneste = aktørTjeneste;
        this.entityManager = entityManager;
        this.oppdragRestKlient = oppdragRestKlient;
        this.abakusTjeneste = abakusTjeneste;
        this.fordelKlient = fordelKlient;
        this.k9TilbakeRestKlient = k9TilbakeRestKlient;
    }

    public void patchBrukerAktørId(AktørId nyAktørId,
                                   AktørId gammelAktørId,
                                   Optional<AktørId> aktørIdForIdenterSomSkalByttes,
                                   String begrunnelse,
                                   String tjeneste, boolean skalValidereUtgåttAktør) {
        if (skalValidereUtgåttAktør && aktørTjeneste.hentPersonIdentForAktørId(gammelAktørId).isPresent()) {
            if (Environment.current().isDev() || Environment.current().isLocal()) {//ignorerer sjekk i dev for testbarhet
                logger.warn("Patcher fagsak som har gyldig aktørId.");
            } else {
                throw new IllegalStateException("Fagsaken har gyldig aktørId for bruker - kan ikke patche");
            }
        }
        var nyPersonident = aktørTjeneste.hentPersonIdentForAktørId(nyAktørId);
        if (nyPersonident.isEmpty()) {
            throw new IllegalArgumentException("Ny aktørId er ugyldig - kan ikke patche");
        }
        oppdaterForBruker(nyAktørId, gammelAktørId, aktørIdForIdenterSomSkalByttes, begrunnelse, tjeneste);

        slettIdentFraAktørCache(gammelAktørId);

    }



    private void oppdaterForBruker(AktørId nyAktørId,
                                   AktørId gammelAktørId,
                                   Optional<AktørId> aktørIdForIdenterSomSkalByttes,
                                   String begrunnelse,
                                   String tjeneste) {
        var brukerOppdaterteFagsaker = oppdaterFagsakForBruker(nyAktørId, gammelAktørId);
        oppdaterPoAggregat(nyAktørId, gammelAktørId);
        oppdaterNotatISak(nyAktørId, gammelAktørId);
        var personidenterSomSkalByttesUt = aktørIdForIdenterSomSkalByttes.map(aktørTjeneste::hentHistoriskePersonIdenterForAktørId)
            .orElse(Collections.emptySet());

        var antallRaderEndretIOppdrag = oppdragRestKlient.utførAktørbytte(nyAktørId, gammelAktørId, personidenterSomSkalByttesUt);
        logger.info("Oppdaterte {} rader i oppdrag", antallRaderEndretIOppdrag);

        var antallRaderEndretAbakus = abakusTjeneste.utførAktørbytte(nyAktørId, gammelAktørId);
        logger.info("Oppdaterte {} rader i abakus", antallRaderEndretAbakus);

        var antallRaderEndretFordel = fordelKlient.utførAktørbytte(nyAktørId, gammelAktørId);
        logger.info("Oppdaterte {} rader i fordel", antallRaderEndretFordel);

        var antallRaderEndretTilbake = k9TilbakeRestKlient.utførAktørbytte(nyAktørId, gammelAktørId);
        logger.info("Oppdaterte {} rader i k9-tilbake", antallRaderEndretTilbake);

        brukerOppdaterteFagsaker.forEach(fagsak -> entityManager.persist(new DiagnostikkFagsakLogg(fagsak.getId(), "Oppdatert aktørid for bruker via " + tjeneste, begrunnelse)));
    }

    private void slettIdentFraAktørCache(AktørId gammelAktørId) {
        entityManager.createNativeQuery("delete from tmp_aktoer_id where aktoer_id = :gammel_aktoer_id")
            .setParameter(GAMMEL, gammelAktørId.getAktørId())
            .executeUpdate();
    }


    private void oppdaterNotatISak(AktørId nyAktørId, AktørId gammelAktørId) {
        entityManager.createNativeQuery("update notat_aktoer set aktoer_id = :ny_aktoer_id where aktoer_id = :gammel_aktoer_id")
            .setParameter(GJELDENDE, nyAktørId.getAktørId())
            .setParameter(GAMMEL, gammelAktørId.getAktørId())
            .executeUpdate();
    }

    private List<Fagsak> oppdaterFagsakForBruker(AktørId nyAktørId, AktørId gammelAktørId) {
        var fagsakBrukerQuery = entityManager.createNativeQuery("select * from fagsak where bruker_aktoer_id = :gammel_aktoer_id", Fagsak.class)
            .setParameter(GAMMEL, gammelAktørId.getAktørId());
        List<Fagsak> brukerFagsaker = fagsakBrukerQuery.getResultList();
        int antallRaderBrukerFagsak = entityManager.createNativeQuery("update fagsak set bruker_aktoer_id = :ny_aktoer_id, endret_av='bytte ' || :gammel_aktoer_id, endret_tid=current_timestamp where bruker_aktoer_id = :gammel_aktoer_id")
            .setParameter(GJELDENDE, nyAktørId.getAktørId())
            .setParameter(GAMMEL, gammelAktørId.getAktørId())
            .executeUpdate();
        if (antallRaderBrukerFagsak != brukerFagsaker.size()) {
            throw new IllegalStateException("Forventet å oppdatere " + brukerFagsaker.size() + " rader, men " + antallRaderBrukerFagsak + " ble forsøkt endret.");
        }
        logger.info("oppdaterte følgende saker der gitt aktørid var bruker: {}", brukerFagsaker.stream().map(Fagsak::getSaksnummer).toList());
        return brukerFagsaker;
    }

    private void oppdaterPoAggregat(AktørId nyAktørId, AktørId gammelAktørId) {
        oppdaterPoTabell(gammelAktørId, nyAktørId, "po_relasjon", "fra_aktoer_id", 0, 100);
        oppdaterPoTabell(gammelAktørId, nyAktørId, "po_relasjon", "til_aktoer_id", 0, 100);
        oppdaterPoTabell(gammelAktørId, nyAktørId, "po_personopplysning", "aktoer_id", 1, 100);
    }

    private void oppdaterPoTabell(AktørId gammelAktørId, AktørId nyAktørId, String tabellnavn, String kolonne, int minsteForventeTreff, int maxForventetTreff) {
        //streng validering av parametre som brukes til generering av sql - for å unngå injection ved evt. feil bruk av denne metoden
        if (!tabellnavn.matches("^[a-z_]+$")) {
            throw new IllegalArgumentException("Ugyldig tabellnavn");
        }
        if (!kolonne.matches("^[a-z_]+$")) {
            throw new IllegalArgumentException("Ugyldig kolonnenavn");
        }
        String sql = "update " + tabellnavn + " set " + kolonne + " = :ny_aktoer_id, endret_av='bytte ' || :gammel_aktoer_id, endret_tid=current_timestamp where " + kolonne + " = :gammel_aktoer_id";
        int antall = entityManager.createNativeQuery(sql)
            .setParameter(GJELDENDE, nyAktørId.getAktørId())
            .setParameter(GAMMEL, gammelAktørId.getAktørId())
            .executeUpdate();
        if (antall < minsteForventeTreff || antall > maxForventetTreff) {
            //sanity-sjekk for å avbryte i tilfelle noe skulle være veldig feil med spørringen
            throw new IllegalStateException("Forventet " + minsteForventeTreff + "-" + maxForventetTreff + ", men fikk " + antall + " fra sjekk mot " + tabellnavn + "." + kolonne);
        }
        logger.info("oppdaterte {} rader fra {}.{}", antall, tabellnavn, kolonne);
    }

}
