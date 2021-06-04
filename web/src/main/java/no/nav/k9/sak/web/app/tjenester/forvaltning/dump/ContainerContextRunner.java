package no.nav.k9.sak.web.app.tjenester.forvaltning.dump;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.transaction.Transactional;

import no.nav.k9.felles.log.mdc.MdcExtendedLogContext;
import no.nav.k9.felles.sikkerhet.loginmodule.ContainerLogin;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.kalkulus.KalkulusDump;
import no.nav.k9.sikkerhet.oidc.token.impl.ContextTokenProvider;

/**
 * Kjører et kall på en egen tråd med ContainerLogin. Kan benyttes til å kalle med system kontekst videre internt.
 * NB: ikke bruk som convenience utenfor dump.
 */
@Dependent
@ActivateRequestContext
@Transactional
public class ContainerContextRunner implements AutoCloseable {

    private static final MdcExtendedLogContext LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess"); //$NON-NLS-1$

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName(KalkulusDump.class.getSimpleName() + "-thread");
        return t;
    });

    private ContextTokenProvider tokenProvider;

    @Inject
    public ContainerContextRunner(ContextTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    public static ContainerContextRunner createRunner() {
        return CDI.current().select(ContainerContextRunner.class).get();
    }

    @Override
    public void close() throws Exception {
        CDI.current().destroy(this);
    }

    private <T> T submit(Callable<T> call) throws Exception {
        var containerLogin = new ContainerLogin(tokenProvider);
        try {
            containerLogin.login();
            var result = call.call();
            return result;
        } finally {
            containerLogin.logout();
        }
    }

    public static <T> T doRun(Behandling behandling, Callable<T> call) {
        final var fagsakId = behandling.getFagsakId();
        final var saksnummer = behandling.getFagsak().getSaksnummer();
        final var behandlingId = behandling.getId();

        try {
            var future = EXECUTOR.submit((() -> {
                try (var runner = ContainerContextRunner.createRunner()) {
                    LOG_CONTEXT.add("fagsak", fagsakId);
                    LOG_CONTEXT.add("saksnummer", saksnummer);
                    LOG_CONTEXT.add("behandling", behandlingId);
                    return runner.submit(call);
                } finally {
                    LOG_CONTEXT.remove("behandling");
                    LOG_CONTEXT.remove("fagsak");
                    LOG_CONTEXT.remove("saksnummer");
                }
            }));

            return future.get(20, TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

}
