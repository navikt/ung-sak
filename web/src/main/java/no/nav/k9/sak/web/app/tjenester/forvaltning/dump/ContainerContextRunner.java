package no.nav.k9.sak.web.app.tjenester.forvaltning.dump;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.jboss.weld.context.RequestContext;
import org.jboss.weld.context.unbound.UnboundLiteral;

import no.nav.k9.felles.log.mdc.MdcExtendedLogContext;
import no.nav.k9.felles.sikkerhet.loginmodule.ContainerLogin;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sikkerhet.oidc.token.impl.ContextTokenProvider;

/**
 * Kjører et kall på en egen tråd med ContainerLogin. Kan benyttes til å kalle med system kontekst videre internt.
 * NB: ikke bruk som convenience utenfor dump.
 */
@Dependent
public class ContainerContextRunner {

    private static final MdcExtendedLogContext LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess"); //$NON-NLS-1$

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName(ContainerContextRunner.class.getSimpleName() + "-thread");
        return t;
    });

    private final ContextTokenProvider tokenProvider;

    @Inject
    public ContainerContextRunner(ContextTokenProvider tokenProvider) {
        this.tokenProvider = Objects.requireNonNull(tokenProvider);
    }

    public static ContainerContextRunner createRunner() {
        return CDI.current().select(ContainerContextRunner.class).get();
    }

    @Transactional
    private <T> T submit(Callable<T> call) throws Exception {
        var containerLogin = new ContainerLogin(tokenProvider);
        containerLogin.login();
        var result = call.call();
        return result;
    }

    public static <T> T doRun(Behandling behandling, Callable<T> call) {
        final var fagsakId = behandling.getFagsakId();
        final var saksnummer = behandling.getFagsak().getSaksnummer();
        final var behandlingId = behandling.getId();

        try {
            var future = EXECUTOR.submit((() -> {
                T result;
                var requestContext = CDI.current().select(RequestContext.class, UnboundLiteral.INSTANCE).get();
                requestContext.activate();
                var runner = ContainerContextRunner.createRunner();
                try {
                    LOG_CONTEXT.add("fagsak", fagsakId);
                    LOG_CONTEXT.add("saksnummer", saksnummer);
                    LOG_CONTEXT.add("behandling", behandlingId);
                    result = runner.submit(call);
                } finally {
                    LOG_CONTEXT.remove("behandling");
                    LOG_CONTEXT.remove("fagsak");
                    LOG_CONTEXT.remove("saksnummer");
                    CDI.current().destroy(runner);
                    requestContext.deactivate();
                }
                return result;
            }));

            return future.get(20, TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

}
