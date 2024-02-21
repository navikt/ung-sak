package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.mottak;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.saksnummer.ReservertSaksnummerRepository;
import no.nav.k9.sak.behandlingslager.saksnummer.SaksnummerRepository;
import no.nav.k9.sak.mottak.SøknadMottakTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@ApplicationScoped
public class PPNSøknadMottaker implements SøknadMottakTjeneste<PPNSøknadInnsending> {

    private SaksnummerRepository saksnummerRepository;
    private ReservertSaksnummerRepository reservertSaksnummerRepository;
    private FagsakTjeneste fagsakTjeneste;

    protected PPNSøknadMottaker() {
        // for proxy
    }

    @Inject
    public PPNSøknadMottaker(SaksnummerRepository saksnummerRepository, ReservertSaksnummerRepository reservertSaksnummerRepository, FagsakTjeneste fagsakTjeneste) {
        this.saksnummerRepository = saksnummerRepository;
        this.reservertSaksnummerRepository = reservertSaksnummerRepository;
        this.fagsakTjeneste = fagsakTjeneste;
    }

    @Override
    public Fagsak finnEllerOpprettFagsak(FagsakYtelseType ytelseType, AktørId søkerAktørId, AktørId pleietrengendeAktørId, AktørId relatertPersonAktørId, LocalDate startDato, LocalDate sluttDato, Saksnummer reservertSaksnummer) {
        ytelseType.validerNøkkelParametere(pleietrengendeAktørId, relatertPersonAktørId);

        if (sluttDato == null) {
            sluttDato = startDato;
        }

        if (sluttDato.isAfter(LocalDate.now().plusYears(5))) {
            // Hvis dette skulle bli nødvendig i fremtiden kan denne sjekken fjernes.
            throw new IllegalArgumentException("Fagsak kan ikke være mer enn 5 år inn i fremtiden.");
        }

        /*
         * Flere fagsaker kommer trolig til å komme tilbake igjen etter at alle sakene har blitt flyttet fra Infotrygd. Merk at sjekken
         * da må gjøres på tvers av alle søkere på den samme pleietrengende for at bruddet i tidslinjen skal gi mening.
         */
        final Optional<Fagsak> fagsak = fagsakTjeneste.finnesEnFagsakSomOverlapper(ytelseType, søkerAktørId, pleietrengendeAktørId, null, Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE);

        if (reservertSaksnummer != null) {
            if (fagsak.isPresent() && !fagsak.get().getSaksnummer().equals(reservertSaksnummer)) {
                throw new IllegalArgumentException("Har allerede en fagsak med annet saksnummer enn reservert saksnummer, saksnummer=" + fagsak.get().getSaksnummer() + ", reservertSaksnummer=" + reservertSaksnummer);
            }
            if (fagsak.isEmpty() && fagsakTjeneste.finnFagsakGittSaksnummer(reservertSaksnummer, false).isPresent()) {
                throw new IllegalArgumentException("Fagsak med reservert saksnummer " + reservertSaksnummer + " eksisterer allerede");
            }
        }

        if (fagsak.isPresent()) {
            return fagsak.get();
        }

        final Saksnummer saksnummer = reservertSaksnummer != null ? reservertSaksnummer : new Saksnummer(saksnummerRepository.genererNyttSaksnummer());
        final Fagsak nyFagsak = opprettSakFor(saksnummer, søkerAktørId, pleietrengendeAktørId, ytelseType, startDato, sluttDato);
        reservertSaksnummerRepository.slettHvisEksisterer(reservertSaksnummer);
        return nyFagsak;
    }


    private Fagsak opprettSakFor(Saksnummer saksnummer, AktørId brukerIdent, AktørId pleietrengendeAktørId, FagsakYtelseType ytelseType, LocalDate startDato, LocalDate sluttDato) {
        final Fagsak fagsak = Fagsak.opprettNy(ytelseType, brukerIdent, pleietrengendeAktørId, null, saksnummer, startDato, sluttDato);
        fagsakTjeneste.opprettFagsak(fagsak);
        return fagsak;
    }
}
