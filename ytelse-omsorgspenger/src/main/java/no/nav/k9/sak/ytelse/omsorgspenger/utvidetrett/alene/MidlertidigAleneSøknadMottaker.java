package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.alene;

import java.time.LocalDate;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.saksnummer.SaksnummerRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.søknad.innsending.InnsendingInnhold;
import no.nav.k9.sak.mottak.SøknadMottakTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;

@FagsakYtelseTypeRef("OMP_MA")
@ApplicationScoped
public class MidlertidigAleneSøknadMottaker implements SøknadMottakTjeneste<InnsendingInnhold> {

    private FagsakTjeneste fagsakTjeneste;
    private SaksnummerRepository saksnummerRepository;

    MidlertidigAleneSøknadMottaker() {
        // proxy
    }

    @Inject
    public MidlertidigAleneSøknadMottaker(SaksnummerRepository saksnummerRepository,
                                          FagsakTjeneste fagsakTjeneste) {
        this.fagsakTjeneste = fagsakTjeneste;
        this.saksnummerRepository = saksnummerRepository;
    }

    @Override
    public Behandling mottaSøknad(Saksnummer saksnummer, JournalpostId journalpostId, InnsendingInnhold søknadInnsending) {
        throw new UnsupportedOperationException("Ikke implementert for /innsending grensesnitt, kun journalpostmottak");
    }

    @Override
    public Fagsak finnEllerOpprettFagsak(FagsakYtelseType ytelseType, AktørId søkerAktørId, AktørId pleietrengendeAktørId, AktørId relatertPersonAktørId, LocalDate startDato, LocalDate sluttDato) {
        ytelseType.validerNøkkelParametere(pleietrengendeAktørId, relatertPersonAktørId);
        Objects.requireNonNull(startDato);
        var datoIntervall = sluttDato != null ? DatoIntervallEntitet.fraOgMedTilOgMed(startDato, sluttDato) : DatoIntervallEntitet.fraOgMed(startDato);
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

}
