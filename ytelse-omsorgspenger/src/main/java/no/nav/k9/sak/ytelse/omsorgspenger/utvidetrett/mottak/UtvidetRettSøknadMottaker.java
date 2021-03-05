package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.mottak;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.saksnummer.SaksnummerRepository;
import no.nav.k9.sak.domene.person.pdl.AktørTjeneste;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.kontrakt.søknad.innsending.InnsendingInnhold;
import no.nav.k9.sak.mottak.SøknadMottakTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;

@FagsakYtelseTypeRef("OMP_KS")
@FagsakYtelseTypeRef("OMP_MA")
@ApplicationScoped
public class UtvidetRettSøknadMottaker implements SøknadMottakTjeneste<InnsendingInnhold> {

    private FagsakTjeneste fagsakTjeneste;
    private SaksnummerRepository saksnummerRepository;
    private PersoninfoAdapter personInfoAdapter;

    UtvidetRettSøknadMottaker() {
        // proxy
    }

    @Inject
    public UtvidetRettSøknadMottaker(SaksnummerRepository saksnummerRepository,
                                     FagsakTjeneste fagsakTjeneste,
                                     PersoninfoAdapter personInfoAdapter) {
        this.fagsakTjeneste = fagsakTjeneste;
        this.saksnummerRepository = saksnummerRepository;
        this.personInfoAdapter = personInfoAdapter;
    }

    @Override
    public Behandling mottaSøknad(Saksnummer saksnummer, JournalpostId journalpostId, InnsendingInnhold søknadInnsending) {
        throw new UnsupportedOperationException("Ikke implementert for /innsending grensesnitt, kun journalpostmottak");
    }

    @Override
    public Fagsak finnEllerOpprettFagsak(FagsakYtelseType ytelseType, AktørId søkerAktørId, AktørId pleietrengendeAktørId, AktørId relatertPersonAktørId, LocalDate startDato, LocalDate sluttDato) {
        ytelseType.validerNøkkelParametere(pleietrengendeAktørId, relatertPersonAktørId);

        if (pleietrengendeAktørId != null) {
            var personinfo = personInfoAdapter.hentBrukerBasisForAktør(pleietrengendeAktørId).orElseThrow(() -> new IllegalStateException("Fant ikke personinfo for angitt pleietrengende aktørId"));
            var fødselsdato = personinfo.getFødselsdato();
            LocalDate maksdato = fødselsdato.plusYears(18).withMonth(12).withDayOfMonth(31); // slutt av kalenderår 18 år
            if (sluttDato == null || sluttDato.isAfter(maksdato)) {
                sluttDato = maksdato;
            }
            if (startDato == null || startDato.isBefore(fødselsdato)) {
                startDato = fødselsdato;
            }
        }

        var fagsak = fagsakTjeneste.finnesEnFagsakSomOverlapper(ytelseType, søkerAktørId, pleietrengendeAktørId, relatertPersonAktørId, startDato, sluttDato);
        if (fagsak.isPresent()) {
            return fagsak.get();
        }

        var saksnummer = new Saksnummer(saksnummerRepository.genererNyttSaksnummer());

        return opprettSakFor(saksnummer, søkerAktørId, pleietrengendeAktørId, relatertPersonAktørId, ytelseType, startDato, sluttDato);
    }

    private Fagsak opprettSakFor(Saksnummer saksnummer, AktørId brukerIdent, AktørId pleietrengendeAktørId, AktørId relatertPersonAktørId, FagsakYtelseType ytelseType, LocalDate fom, LocalDate tom) {
        var fagsak = Fagsak.opprettNy(ytelseType, brukerIdent, pleietrengendeAktørId, relatertPersonAktørId, saksnummer, fom, tom);
        fagsakTjeneste.opprettFagsak(fagsak);
        return fagsak;
    }

}
