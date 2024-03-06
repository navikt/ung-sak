package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import java.time.LocalDate;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.saksnummer.ReservertSaksnummerEntitet;
import no.nav.k9.sak.behandlingslager.saksnummer.ReservertSaksnummerRepository;
import no.nav.k9.sak.behandlingslager.saksnummer.SaksnummerRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.mottak.SøknadMottakTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

@FagsakYtelseTypeRef(OMSORGSPENGER)
@ApplicationScoped
public class OmsorgspengerSøknadMottaker implements SøknadMottakTjeneste<OmsorgspengerSøknadInnsending> {

    private final static Logger logger = LoggerFactory.getLogger(OmsorgspengerSøknadMottaker.class);
    private static final int CUT_OFF_OMP = 2020;
    private FagsakTjeneste fagsakTjeneste;
    private SaksnummerRepository saksnummerRepository;
    private ReservertSaksnummerRepository reservertSaksnummerRepository;

    OmsorgspengerSøknadMottaker() {
        // proxy
    }

    @Inject
    public OmsorgspengerSøknadMottaker(SaksnummerRepository saksnummerRepository, ReservertSaksnummerRepository reservertSaksnummerRepository, FagsakTjeneste fagsakTjeneste) {
        this.fagsakTjeneste = fagsakTjeneste;
        this.saksnummerRepository = saksnummerRepository;
        this.reservertSaksnummerRepository = reservertSaksnummerRepository;
    }

    @Override
    public Fagsak finnEllerOpprettFagsak(FagsakYtelseType ytelseType, AktørId søkerAktørId, AktørId pleietrengendeAktørId, AktørId relatertPersonAktørId, LocalDate startDato, LocalDate sluttDato, Saksnummer reservertSaksnummer) {
        ytelseType.validerNøkkelParametere(pleietrengendeAktørId, relatertPersonAktørId);
        final var fagsak = fagsakTjeneste.finnesEnFagsakSomOverlapper(ytelseType, søkerAktørId, null, null, startDato, sluttDato);

        if (reservertSaksnummer != null) {
            if (fagsak.isPresent() && !fagsak.get().getSaksnummer().equals(reservertSaksnummer)) {
                throw new IllegalArgumentException("Har allerede en fagsak med annet saksnummer enn reservert saksnummer, saksnummer=" + fagsak.get().getSaksnummer() + ", reservertSaksnummer=" + reservertSaksnummer);
            }
            if (fagsak.isEmpty() && fagsakTjeneste.finnFagsakGittSaksnummer(reservertSaksnummer, false).isPresent()) {
                throw new IllegalArgumentException("Fagsak med reservert saksnummer " + reservertSaksnummer + " eksisterer allerede");
            }
        }

        if (fagsak.isPresent()) {
            logger.info("Fant fagsak {} for periode {}/{}", fagsak.get().getSaksnummer().getVerdi(), startDato, sluttDato);
            return fagsak.get();
        }

        LocalDate idag = LocalDate.now();
        var detteÅret = DatoIntervallEntitet.fraOgMedTilOgMed(idag.withDayOfYear(1), idag.withMonth(12).withDayOfMonth(31));
        var ettÅrTilbake = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusYears(1).withDayOfYear(1), idag.minusYears(1).withMonth(12).withDayOfMonth(31));
        var toÅrTilbake = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusYears(2).withDayOfYear(1), idag.minusYears(2).withMonth(12).withDayOfMonth(31));
        var treÅrTilbake = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusYears(3).withDayOfYear(1), idag.minusYears(3).withMonth(12).withDayOfMonth(31));
        var angittPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(startDato, sluttDato);

        for (var p : Arrays.asList(detteÅret, ettÅrTilbake, toÅrTilbake, treÅrTilbake)) {
            if (p.overlapper(angittPeriode)) {
                if (p.getFomDato().getYear() >= CUT_OFF_OMP) {
                    // ta utgangspunkt i året i år først, sjekk deretter fjoråret. Men ikke tillatt 2019 eller tidligere her
                    final Saksnummer saksnummer = reservertSaksnummer != null ? reservertSaksnummer : hentReservertEllerGenererSaksnummer(søkerAktørId, p.getFomDato().getYear());
                    final Fagsak nyFagsak = opprettSakFor(saksnummer, søkerAktørId, pleietrengendeAktørId, ytelseType, p.getFomDato(), p.getTomDato());
                    logger.info("Opprettet fagsak {} med periode {}/{}. Etterspurte fagsak for periode {}/{}", nyFagsak.getSaksnummer().getVerdi(), p.getFomDato(), p.getTomDato(), startDato, sluttDato);
                    reservertSaksnummerRepository.slettHvisEksisterer(saksnummer);
                    return nyFagsak;
                }
            }
        }

        throw new IllegalArgumentException("Kan ikke opprette " + ytelseType + " sak for periode: " + angittPeriode);
    }

    private Fagsak opprettSakFor(Saksnummer saksnummer, AktørId brukerIdent, AktørId pleietrengendeAktørId, FagsakYtelseType ytelseType, LocalDate fom, LocalDate tom) {
        var fagsak = Fagsak.opprettNy(ytelseType, brukerIdent, pleietrengendeAktørId, null, saksnummer, fom, tom);
        fagsakTjeneste.opprettFagsak(fagsak);
        return fagsak;
    }

    private Saksnummer hentReservertEllerGenererSaksnummer(AktørId søkerAktørId, int behandlingsår) {
        var optReservert = reservertSaksnummerRepository.hent(OMSORGSPENGER, søkerAktørId.getAktørId(), null, Integer.toString(behandlingsår));
        return optReservert.map(ReservertSaksnummerEntitet::getSaksnummer).orElse(new Saksnummer(saksnummerRepository.genererNyttSaksnummer()));
    }
}
