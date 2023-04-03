package no.nav.k9.sak.web.app.tjenester.behandling.personopplysning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.person.pdl.AktørTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.logg.DiagnostikkFagsakLogg;

@Dependent
public class AktørIdSplittTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(AktørIdSplittTjeneste.class);

    private AktørTjeneste aktørTjeneste;

    private FagsakRepository fagsakRepository;

    private EntityManager entityManager;

    @Inject
    public AktørIdSplittTjeneste(AktørTjeneste aktørTjeneste, FagsakRepository fagsakRepository, EntityManager entityManager) {
        this.aktørTjeneste = aktørTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.entityManager = entityManager;
    }

    public void patchBrukerAktørId(AktørId nyAktørId, Saksnummer saksnummer, String begrunnelse, String tjeneste) {
        Fagsak fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElseThrow();
        AktørId gammelAktørId = fagsak.getAktørId();

        if (fagsak.getYtelseType() != FagsakYtelseType.OMP) {
            throw new IllegalArgumentException("Tjenesten er utviklet for OMP i første omgang");
        }
        if (aktørTjeneste.hentPersonIdentForAktørId(gammelAktørId).isPresent()) {
            throw new IllegalStateException("Fagsaken har gyldig aktørId for bruker - kan ikke patche");
        }
        if (aktørTjeneste.hentPersonIdentForAktørId(nyAktørId).isEmpty()) {
            throw new IllegalArgumentException("Ny aktørId er ugyldig - kan ikke patche");
        }

        int antallRaderFagsak = entityManager.createNativeQuery("update fagsak set bruker_aktoer_id = :ny_aktoer_id, endret_av='bytte ' || :gammel_aktoer_id, endret_tid=current_timestamp where saksnummer = :saksnummer")
            .setParameter("ny_aktoer_id", nyAktørId.getAktørId())
            .setParameter("gammel_aktoer_id", gammelAktørId.getAktørId())
            .setParameter("saksnummer", saksnummer.getVerdi())
            .executeUpdate();
        if (antallRaderFagsak != 1) {
            throw new IllegalArgumentException("Forventet å oppdatere 1 rad, men traff " + antallRaderFagsak + " rader");
        }
        logger.info("oppdaterte {} rader i fagsak", antallRaderFagsak);

        oppdaterPoAggregat(nyAktørId, saksnummer, gammelAktørId);

        entityManager.persist(new DiagnostikkFagsakLogg(fagsak.getId(), tjeneste, begrunnelse));
    }

    private void oppdaterPoAggregat(AktørId nyAktørId, Saksnummer saksnummer, AktørId gammelAktørId) {
        oppdaterPoTabell(gammelAktørId, nyAktørId, saksnummer, "po_relasjon", "fra_aktoer_id", 0, 100);
        oppdaterPoTabell(gammelAktørId, nyAktørId, saksnummer, "po_relasjon", "til_aktoer_id", 0, 100);
        oppdaterPoTabell(gammelAktørId, nyAktørId, saksnummer, "po_personopplysning", "aktoer_id", 1, 100);
        oppdaterPoTabell(gammelAktørId, nyAktørId, saksnummer, "po_adresse", "aktoer_id", 1, 100);
        oppdaterPoTabell(gammelAktørId, nyAktørId, saksnummer, "po_statsborgerskap", "aktoer_id", 1, 100);
        oppdaterPoTabell(gammelAktørId, nyAktørId, saksnummer, "po_personstatus", "aktoer_id", 1, 100);
    }

    private void oppdaterPoTabell(AktørId gammelAktørId, AktørId nyAktørId, Saksnummer saksnummer, String tabellnavn, String kolonne, int minsteForventeTreff, int maxForventetTreff) {
        //streng validering av parametre som brukes til generering av sql - for å unngå injection ved evt. feil bruk av denne metoden
        if (!tabellnavn.matches("^[a-z_]$")) {
            throw new IllegalArgumentException("Ugyldig tabellnavn");
        }
        if (!kolonne.matches("^[a-z_]$")) {
            throw new IllegalArgumentException("Ugyldig kolonnenavn");
        }
        int antall = entityManager.createNativeQuery("update " + tabellnavn + " set " + kolonne + " = :ny_aktoer_id, endret_av='bytte ' || :gammel_aktoer_id, endret_tid=current_timestamp where " + kolonne + " = :gammel_aktoer_id and po_informasjon_id in (select pi.id from fagsak f join behandling b on f.id = b.fagsak_id join gr_personopplysning gp on b.id = gp.behandling_id join po_informasjon pi on (gp.registrert_informasjon_id = pi.id or gp.overstyrt_informasjon_id = pi.id) where f.saksnummer = :saksnummer)")
            .setParameter("ny_aktoer_id", nyAktørId.getAktørId())
            .setParameter("gammel_aktoer_id", gammelAktørId.getAktørId())
            .setParameter("saksnummer", saksnummer.getVerdi())
            .executeUpdate();
        if (antall < minsteForventeTreff || antall > maxForventetTreff) {
            //sanity-sjekk for å avbryte i tilfelle noe skulle være veldig feil med spørringen
            throw new IllegalStateException("Forventet " + minsteForventeTreff + "-" + maxForventetTreff + ", men fikk " + antall + " fra sjekk mot " + tabellnavn + "." + kolonne);
        }
        logger.info("oppdaterte {} rader fra {}.{}", antall, tabellnavn, kolonne);
    }

}
