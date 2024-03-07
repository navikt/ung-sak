package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aleneomsorg;

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

@FagsakYtelseTypeRef(FagsakYtelseType.OMSORGSPENGER_AO)
@ApplicationScoped
public class AleneomsorgSøknadMottaker implements SøknadMottakTjeneste<InnsendingInnhold> {

    private FagsakTjeneste fagsakTjeneste;
    private SaksnummerRepository saksnummerRepository;
    private ReservertSaksnummerRepository reservertSaksnummerRepository;
    private AleneomsorgVilkårsPerioderTilVurderingTjeneste vilkårsVurderingTjeneste;

    AleneomsorgSøknadMottaker() {
        // proxy
    }

    @Inject
    public AleneomsorgSøknadMottaker(SaksnummerRepository saksnummerRepository,
                                     ReservertSaksnummerRepository reservertSaksnummerRepository,
                                     @Any AleneomsorgVilkårsPerioderTilVurderingTjeneste vilkårsVurderingTjeneste,
                                     FagsakTjeneste fagsakTjeneste) {
        this.vilkårsVurderingTjeneste = vilkårsVurderingTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.saksnummerRepository = saksnummerRepository;
        this.reservertSaksnummerRepository = reservertSaksnummerRepository;
    }

    @Override
    public Fagsak finnEllerOpprettFagsak(FagsakYtelseType ytelseType, AktørId søkerAktørId, AktørId pleietrengendeAktørId, AktørId relatertPersonAktørId, LocalDate startDato, LocalDate sluttDato, Saksnummer reservertSaksnummer) {
        ytelseType.validerNøkkelParametere(pleietrengendeAktørId, relatertPersonAktørId);
        Objects.requireNonNull(startDato);
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

        Saksnummer saksnummer;
        if (reservertSaksnummer != null) {
            saksnummer = reservertSaksnummer;
        } else {
            var optReservert = reservertSaksnummerRepository.hent(FagsakYtelseType.OMSORGSPENGER_AO, søkerAktørId.getAktørId(), pleietrengendeAktørId.getAktørId(), Integer.toString(datoIntervall.getFomDato().getYear()));
            saksnummer = optReservert.map(ReservertSaksnummerEntitet::getSaksnummer).orElseGet(() -> new Saksnummer(saksnummerRepository.genererNyttSaksnummer()));
        }
        final var nyFagsak = opprettSakFor(saksnummer, søkerAktørId, pleietrengendeAktørId, relatertPersonAktørId, ytelseType, datoIntervall.getFomDato(), datoIntervall.getTomDato());
        reservertSaksnummerRepository.slettHvisEksisterer(saksnummer);
        return nyFagsak;
    }

    private Fagsak opprettSakFor(Saksnummer saksnummer, AktørId brukerIdent, AktørId pleietrengendeAktørId, AktørId relatertPersonAktørId, FagsakYtelseType ytelseType, LocalDate fom, LocalDate tom) {
        var fagsak = Fagsak.opprettNy(ytelseType, brukerIdent, pleietrengendeAktørId, relatertPersonAktørId, saksnummer, fom, tom);
        fagsakTjeneste.opprettFagsak(fagsak);
        return fagsak;
    }

}
