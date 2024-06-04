package no.nav.k9.sak.web.app.tjenester.brukerdialog;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.pdl.Pdl;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.web.app.tjenester.brukerdialog.policy.PolicyEvaluation;
import no.nav.k9.sikkerhet.oidc.token.context.ContextAwareTokenProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

import static no.nav.k9.sak.web.app.tjenester.brukerdialog.policy.PolicyUtils.evaluate;

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
        var brukerAktørId = pdlKlient.hentAktørIdForPersonIdent(brukerident).orElseThrow(() -> new IllegalStateException("Fant ikke aktørId for bruker"));

        logger.info("Henter siste behandling med innvilget vedtak.");
        Optional<Behandling> sistBehandlingMedInnvilgetVedtak =
            fagsakRepository.finnFagsakRelatertTil(
                    FagsakYtelseType.OMSORGSPENGER_KS,
                    new AktørId(brukerAktørId),
                    pleietrengendeAktørId,
                    null,
                    null,
                    null
                )
                .stream()
                .map(fagsak -> behandlingRepository.finnSisteInnvilgetBehandling(fagsak.getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(Comparator.comparing(Behandling::getAvsluttetDato));

        if (sistBehandlingMedInnvilgetVedtak.isPresent()) {
            Behandling behandling = sistBehandlingMedInnvilgetVedtak.get();
            logger.info("Fant siste behandling med innvilget vedtak.");

            logger.info("Sjekker om bruker og pleietrengende er parter i saken.");

            BehandlingContext behandlingContext = new BehandlingContext(behandling);

            return evaluate(
                behandlingContext,
                BrukerdialogPolicies.erPartISaken(brukerAktørId, "BrukerAktørId")
                    .and(BrukerdialogPolicies.erPartISaken(pleietrengendeAktørId.getAktørId(), "PleietrengendeAktørId")),
                evaluerOgReturner(behandlingContext)
            );
        } else {
            logger.info("Fant ingen behandlinger med innvilget vedtak.");
            return new HarGyldigOmsorgsdagerVedtakDto(
                false,
                null,
                null,
                PolicyEvaluation.notApplicable("Fant ingen behandlinger med innvilget vedtak")
            );
        }
    }


    @NotNull
    private static Function<PolicyEvaluation, HarGyldigOmsorgsdagerVedtakDto> evaluerOgReturner(BehandlingContext context) {
        return (PolicyEvaluation evaluation) -> switch (evaluation.getDecision()) {
            case PERMIT -> {
                logger.info("Partene er parter i saken. Returnerer gyldig vedtak.");
                yield new HarGyldigOmsorgsdagerVedtakDto(
                    true,
                    context.behandling().getFagsak().getSaksnummer(),
                    context.behandling().getAvsluttetDato().toLocalDate(),
                    evaluation
                );
            }

            case DENY, NOT_APPLICABLE -> {
                logger.info("Partene er ikke parter i saken. Returnerer ingen vedtak.");
                yield new HarGyldigOmsorgsdagerVedtakDto(
                    false,
                    null,
                    null,
                    evaluation
                );
            }
        };
    }
}
