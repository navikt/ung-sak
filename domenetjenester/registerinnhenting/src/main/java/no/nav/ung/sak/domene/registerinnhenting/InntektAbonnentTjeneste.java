package no.nav.ung.sak.domene.registerinnhenting;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandling.FagsakTjeneste;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.InntektAbonnement;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.InntektAbonnementRepository;
import no.nav.ung.sak.domene.person.tps.TpsTjeneste;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.PersonIdent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Dependent
public class InntektAbonnentTjeneste {

    private static final String UNG_INNTEKT_FORMAAL = "Ung";
    private static final String UNG_INNTEKT_FILTER = "Ung";
    private static final int BEVARINGTID_I_INNTEKTSKOMPONENTEN_MAANEDER = 1;
    private static final Logger log = LoggerFactory.getLogger(InntektAbonnentTjeneste.class);

    private InntektAbonnementRepository inntektAbonnementRepository;
    private InntektAbonnentKlient inntektAbonnentKlient;
    private FagsakTjeneste fagsakTjeneste;
    private TpsTjeneste tpsTjeneste;


    public InntektAbonnentTjeneste() {
        // for CDI proxy
    }

    @Inject
    public InntektAbonnentTjeneste(InntektAbonnementRepository inntektAbonnementRepository,
                                   InntektAbonnentKlient inntektAbonnentKlient,
                                   FagsakTjeneste fagsakTjeneste, TpsTjeneste tpsTjeneste) {
        this.inntektAbonnementRepository = inntektAbonnementRepository;
        this.inntektAbonnentKlient = inntektAbonnentKlient;
        this.fagsakTjeneste = fagsakTjeneste;
        this.tpsTjeneste = tpsTjeneste;
    }

    public void opprettAbonnement(AktørId aktørId, Periode periode) {
        var tomFagsakPeriode = fagsakTjeneste.finnFagsakerForAktør(aktørId).stream()
            .filter(Fagsak::erÅpen)
            .map(fagsak -> fagsak.getPeriode().getTomDato())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Fant ingen åpen fagsak med gyldig periode"));

        var personIdent = tpsTjeneste.hentFnr(aktørId).orElseThrow();
        long abonnementId = inntektAbonnentKlient.opprettAbonnement(
            personIdent,
            UNG_INNTEKT_FORMAAL,
            List.of(UNG_INNTEKT_FILTER),
            YearMonth.from(periode.getFom()),
            YearMonth.from(periode.getTom()),
            tomFagsakPeriode,
            BEVARINGTID_I_INNTEKTSKOMPONENTEN_MAANEDER
        );
        inntektAbonnementRepository.lagre(new InntektAbonnement(String.valueOf(abonnementId), aktørId));
    }

    public Optional<Long> hentFørsteSekvensnummer() {
        return  inntektAbonnentKlient.hentStartSekvensnummer(LocalDate.now());
    }

    public List<InntektHendelse> hentNyeInntektHendelser(long startSekvensnummer) {
        log.info("Henter inntektshendelser fra sekvensnummer={}", startSekvensnummer);

        return inntektAbonnentKlient.hentAbonnentHendelser(startSekvensnummer, List.of(UNG_INNTEKT_FILTER)).stream()
            .map(InntektHendelseMapper::tilDomene)
            .toList();
    }

    public void avsluttAbonnentHvisFinnes(AktørId aktørId) {
        inntektAbonnementRepository.hentAbonnementForAktør(aktørId)
            .ifPresent(abonnement -> {
                inntektAbonnentKlient.avsluttAbonnement(Long.parseLong(abonnement.getAbonnementId()));
                inntektAbonnementRepository.slettAbonnement(abonnement);
                log.info("Avsluttet abonnement for aktørId={}", aktørId.getId());
            });
    }

    public record InntektHendelse(Long sekvensnummer, AktørId aktørId, Periode periode) {
    }

    private List<InntektAbonnentKlient.AbonnementHendelse> hentAbonnentHendelser(long sekvensnummer) {
        return inntektAbonnentKlient.hentAbonnentHendelser(sekvensnummer, List.of(UNG_INNTEKT_FILTER));
    }


    private static class InntektHendelseMapper {
        static List<InntektHendelse> tilDomeneListe(List<InntektAbonnentKlient.AbonnementHendelse> hendelser) {
            return hendelser.stream()
                .map(InntektHendelseMapper::tilDomene)
                .toList();
        }

        static InntektHendelse tilDomene(InntektAbonnentKlient.AbonnementHendelse hendelse) {
            return new InntektHendelse(
                hendelse.sekvensnummer(),
                new AktørId(hendelse.norskident()),
                new Periode(hendelse.maaned())
            );
        }
    }

}
