package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import java.time.LocalDate;

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

        final var angittPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(startDato, sluttDato);

        for (int årstall = LocalDate.now().getYear(); årstall >= CUT_OFF_OMP; årstall--) {
            var heleÅret = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(årstall, 1, 1), LocalDate.of(årstall, 12, 31));
            if (heleÅret.overlapper(angittPeriode)) {
                final Saksnummer saksnummer = reservertSaksnummer != null ? reservertSaksnummer : hentReservertEllerGenererSaksnummer(søkerAktørId, årstall);
                final Fagsak nyFagsak = opprettSakFor(saksnummer, søkerAktørId, pleietrengendeAktørId, ytelseType, heleÅret.getFomDato(), heleÅret.getTomDato());
                logger.info("Opprettet fagsak {} med periode {}/{}. Etterspurte fagsak for periode {}/{}", nyFagsak.getSaksnummer().getVerdi(), heleÅret.getFomDato(), heleÅret.getTomDato(), startDato, sluttDato);
                reservertSaksnummerRepository.slettHvisEksisterer(saksnummer);
                return nyFagsak;
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
        var optReservert = reservertSaksnummerRepository.hent(OMSORGSPENGER, søkerAktørId.getAktørId(), null, null, Integer.toString(behandlingsår));
        return optReservert.map(ReservertSaksnummerEntitet::getSaksnummer).orElseGet(() -> new Saksnummer(saksnummerRepository.genererNyttSaksnummer()));
    }
}
