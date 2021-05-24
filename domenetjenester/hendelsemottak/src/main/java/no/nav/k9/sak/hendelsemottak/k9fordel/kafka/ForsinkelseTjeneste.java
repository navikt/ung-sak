package no.nav.k9.sak.hendelsemottak.k9fordel.kafka;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.felles.konfigurasjon.env.Cluster;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.sak.hendelsemottak.k9fordel.domene.HendelseRepository;

/**
 * Tjenesten forsinker hendelser i minst 1 time pga oppførsel der det benyttes ANNULLERT+OPPRETTET til korrigeringer.
 * Det er ikke ønsket at ANNULLERT-hendelsen skal slippes før grunnlaget er klart med den korrigerte informasjonen.
 *
 * Videre ønsker vi å unngå hendelser på faste stengt dager, helger, og natten når Oppdrag er stengt.
 */
// TODO: Tore, trenger vi denne???
@ApplicationScoped
public class ForsinkelseTjeneste {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForsinkelseTjeneste.class);

    // Velger et tidspunkt litt etter at Oppdrag har åpnet for business kl 06:00.
    private static final LocalTime OPPDRAG_VÅKNER = LocalTime.of(6, 30);

    private static final Set<DayOfWeek> HELGEDAGER = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    private static final Set<MonthDay> FASTE_STENGT_DAGER = Set.of(
            MonthDay.of(1, 1),
            MonthDay.of(5, 1),
            MonthDay.of(5, 17),
            MonthDay.of(12, 25),
            MonthDay.of(12, 26),
            MonthDay.of(12, 31)
    );

    private ForsinkelseKonfig forsinkelseKonfig;
    private no.nav.k9.sak.hendelsemottak.k9fordel.domene.HendelseRepository hendelseRepository;

    public ForsinkelseTjeneste() {
        // CDI
    }

    @Inject
    public ForsinkelseTjeneste(ForsinkelseKonfig forsinkelseKonfig, HendelseRepository hendelseRepository) {
        this.forsinkelseKonfig = forsinkelseKonfig;
        this.hendelseRepository = hendelseRepository;
    }

    public LocalDateTime finnTidspunktForInnsendingAvHendelse() {
        if (Cluster.LOCAL.equals(Environment.current().getCluster())) {
            return LocalDateTime.now();
        }
        return LocalDateTime.now().plusHours(1);
    }


    private LocalDateTime finnNesteÅpningsdag(LocalDate utgangspunkt) {
        if (!erStengtDag(utgangspunkt)) {
            return getTidspunktMellom0630og0659(utgangspunkt);
        } else {
            return finnNesteÅpningsdag(utgangspunkt.plusDays(1));
        }
    }


    private boolean erStengtDag(LocalDate dato) {
        return HELGEDAGER.contains(dato.getDayOfWeek()) || erFastRødDag(dato);
    }

    private LocalDateTime getTidspunktMellom0630og0659(LocalDate utgangspunkt) {
        return LocalDateTime.of(utgangspunkt,
                OPPDRAG_VÅKNER.plusSeconds(LocalDateTime.now().getNano() % 1739));
    }

    private boolean erFastRødDag(LocalDate dato) {
        return FASTE_STENGT_DAGER.contains(MonthDay.from(dato));
    }
}
