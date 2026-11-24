package no.nav.ung.sak.domene.registerinnhenting;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandling.FagsakTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.ParallelleBehandlingerRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.InntektAbonnement;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.InntektAbonnementRepository;
import no.nav.ung.sak.domene.person.tps.TpsTjeneste;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Dependent
public class InntektAbonnentTjeneste {

    private static final String UNG_INNTEKT_FORMAAL = "Ung";
    private static final String UNG_INNTEKT_FILTER = "Ung";
    private static final Logger log = LoggerFactory.getLogger(InntektAbonnentTjeneste.class);

    private InntektAbonnementRepository inntektAbonnementRepository;
    private InntektAbonnentKlient inntektAbonnentKlient;
    private FagsakTjeneste fagsakTjeneste;
    private TpsTjeneste tpsTjeneste;
    private ParallelleBehandlingerRepository parallelleBehandlingerRepository;


    public InntektAbonnentTjeneste() {
        // for CDI proxy
    }

    @Inject
    public InntektAbonnentTjeneste(InntektAbonnementRepository inntektAbonnementRepository,
                                   InntektAbonnentKlient inntektAbonnentKlient,
                                   FagsakTjeneste fagsakTjeneste, TpsTjeneste tpsTjeneste,
                                   ParallelleBehandlingerRepository parallelleBehandlingerRepository) {
        this.inntektAbonnementRepository = inntektAbonnementRepository;
        this.inntektAbonnentKlient = inntektAbonnentKlient;
        this.fagsakTjeneste = fagsakTjeneste;
        this.tpsTjeneste = tpsTjeneste;
        this.parallelleBehandlingerRepository = parallelleBehandlingerRepository;
    }

    /**
     * Oppretter abonnement for kontrollperiodene i behandlingen, eller erstatter abonnementet til aktøren.
     * Har fagsaken en annen åpen kontrollbehandling, eier den abonnementet, og behandlingen endrer det ikke.
     */
    public void opprettAbonnementForBehandling(BehandlingReferanse behandling, Periode periode) {
        if (annenÅpenKontrollbehandlingEierAbonnementet(behandling)) {
            log.info("Fagsaken har en annen åpen kontrollbehandling som eier inntektsabonnementet. Oppretter ikke abonnement for behandlingen.");
            return;
        }
        opprettAbonnement(behandling.getAktørId(), periode);
    }

    /**
     * Avslutter abonnementet til aktøren når behandlingen avsluttes.
     * Har fagsaken en annen åpen kontrollbehandling, eier den abonnementet, og abonnementet beholdes.
     */
    public void avsluttAbonnementForAvsluttetBehandling(BehandlingReferanse behandling) {
        if (annenÅpenKontrollbehandlingEierAbonnementet(behandling)) {
            log.info("Fagsaken har en annen åpen kontrollbehandling som eier inntektsabonnementet. Beholder abonnementet.");
            return;
        }
        avsluttAbonnentHvisFinnes(behandling.getAktørId());
    }

    private boolean annenÅpenKontrollbehandlingEierAbonnementet(BehandlingReferanse behandling) {
        return parallelleBehandlingerRepository.finnIkkeAvsluttetKontrollbehandling(behandling.getFagsakId())
            .filter(kontrollbehandling -> !kontrollbehandling.behandlingId().equals(behandling.getBehandlingId()))
            .isPresent();
    }

    void opprettAbonnement(AktørId aktørId, Periode periode) {
        var eksisterendeAbonnement = inntektAbonnementRepository.hentAbonnementForAktør(aktørId).orElse(null);
        if (eksisterendeAbonnement != null) {
            if (eksisterendeAbonnement.getPeriode().tilPeriode().equals(periode)) {
                log.info("Prøver å opprette abonnement for aktør, men abonnementID = {} eksisterer allerede for denne periode", eksisterendeAbonnement.getAbonnementId());
                return;
            } else {
                log.info("Prøver å opprette abonnement for aktør for periode {}, men det finnes allerede et abonnement for periode {} med id {}. Avslutter og oppretter ny.",
                    periode, eksisterendeAbonnement.getPeriode(), eksisterendeAbonnement.getAbonnementId());
                avsluttAbonnement(eksisterendeAbonnement);
            }
        }

        var tomFagsakPeriode = fagsakTjeneste.finnFagsakerForAktør(aktørId).stream()
            .filter(Fagsak::erÅpen)
            .map(fagsak -> fagsak.getPeriode().getTomDato())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Fant ingen åpen fagsak med gyldig periode"));

        var personIdent = tpsTjeneste.hentFnr(aktørId).orElseThrow();

        //pga validering i inntektskomponenten må bevaringstidImåneder være minst differansen mellom fom og nå. Legger på en ekstra måned i tilfelle forsinkelse
        int bevarlingtidIMåneder = (int) ChronoUnit.MONTHS.between(YearMonth.from(periode.getFom()), YearMonth.now()) + 2;
        long abonnementId = inntektAbonnentKlient.opprettAbonnement(
            personIdent,
            UNG_INNTEKT_FORMAAL,
            List.of(UNG_INNTEKT_FILTER),
            YearMonth.from(periode.getFom()),
            YearMonth.from(periode.getTom()),
            YearMonth.from(tomFagsakPeriode).atEndOfMonth().plusMonths(1), // Lytter på hendelser en måned etter fagsakens tom dato for å fange opp sene inntektsrapporeringer.
            bevarlingtidIMåneder
        );
        inntektAbonnementRepository.lagre(new InntektAbonnement(String.valueOf(abonnementId), aktørId, periode, tomFagsakPeriode));
    }

    public Optional<Long> hentFørsteSekvensnummer() {
        Optional<Long> resultat = inntektAbonnentKlient.hentStartSekvensnummer(LocalDate.now());
        log.info("Forsøkte å hente første sekvensnummer for hendelser fra inntektskomponenten, fikk {}", resultat.orElse(null));
        return resultat;
    }

    public List<InntektHendelse> hentNyeInntektHendelser(long startSekvensnummer) {
        log.info("Henter inntektshendelser fra sekvensnummer={}", startSekvensnummer);

        return inntektAbonnentKlient.hentAbonnentHendelser(startSekvensnummer, List.of(UNG_INNTEKT_FILTER)).stream()
            .map(InntektHendelseMapper::tilDomene)
            .toList();
    }

    void avsluttAbonnentHvisFinnes(AktørId aktørId) {
        inntektAbonnementRepository.hentAbonnementForAktør(aktørId)
            .ifPresent(this::avsluttAbonnement);
    }

    private void avsluttAbonnement(InntektAbonnement abonnement) {
        inntektAbonnentKlient.avsluttAbonnement(Long.parseLong(abonnement.getAbonnementId()));
        inntektAbonnementRepository.slettAbonnement(abonnement);
        log.info("Avsluttet abonnement med id={}", abonnement.getAbonnementId());
    }

    public record InntektHendelse(Long sekvensnummer, AktørId aktørId, Periode periode) {
    }

    private static class InntektHendelseMapper {
        static InntektHendelse tilDomene(InntektAbonnentKlient.AbonnementHendelse hendelse) {
            return new InntektHendelse(
                hendelse.sekvensnummer(),
                new AktørId(hendelse.norskident()),
                new Periode(hendelse.maaned())
            );
        }
    }
}
