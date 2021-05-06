package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.kalkulus;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectWriter;

import no.nav.folketrygdloven.kalkulus.mappers.JsonMapper;
import no.nav.k9.felles.log.mdc.MdcExtendedLogContext;
import no.nav.k9.felles.sikkerhet.loginmodule.ContainerLogin;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;
import no.nav.k9.sikkerhet.oidc.token.impl.ContextTokenProvider;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
@FagsakYtelseTypeRef("PSB")
@FagsakYtelseTypeRef("FRISINN")
public class KalkulusDump implements DebugDumpBehandling {

    private static final MdcExtendedLogContext LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess"); //$NON-NLS-1$

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName(KalkulusDump.class.getSimpleName() + "-thread");
        return t;
    });

    private final ObjectWriter objectWriter = JsonMapper.getMapper().writerWithDefaultPrettyPrinter();
    private KalkulusTjenesteAdapter tjeneste;
    private ContextTokenProvider tokenProvider;

    KalkulusDump() {
        // for proxy
    }

    @Inject
    public KalkulusDump(KalkulusTjenesteAdapter tjeneste,
                        ContextTokenProvider tokenProvider) {
        this.tjeneste = tjeneste;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public List<DumpOutput> dump(Behandling behandling) {
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling);
        try {

            var data = submit(behandling, () -> tjeneste.hentBeregningsgrunnlagForGui(ref))
                .get(20, TimeUnit.SECONDS);

            if (data.isEmpty()) {
                return List.of();
            }
            var content = objectWriter.writeValueAsString(data.get());
            return List.of(new DumpOutput("kalkulus-beregningsgrunnlag-for-gui.json", content));
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return List.of(new DumpOutput("kalkulus-beregningsgrunnlag-for-gui-ERROR.txt", sw.toString()));
        }
    }

    private <T> Future<T> submit(Behandling behandling, Callable<T> call) {
        final var fagsakId = behandling.getFagsakId();
        final var saksnummer = behandling.getFagsak().getSaksnummer();
        final var behandlingId = behandling.getId();

        var future = EXECUTOR.submit(() -> {
            LOG_CONTEXT.add("fagsak", fagsakId);
            LOG_CONTEXT.add("saksnummer", saksnummer);
            LOG_CONTEXT.add("behandling", behandlingId);

            var containerLogin = new ContainerLogin(tokenProvider);
            try {
                containerLogin.login();
                var result = call.call();
                return result;
            } finally {
                containerLogin.logout();
                LOG_CONTEXT.remove("behandling");
                LOG_CONTEXT.remove("fagsak");
                LOG_CONTEXT.remove("saksnummer");
            }

        });
        return future;
    }
}
