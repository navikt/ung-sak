package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.kronisksyk;

import java.time.LocalDate;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.saksnummer.SaksnummerRepository;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.søknad.innsending.InnsendingInnhold;
import no.nav.k9.sak.mottak.SøknadMottakTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;

@FagsakYtelseTypeRef("OMP_KS")
@ApplicationScoped
public class KroniskSykSøknadMottaker implements SøknadMottakTjeneste<InnsendingInnhold> {

    private static final Logger log = LoggerFactory.getLogger(KroniskSykSøknadMottaker.class);

    private FagsakTjeneste fagsakTjeneste;
    private SaksnummerRepository saksnummerRepository;
    private PersoninfoAdapter personInfoAdapter;

    KroniskSykSøknadMottaker() {
        // proxy
    }

    @Inject
    public KroniskSykSøknadMottaker(SaksnummerRepository saksnummerRepository,
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
        Objects.requireNonNull(startDato);
        Objects.requireNonNull(pleietrengendeAktørId);
        var personinfo = personInfoAdapter.hentBrukerBasisForAktør(pleietrengendeAktørId).orElseThrow(() -> new IllegalStateException("Fant ikke personinfo for angitt pleietrengende aktørId"));
        var datoIntervall = utledDatoIntervall(personinfo.getFødselsdato(), startDato, sluttDato);
        var fagsak = fagsakTjeneste.finnesEnFagsakSomOverlapper(ytelseType, søkerAktørId, pleietrengendeAktørId, relatertPersonAktørId, datoIntervall.getFomDato(), datoIntervall.getTomDato());
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

    private static DatoIntervallEntitet utledDatoIntervall(LocalDate fødselsdato, LocalDate startDato, LocalDate sluttDato) {
        LocalDate maksDato = DatoIntervallEntitet.TIDENES_ENDE;
        if (sluttDato == null || !sluttDato.isEqual(maksDato)) {
            // overstyrer alltid til tidenens ende, men logger her. Oppdater evt. k9-fordel el.
            if (sluttDato != null) {
                log.warn("overstyrer sluttdato, var {}, setter tidenes ende", sluttDato);
            }
            sluttDato = maksDato;
        }
        if (startDato == null || startDato.isBefore(fødselsdato)) {
            if (startDato != null) {
                log.warn("Overstyrer startdato, var {}, setter til fødselsdato", startDato);
            }
            startDato = fødselsdato;
        }
        var datoIntervall = DatoIntervallEntitet.fraOgMedTilOgMed(startDato, sluttDato);
        return datoIntervall;
    }

}
