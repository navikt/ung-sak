package no.nav.k9.sak.web.app.tjenester.brukerdialog;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.Resultat;
import no.nav.fpsak.nare.evaluation.RuleReasonRef;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSummary;
import no.nav.k9.felles.integrasjon.pdl.Pdl;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.web.app.tjenester.brukerdialog.policy.erpartisaken.ErPartISakenVilkår;
import no.nav.k9.sikkerhet.oidc.token.context.ContextAwareTokenProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

@Dependent
public class BrukerdialogTjeneste implements BrukerdialogFasade {
    private static final Logger logger = LoggerFactory.getLogger(BrukerdialogTjeneste.class);

    private final FagsakRepository fagsakRepository;

    private final BehandlingRepository behandlingRepository;

    private final ContextAwareTokenProvider tokenProvider;

    private final Pdl pdlKlient;

    @Inject
    public BrukerdialogTjeneste(
        FagsakRepository fagsakRepository,
        BehandlingRepository behandlingRepository,
        ContextAwareTokenProvider tokenProvider,
        Pdl pdlKlient

    ) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.tokenProvider = tokenProvider;
        this.pdlKlient = pdlKlient;

    }

    @Override
    public HarGyldigOmsorgsdagerVedtakDto harGyldigOmsorgsdagerVedtak(AktørId pleietrengendeAktørId) {
        //FIXME spør via k9-aarskvantum når tjenesten der er klar, for å få med Infotrygd-vedtak
        //FIXME håndter hvis bruker har fått avslag etter innvilgelse
        //FIXME ta med alle typer omsorgsdagervedtak eller rename metode
        var brukerident = tokenProvider.getUserId();
        logger.info("Henter aktørId for brukerident.");
        var brukerAktørId = new AktørId(pdlKlient.hentAktørIdForPersonIdent(brukerident).orElseThrow(() -> new IllegalStateException("Fant ikke aktørId for bruker")));

        logger.info("Henter siste behandling med innvilget vedtak.");
        Behandling sistBehandlingMedInnvilgetVedtak =
            fagsakRepository.finnFagsakRelatertTil(
                    FagsakYtelseType.OMSORGSPENGER_KS,
                    brukerAktørId,
                    pleietrengendeAktørId,
                    null,
                    null,
                    null
                )
                .stream()
                .map(fagsak -> behandlingRepository.finnSisteInnvilgetBehandling(fagsak.getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(Comparator.comparing(Behandling::getAvsluttetDato))
                .orElse(null);


            logger.info("Fant siste behandling med innvilget vedtak.");
            return evaluerOgReturner(brukerAktørId, pleietrengendeAktørId, sistBehandlingMedInnvilgetVedtak);
    }

    @NotNull
    private static HarGyldigOmsorgsdagerVedtakDto evaluerOgReturner(AktørId brukerAktørId, AktørId pleietrengendeAktørId, Behandling behandling) {
        ErPartISakenGrunnlag erPartISakenGrunnlag = new ErPartISakenGrunnlag(behandling);

        logger.info("Sjekker om bruker og pleietrengende er parter i saken.");
        Evaluation evaluer = new ErPartISakenVilkår(brukerAktørId, pleietrengendeAktørId).evaluer(erPartISakenGrunnlag);

        if (evaluer.result() == Resultat.JA) {
            logger.info("Partene er parter i saken. Returnerer gyldig vedtak.");
            return new HarGyldigOmsorgsdagerVedtakDto(
                true,
                behandling.getFagsak().getSaksnummer(),
                behandling.getAvsluttetDato().toLocalDate(),
                evaluer
            );
        } else {
            Stream<String> reasons = new EvaluationSummary(evaluer)
                .allOutcomes()
                .stream()
                .map(RuleReasonRef::getReasonTextTemplate);
            logger.info("Partene er ikke parter i saken. Returnerer ugyldig vedtak. Grunn: {}", reasons);

            return new HarGyldigOmsorgsdagerVedtakDto(
                false,
                null,
                null,
                evaluer
            );
        }
    }
}
