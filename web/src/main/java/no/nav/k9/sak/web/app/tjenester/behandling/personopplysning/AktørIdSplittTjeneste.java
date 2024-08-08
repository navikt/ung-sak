package no.nav.k9.sak.web.app.tjenester.behandling.personopplysning;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusTjeneste;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.abakus.AbakusTjeneste;
import no.nav.k9.sak.domene.person.pdl.AktørTjeneste;
import no.nav.k9.sak.kontrakt.person.AktørIdDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.logg.DiagnostikkFagsakLogg;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;
import no.nav.k9.sak.økonomi.simulering.klient.K9OppdragRestKlient;
import no.nav.k9.sak.økonomi.tilbakekreving.klient.K9TilbakeRestKlient;

@Dependent
public class AktørIdSplittTjeneste {

    public static final String GAMMEL = "gammel_aktoer_id";
    private static final Logger logger = LoggerFactory.getLogger(AktørIdSplittTjeneste.class);
    public static final String GJELDENDE = "ny_aktoer_id";

    private final AktørTjeneste aktørTjeneste;
    private final EntityManager entityManager;
    private final K9OppdragRestKlient oppdragRestKlient;
    private final AbakusTjeneste abakusTjeneste;
    private final KalkulusTjeneste kalkulusTjeneste;
    private final AktørBytteFordelKlient fordelKlient;
    private final ÅrskvantumTjeneste årskvantumTjeneste;
    private final K9TilbakeRestKlient k9TilbakeRestKlient;

    @Inject
    public AktørIdSplittTjeneste(AktørTjeneste aktørTjeneste,
                                 EntityManager entityManager,
                                 K9OppdragRestKlient oppdragRestKlient,
                                 AbakusTjeneste abakusTjeneste,
                                 KalkulusTjeneste kalkulusTjeneste,
                                 AktørBytteFordelKlient fordelKlient,
                                 ÅrskvantumTjeneste årskvantumTjeneste, K9TilbakeRestKlient k9TilbakeRestKlient) {
        this.aktørTjeneste = aktørTjeneste;
        this.entityManager = entityManager;
        this.oppdragRestKlient = oppdragRestKlient;
        this.abakusTjeneste = abakusTjeneste;
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.fordelKlient = fordelKlient;
        this.årskvantumTjeneste = årskvantumTjeneste;
        this.k9TilbakeRestKlient = k9TilbakeRestKlient;
    }

