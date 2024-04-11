package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.kronisksyk;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_KS;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.saksnummer.ReservertSaksnummerEntitet;
import no.nav.k9.sak.behandlingslager.saksnummer.ReservertSaksnummerRepository;
import no.nav.k9.sak.behandlingslager.saksnummer.SaksnummerRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.søknad.innsending.InnsendingInnhold;
import no.nav.k9.sak.mottak.SøknadMottakTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

@FagsakYtelseTypeRef(OMSORGSPENGER_KS)
@ApplicationScoped
public class KroniskSykSøknadMottaker implements SøknadMottakTjeneste<InnsendingInnhold> {

    private FagsakTjeneste fagsakTjeneste;
    private SaksnummerRepository saksnummerRepository;
    private ReservertSaksnummerRepository reservertSaksnummerRepository;
    private KroniskSykVilkårsVurderingTjeneste vilkårsVurderingTjeneste;

    KroniskSykSøknadMottaker() {
        // proxy
    }

    @Inject
    public KroniskSykSøknadMottaker(SaksnummerRepository saksnummerRepository,
                                    ReservertSaksnummerRepository reservertSaksnummerRepository,
                                    FagsakTjeneste fagsakTjeneste,
                                    @Any KroniskSykVilkårsVurderingTjeneste vilkårsVurderingTjeneste) {
        this.fagsakTjeneste = fagsakTjeneste;
        this.saksnummerRepository = saksnummerRepository;
        this.reservertSaksnummerRepository = reservertSaksnummerRepository;
        this.vilkårsVurderingTjeneste = vilkårsVurderingTjeneste;
    }

    @Override
    public Fagsak finnEllerOpprettFagsak(FagsakYtelseType ytelseType, AktørId søkerAktørId, AktørId pleietrengendeAktørId, AktørId relatertPersonAktørId, LocalDate startDato, LocalDate sluttDato, Saksnummer reservertSaksnummer) {
        ytelseType.validerNøkkelParametere(pleietrengendeAktørId, relatertPersonAktørId);
        Objects.requireNonNull(startDato);
        Objects.requireNonNull(pleietrengendeAktørId);
        final var datoIntervall = vilkårsVurderingTjeneste.utledMaksPeriode(DatoIntervallEntitet.fra(startDato, sluttDato), pleietrengendeAktørId);
        final var fagsak = fagsakTjeneste.finnesEnFagsakSomOverlapper(ytelseType, søkerAktørId, pleietrengendeAktørId, relatertPersonAktørId, datoIntervall.getFomDato(), datoIntervall.getTomDato());

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

        final var saksnummer = reservertSaksnummer != null ? reservertSaksnummer : hentReservertEllerGenererSaksnummer(søkerAktørId, pleietrengendeAktørId, datoIntervall.getFomDato().getYear());
        final var nyFagsak = opprettSakFor(saksnummer, søkerAktørId, pleietrengendeAktørId, relatertPersonAktørId, ytelseType, datoIntervall.getFomDato(), datoIntervall.getTomDato());
        reservertSaksnummerRepository.slettHvisEksisterer(saksnummer);
        return nyFagsak;
    }

    private Fagsak opprettSakFor(Saksnummer saksnummer, AktørId brukerIdent, AktørId pleietrengendeAktørId, AktørId relatertPersonAktørId, FagsakYtelseType ytelseType, LocalDate fom, LocalDate tom) {
        var fagsak = Fagsak.opprettNy(ytelseType, brukerIdent, pleietrengendeAktørId, relatertPersonAktørId, saksnummer, fom, tom);
        fagsakTjeneste.opprettFagsak(fagsak);
        return fagsak;
    }

    private Saksnummer hentReservertEllerGenererSaksnummer(AktørId søkerAktørId, AktørId pleietrengendeAktørId, int behandlingsår) {
        var optReservert = reservertSaksnummerRepository.hent(OMSORGSPENGER_KS, søkerAktørId.getAktørId(), pleietrengendeAktørId.getAktørId(), null, Integer.toString(behandlingsår));
        return optReservert.map(ReservertSaksnummerEntitet::getSaksnummer).orElseGet(() -> new Saksnummer(saksnummerRepository.genererNyttSaksnummer()));
    }
}
