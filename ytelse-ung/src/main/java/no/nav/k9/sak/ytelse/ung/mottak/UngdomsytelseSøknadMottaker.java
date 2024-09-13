package no.nav.k9.sak.ytelse.ung.mottak;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.saksnummer.SaksnummerRepository;
import no.nav.k9.sak.mottak.SøknadMottakTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

@FagsakYtelseTypeRef(UNGDOMSYTELSE)
@ApplicationScoped
public class UngdomsytelseSøknadMottaker implements SøknadMottakTjeneste<UngdomsytelseSøknadInnsending> {

    private SaksnummerRepository saksnummerRepository;
    private FagsakTjeneste fagsakTjeneste;


    public UngdomsytelseSøknadMottaker() {
        // for proxy
    }

    @Inject
    public UngdomsytelseSøknadMottaker(SaksnummerRepository saksnummerRepository, FagsakTjeneste fagsakTjeneste) {
        this.saksnummerRepository = saksnummerRepository;
        this.fagsakTjeneste = fagsakTjeneste;
    }

    @Override
    public Fagsak finnEllerOpprettFagsak(FagsakYtelseType ytelseType,
                                         AktørId søkerAktørId,
                                         AktørId pleietrengendeAktørId,
                                         AktørId relatertPersonAktørId,
                                         LocalDate startDato,
                                         LocalDate sluttDato,
                                         Saksnummer reservertSaksnummer) {
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
        final Optional<Fagsak> fagsak = fagsakTjeneste.finnesEnFagsakSomOverlapper(ytelseType, søkerAktørId, null, null, Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE);


        if (fagsak.isPresent()) {
            return fagsak.get();
        }

        final Saksnummer saksnummer = new Saksnummer(saksnummerRepository.genererNyttSaksnummer());
        final Fagsak nyFagsak = opprettSakFor(saksnummer, søkerAktørId, pleietrengendeAktørId, ytelseType, startDato, sluttDato);
        return nyFagsak;
    }

    private Fagsak opprettSakFor(Saksnummer saksnummer, AktørId brukerIdent, AktørId pleietrengendeAktørId, FagsakYtelseType ytelseType, LocalDate startDato, LocalDate sluttDato) {
        final Fagsak fagsak = Fagsak.opprettNy(ytelseType, brukerIdent, pleietrengendeAktørId, null, saksnummer, startDato, sluttDato);
        fagsakTjeneste.opprettFagsak(fagsak);
        return fagsak;
    }

}