    public void patchBrukerAktørId(AktørId nyAktørId,
                                   AktørId gammelAktørId,
                                   Optional<AktørId> aktørIdForIdenterSomSkalByttes,
                                   String begrunnelse,
                                   String tjeneste) {
        if (aktørTjeneste.hentPersonIdentForAktørId(gammelAktørId).isPresent()) {
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


        var brukerOppdaterteFagsaker = oppdaterFagsakForBruker(nyAktørId, gammelAktørId);
        var pleietrengedeOppdaterteFagsakIder = oppdaterFagsakForPleietrengende(nyAktørId, gammelAktørId);
        var relatertPersonOppdaterteFagsakIder = oppdaterFagsakForRelatertPerson(nyAktørId, gammelAktørId);

        oppdaterPoAggregat(nyAktørId, gammelAktørId);
        oppdaterPSBGrunnlag(nyAktørId, gammelAktørId);
        oppdaterOMPGrunnlag(nyAktørId, gammelAktørId);
        oppdaterNotatISak(nyAktørId, gammelAktørId);
        oppdaterReservertSaksnummer(nyAktørId, gammelAktørId);
        oppdaterSøknadGrunnlag(nyAktørId, gammelAktørId);
        slettIdentFraAktørCache(gammelAktørId);


        var personidenterSomSkalByttesUt = aktørIdForIdenterSomSkalByttes.map(aktørTjeneste::hentHistoriskePersonIdenterForAktørId)
            .orElse(Collections.emptySet());

        oppdragRestKlient.utførAktørbytte(nyAktørId, gammelAktørId, personidenterSomSkalByttesUt);
        abakusTjeneste.endreAktørId(nyAktørId, gammelAktørId);
        kalkulusTjeneste.opppdaterAktørId(nyAktørId, gammelAktørId);
        fordelKlient.oppdaterAktørId(nyAktørId, gammelAktørId);
        if (brukerOppdaterteFagsaker.stream().map(Fagsak::getYtelseType).anyMatch(FagsakYtelseType.OMSORGSPENGER::equals) && !personidenterSomSkalByttesUt.isEmpty()) {
            årskvantumTjeneste.oppdaterPersonident(nyPersonident.get(), personidenterSomSkalByttesUt);
        }
        k9TilbakeRestKlient.oppdaterAktørId(nyAktørId, gammelAktørId);


        brukerOppdaterteFagsaker.forEach(fagsak -> entityManager.persist(new DiagnostikkFagsakLogg(fagsak.getId(), "Oppdatert aktørid for bruker via " + tjeneste, begrunnelse)));
        pleietrengedeOppdaterteFagsakIder.forEach(id -> entityManager.persist(new DiagnostikkFagsakLogg(id, "Oppdatert aktørid for pleietrengende via " + tjeneste, begrunnelse)));
        relatertPersonOppdaterteFagsakIder.forEach(id -> entityManager.persist(new DiagnostikkFagsakLogg(id, "Oppdatert aktørid for relatert person via " + tjeneste, begrunnelse)));
    }

    private void slettIdentFraAktørCache(AktørId gammelAktørId) {
        entityManager.createNativeQuery("delete from tmp_aktoer_id where aktoer_id = :gammel_aktoer_id")
            .setParameter(GAMMEL, gammelAktørId.getAktørId())
            .executeUpdate();
    }

    private void oppdaterSøknadGrunnlag(AktørId nyAktørId, AktørId gammelAktørId) {
        entityManager.createNativeQuery("update SO_SOEKNAD_ANGITT_PERSON set AKTOER_ID = :ny_aktoer_id where AKTOER_ID = :gammel_aktoer_id")
            .setParameter(GJELDENDE, nyAktørId.getAktørId())
            .setParameter(GAMMEL, gammelAktørId.getAktørId())
            .executeUpdate();
    }

    private void oppdaterReservertSaksnummer(AktørId nyAktørId, AktørId gammelAktørId) {
        entityManager.createNativeQuery("update reservert_saksnummer set BRUKER_AKTOER_ID = :ny_aktoer_id where BRUKER_AKTOER_ID = :gammel_aktoer_id")
            .setParameter(GJELDENDE, nyAktørId.getAktørId())
            .setParameter(GAMMEL, gammelAktørId.getAktørId())
            .executeUpdate();
        entityManager.createNativeQuery("update reservert_saksnummer set PLEIETRENGENDE_AKTOER_ID = :ny_aktoer_id where PLEIETRENGENDE_AKTOER_ID = :gammel_aktoer_id")
            .setParameter(GJELDENDE, nyAktørId.getAktørId())
            .setParameter(GAMMEL, gammelAktørId.getAktørId())
            .executeUpdate();
        entityManager.createNativeQuery("update reservert_saksnummer set relatert_person_aktoer_id = :ny_aktoer_id where relatert_person_aktoer_id = :gammel_aktoer_id")
            .setParameter(GJELDENDE, nyAktørId.getAktørId())
            .setParameter(GAMMEL, gammelAktørId.getAktørId())
            .executeUpdate();
    }

    private void oppdaterNotatISak(AktørId nyAktørId, AktørId gammelAktørId) {
        entityManager.createNativeQuery("update notat_aktoer set aktoer_id = :ny_aktoer_id where aktoer_id = :gammel_aktoer_id")
            .setParameter(GJELDENDE, nyAktørId.getAktørId())
            .setParameter(GAMMEL, gammelAktørId.getAktørId())
            .executeUpdate();
    }

    private void oppdaterOMPGrunnlag(AktørId nyAktørId, AktørId gammelAktørId) {
        entityManager.createNativeQuery("update OMP_FOSTERBARN set aktoer_id = :ny_aktoer_id where aktoer_id = :gammel_aktoer_id")
            .setParameter(GJELDENDE, nyAktørId.getAktørId())
            .setParameter(GAMMEL, gammelAktørId.getAktørId())
            .executeUpdate();
    }

    private void oppdaterPSBGrunnlag(AktørId nyAktørId, AktørId gammelAktørId) {
        entityManager.createNativeQuery("update person set aktoer_id = :ny_aktoer_id where aktoer_id = :gammel_aktoer_id")
            .setParameter(GJELDENDE, nyAktørId.getAktørId())
            .setParameter(GAMMEL, gammelAktørId.getAktørId())
            .executeUpdate();
        entityManager.createNativeQuery("update psb_unntak_etablert_tilsyn_pleietrengende set pleietrengende_aktoer_id = :ny_aktoer_id where pleietrengende_aktoer_id = :gammel_aktoer_id")
            .setParameter(GJELDENDE, nyAktørId.getAktørId())
            .setParameter(GAMMEL, gammelAktørId.getAktørId())
            .executeUpdate();
        entityManager.createNativeQuery("update psb_unntak_etablert_tilsyn_periode set soeker_aktoer_id = :ny_aktoer_id where soeker_aktoer_id = :gammel_aktoer_id")
            .setParameter(GJELDENDE, nyAktørId.getAktørId())
            .setParameter(GAMMEL, gammelAktørId.getAktørId())
            .executeUpdate();
        entityManager.createNativeQuery("update PSB_UNNTAK_ETABLERT_TILSYN_BESKRIVELSE set soeker_aktoer_id = :ny_aktoer_id where soeker_aktoer_id = :gammel_aktoer_id")
            .setParameter(GJELDENDE, nyAktørId.getAktørId())
            .setParameter(GAMMEL, gammelAktørId.getAktørId())
            .executeUpdate();
        entityManager.createNativeQuery("update PSB_INFOTRYGD_PERSON set aktoer_id = :ny_aktoer_id where aktoer_id = :gammel_aktoer_id")
            .setParameter(GJELDENDE, nyAktørId.getAktørId())
            .setParameter(GAMMEL, gammelAktørId.getAktørId())
            .executeUpdate();
    }

    private List<Long> oppdaterFagsakForRelatertPerson(AktørId nyAktørId, AktørId gammelAktørId) {
        var fagsakRelatertPersonQuery = entityManager.createNativeQuery("select * from fagsak where relatert_person_aktoer_id = :gammel_aktoer_id", Fagsak.class)
            .setParameter(GAMMEL, gammelAktørId.getAktørId());
        List<Fagsak> relatertPersonFagsaker = fagsakRelatertPersonQuery.getResultList();
        int antallRaderRelatertPersonFagsak = entityManager.createNativeQuery("update fagsak set relatert_person_aktoer_id = :ny_aktoer_id, endret_av='bytte ' || :gammel_aktoer_id, endret_tid=current_timestamp  where relatert_person_aktoer_id = :gammel_aktoer_id")
            .setParameter(GJELDENDE, nyAktørId.getAktørId())
            .setParameter(GAMMEL, gammelAktørId.getAktørId())
            .executeUpdate();
        if (antallRaderRelatertPersonFagsak != relatertPersonFagsaker.size()) {
            throw new IllegalStateException("Forventet å oppdatere " + relatertPersonFagsaker.size() + " rader, men " + antallRaderRelatertPersonFagsak + " ble forsøkt endret.");
        }
        logger.info("oppdaterte følgende saker der gitt aktørid var relatert person: {}", relatertPersonFagsaker.stream().map(Fagsak::getSaksnummer).toList());
        return relatertPersonFagsaker.stream().map(Fagsak::getId).toList();
    }


    private List<Long> oppdaterFagsakForPleietrengende(AktørId nyAktørId, AktørId gammelAktørId) {
        var fagsakPleietrengendeQuery = entityManager.createNativeQuery("select * from fagsak where pleietrengende_aktoer_id = :gammel_aktoer_id", Fagsak.class)
            .setParameter(GAMMEL, gammelAktørId.getAktørId());
        List<Fagsak> pleietrengendeFagsaker = fagsakPleietrengendeQuery.getResultList();
        int antallRaderPleietrengendeFagsak = entityManager.createNativeQuery("update fagsak set pleietrengende_aktoer_id = :ny_aktoer_id, endret_av='bytte ' || :gammel_aktoer_id, endret_tid=current_timestamp where pleietrengende_aktoer_id = :gammel_aktoer_id")
            .setParameter(GJELDENDE, nyAktørId.getAktørId())
            .setParameter(GAMMEL, gammelAktørId.getAktørId())
            .executeUpdate();
        if (antallRaderPleietrengendeFagsak != pleietrengendeFagsaker.size()) {
            throw new IllegalStateException("Forventet å oppdatere " + pleietrengendeFagsaker.size() + " rader, men " + antallRaderPleietrengendeFagsak + " ble forsøkt endret.");
        }
        logger.info("oppdaterte følgende saker der gitt aktørid var pleietrengende: {}", pleietrengendeFagsaker.stream().map(Fagsak::getSaksnummer).toList());
        return pleietrengendeFagsaker.stream().map(Fagsak::getId).toList();

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
        oppdaterPoTabell(gammelAktørId, nyAktørId, "po_adresse", "aktoer_id", 1, 100);
        oppdaterPoTabell(gammelAktørId, nyAktørId, "po_statsborgerskap", "aktoer_id", 1, 100);
        oppdaterPoTabell(gammelAktørId, nyAktørId, "po_personstatus", "aktoer_id", 1, 100);
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
