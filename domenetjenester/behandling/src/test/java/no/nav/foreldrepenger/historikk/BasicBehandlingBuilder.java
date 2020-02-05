package no.nav.foreldrepenger.historikk;

import static java.time.Month.JANUARY;
import static no.nav.k9.kodeverk.person.NavBrukerKjønn.KVINNE;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Saksnummer;

/** Enkel builder for å lage en enkel behandling for internt bruk her. */
public class BasicBehandlingBuilder {

    private EntityManager em;

    private Fagsak fagsak;

    private final AktørId aktørId = AktørId.dummy();

    private Map<AktørId, NavBruker> lagredeBrukere = new HashMap<>();

    public BasicBehandlingBuilder(EntityManager em) {
        this.em = em;
    }

    public Fagsak opprettFagsak(FagsakYtelseType ytelse) {
        if (fagsak != null) {
            assert fagsak.getYtelseType().equals(ytelse) : "Feil ytelsetype - kan ikke gjenbruke fagsak: " + fagsak;
            return fagsak;
        }
        return opprettFagsak(ytelse, aktørId);
    }

    public Fagsak opprettFagsak(FagsakYtelseType ytelse, AktørId aktørId) {

        var bruker = lagredeBrukere.computeIfAbsent(aktørId, aid -> {
            return NavBruker.opprettNy(
                new Personinfo.Builder()
                    .medAktørId(aid)
                    .medPersonIdent(new PersonIdent("12345678901"))
                    .medNavn("Kari Nordmann")
                    .medFødselsdato(LocalDate.of(1990, JANUARY, 1))
                    .medForetrukketSpråk(Språkkode.nb)
                    .medKjønn(KVINNE)
                    .build());
        });
        em.persist(bruker);

        // Opprett fagsak
        String randomSaksnummer = System.nanoTime() + "";
        this.fagsak = Fagsak.opprettNy(ytelse, bruker, new Saksnummer(randomSaksnummer));
        em.persist(fagsak);
        em.flush();
        return fagsak;
    }
}
