package no.nav.ung.sak.domene.registerinnhenting;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandling.FagsakTjeneste;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.InntektAbonnement;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.InntektAbonnementRepository;
import no.nav.ung.sak.typer.AktørId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

@ApplicationScoped
public class InntektAbonnentTjeneste {

    private InntektAbonnementRepository inntektAbonnementRepository;
    private InntektAbonnentKlient inntektAbonnentKlient;
    private FagsakTjeneste fagsakTjeneste;
    private static final String UNG_INNTEKT_FORMAAL = "Ung";
    private static final String UNG_INNTEKT_FILTER = "Ung";
    private static final Logger log = LoggerFactory.getLogger(InntektAbonnentTjeneste.class);


    public InntektAbonnentTjeneste() {
        // for CDI proxy
    }

    @Inject
    public InntektAbonnentTjeneste(InntektAbonnementRepository inntektAbonnementRepository,
                                   InntektAbonnentKlient inntektAbonnentKlient,
                                   FagsakTjeneste fagsakTjeneste) {
        this.inntektAbonnementRepository = inntektAbonnementRepository;
        this.inntektAbonnentKlient = inntektAbonnentKlient;
        this.fagsakTjeneste = fagsakTjeneste;
    }

    public InntektAbonnement opprettAbonnement(AktørId aktørId) {
        var tomFagsakPeriode = fagsakTjeneste.finnFagsakerForAktør(aktørId).stream()
            .filter(Fagsak::erÅpen)
            .map(fagsak -> fagsak.getPeriode().getTomDato())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Fant ingen åpen fagsak med gyldig periode"));

        var inntektAbonnement = inntektAbonnentKlient.opprettAbonnement(
            aktørId,
            UNG_INNTEKT_FORMAAL,
            List.of(UNG_INNTEKT_FILTER),
            LocalDate.now().minusMonths(1).toString(),
            LocalDate.now().plusMonths(1).toString(),
            tomFagsakPeriode
        );
        inntektAbonnementRepository.lagre(inntektAbonnement);

        return inntektAbonnement;
    }

    public long hentFørsteSekvensnummer() {
        return  inntektAbonnentKlient.hentStartSekvensnummer(LocalDate.now());
    }

    public Stream<InntektHendelse> hentNyeInntektHendelser(long startSekvensnummer) {
        log.info("Henter inntektshendelser fra sekvensnummer={}", startSekvensnummer);

        return Stream.iterate(
                hentAbonnentHendelser(startSekvensnummer),
                abonnentHendelser -> !abonnentHendelser.isEmpty(),
                abonnentHendelser -> hentAbonnentHendelser(abonnentHendelser.getLast().sekvensnummer() + 1)
            )
            .flatMap(InntektHendelseMapper::tilDomeneListe);
    }

    public void avsluttAbonnent(long abonnementId, AktørId aktørId) {
        inntektAbonnentKlient.avsluttAbonnement(abonnementId);
        inntektAbonnementRepository.slettAbonnement(
            inntektAbonnementRepository.hentAbonnementForAktør(aktørId)
                .orElseThrow(() -> new IllegalStateException("Fant ikke abonnement for aktørId: " + aktørId.getId()))
        );
    }

    public record InntektHendelse(Long sekvensnummer, AktørId aktørId, LocalDate hendelsesdato) {
    }

    private List<InntektAbonnentKlient.AbonnementHendelse> hentAbonnentHendelser(long sekvensnummer) {
        return inntektAbonnentKlient.hentAbonnentHendelser(sekvensnummer, List.of(UNG_INNTEKT_FILTER));
    }

    private static class InntektHendelseMapper {
        static Stream<InntektHendelse> tilDomeneListe(List<InntektAbonnentKlient.AbonnementHendelse> hendelser) {
            return hendelser.stream()
                .map(InntektHendelseMapper::tilDomene);
        }

        static InntektHendelse tilDomene(InntektAbonnentKlient.AbonnementHendelse hendelse) {
            return new InntektHendelse(
                hendelse.sekvensnummer(),
                new AktørId(hendelse.norskident()),
                LocalDate.parse(hendelse.maaned() + "-01")
            );
        }
    }

}
