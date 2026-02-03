package no.nav.ung.ytelse.aktivitetspenger.mottak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.FagsakTjeneste;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.saksnummer.SaksnummerRepository;
import no.nav.ung.sak.mottak.SøknadMottakTjeneste;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;

import java.time.LocalDate;
import java.util.Optional;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.AKTIVITETSPENGER;

@FagsakYtelseTypeRef(AKTIVITETSPENGER)
@ApplicationScoped
public class AktivitetspengerSøknadMottaker implements SøknadMottakTjeneste {

    private SaksnummerRepository saksnummerRepository;
    private FagsakTjeneste fagsakTjeneste;


    public AktivitetspengerSøknadMottaker() {
        // for proxy
    }

    @Inject
    public AktivitetspengerSøknadMottaker(SaksnummerRepository saksnummerRepository, FagsakTjeneste fagsakTjeneste) {
        this.saksnummerRepository = saksnummerRepository;
        this.fagsakTjeneste = fagsakTjeneste;
    }


    @Override
    public Fagsak finnEksisterendeFagsak(FagsakYtelseType ytelseType,
                                         AktørId søkerAktørId) {
        return fagsakTjeneste.finnesEnFagsakSomOverlapper(ytelseType, søkerAktørId, Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE)
            .orElseThrow(() -> new IllegalStateException("Finner ikke fagsak for ytelseType: " + ytelseType + " og aktørId: " + søkerAktørId)); // OK logging, maskeres delvis og vi må vite aktørid her
    }

    @Override
    public Fagsak finnEllerOpprettFagsak(FagsakYtelseType ytelseType,
                                         AktørId søkerAktørId,
                                         LocalDate startDato,
                                         LocalDate sluttDato) {
        final Optional<Fagsak> fagsak = fagsakTjeneste.finnesEnFagsakSomOverlapper(ytelseType, søkerAktørId, Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE);


        if (fagsak.isPresent()) {
            return fagsak.get();
        }

        final Saksnummer saksnummer = new Saksnummer(saksnummerRepository.genererNyttSaksnummer());
        final Fagsak nyFagsak = opprettSakFor(saksnummer, søkerAktørId, ytelseType, startDato, sluttDato);
        return nyFagsak;
    }

    public Fagsak finnEllerOpprettFagsakForIkkeDigitalBruker(FagsakYtelseType ytelseType,
                                         AktørId søkerAktørId,
                                         LocalDate startDato,
                                         LocalDate sluttDato) {
        final Optional<Fagsak> fagsak = fagsakTjeneste.finnesEnFagsakSomOverlapper(ytelseType, søkerAktørId, Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE);


        if (fagsak.isPresent()) {
            return fagsak.get();
        }

        final Saksnummer saksnummer = new Saksnummer(saksnummerRepository.genererNyttSaksnummer());
        final Fagsak nyFagsak = opprettSakForIkkeDigitalBruker(saksnummer, søkerAktørId, ytelseType, startDato, sluttDato);
        return nyFagsak;
    }

    private Fagsak opprettSakForIkkeDigitalBruker(Saksnummer saksnummer, AktørId søkerAktørId, FagsakYtelseType ytelseType, LocalDate startDato, LocalDate sluttDato) {
        final Fagsak fagsak = Fagsak.opprettNyForIkkeDigitalBruker(ytelseType, søkerAktørId, saksnummer, startDato, sluttDato);
        fagsakTjeneste.opprettFagsak(fagsak);
        return fagsak;
    }

    private Fagsak opprettSakFor(Saksnummer saksnummer, AktørId brukerIdent, FagsakYtelseType ytelseType, LocalDate startDato, LocalDate sluttDato) {
        final Fagsak fagsak = Fagsak.opprettNy(ytelseType, brukerIdent, saksnummer, startDato, sluttDato);
        fagsakTjeneste.opprettFagsak(fagsak);
        return fagsak;
    }

}
