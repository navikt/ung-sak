package no.nav.k9.sak.ytelse.opplaeringspenger.mottak;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.saksnummer.SaksnummerRepository;
import no.nav.k9.sak.mottak.SøknadMottakTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

@FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER)
@ApplicationScoped
public class OLPSøknadMottaker implements SøknadMottakTjeneste<OLPSøknadInnsending> {

    private SaksnummerRepository saksnummerRepository;
    private FagsakTjeneste fagsakTjeneste;
    private boolean ytelseAktivert;

    protected OLPSøknadMottaker() {
        // for proxy
    }

    @Inject
    public OLPSøknadMottaker(SaksnummerRepository saksnummerRepository, FagsakTjeneste fagsakTjeneste, @KonfigVerdi(value = "ytelse.olp.aktivert", required = false) boolean ytelseAktivert) {
        this.saksnummerRepository = saksnummerRepository;
        this.fagsakTjeneste = fagsakTjeneste;
        this.ytelseAktivert = ytelseAktivert;
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

        if (!ytelseAktivert) {
            throw new IllegalArgumentException("Ytelsen er ikke aktivert");
        }

        // TODO: Pluss minus 9 måneder, vurder om er dekkende
        final Optional<Fagsak> fagsak = fagsakTjeneste.finnesEnFagsakSomOverlapper(ytelseType, søkerAktørId, pleietrengendeAktørId, null, startDato.minusMonths(9), sluttDato.plusMonths(9));

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
        return opprettSakFor(saksnummer, søkerAktørId, pleietrengendeAktørId, ytelseType, startDato, sluttDato);
    }


    private Fagsak opprettSakFor(Saksnummer saksnummer, AktørId brukerIdent, AktørId pleietrengendeAktørId, FagsakYtelseType ytelseType, LocalDate startDato, LocalDate sluttDato) {
        final Fagsak fagsak = Fagsak.opprettNy(ytelseType, brukerIdent, pleietrengendeAktørId, null, saksnummer, startDato, sluttDato);
        fagsakTjeneste.opprettFagsak(fagsak);
        return fagsak;
    }
}
