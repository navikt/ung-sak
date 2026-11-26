package no.nav.ung.sak.web.server.abac;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.sikkerhet.oidc.azure.PreAuthorizedApp;
import no.nav.k9.sikkerhet.oidc.config.DefaultJsonMapper;
import no.nav.k9.sikkerhet.oidc.token.context.ContextAwareTokenProvider;
import no.nav.k9.sikkerhet.oidc.token.internal.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Finner navnet på systemet som kaller med systemtoken, til metrikker og logg.
 * <p>
 * Navnet slås opp fra klient-ID-en (azp) i {@code AZURE_APP_PRE_AUTHORIZED_APPS}, på samme måte som PepImpl gjør ved
 * tilgangskontrollen. {@code azp_name} i tokenet brukes ikke, siden det bare er ment for visning.
 */
@Dependent
public class KallendeSystemUtleder {

    private static final Logger log = LoggerFactory.getLogger(KallendeSystemUtleder.class);

    private final Supplier<String> azpSupplier;
    private final List<PreAuthorizedApp> preAuthorizedApps;

    @Inject
    public KallendeSystemUtleder(ContextAwareTokenProvider tokenProvider,
                                 @KonfigVerdi(value = "AZURE_APP_PRE_AUTHORIZED_APPS", required = false) String preAuthorizedApps) {
        this(() -> JwtUtil.getAzp(tokenProvider.getToken().getToken()), preAuthorizedApps);
    }

    KallendeSystemUtleder(Supplier<String> azpSupplier, String preAuthorizedApps) {
        this.azpSupplier = azpSupplier;
        this.preAuthorizedApps = parse(preAuthorizedApps);
    }

    /**
     * Navnet ({@code <cluster>:<namespace>:<app>}) på systemet som kaller. Faller tilbake på klient-ID-en dersom den ikke
     * finnes blant de pre-autoriserte appene, selv om PepImpl da normalt allerede har avvist kallet.
     */
    public String kallendeSystem() {
        var azp = azpSupplier.get();
        var navn = preAuthorizedApps.stream()
            .filter(app -> Objects.equals(azp, app.clientId()) && app.name() != null)
            .map(PreAuthorizedApp::name)
            .sorted()
            .distinct()
            .collect(Collectors.joining(","));
        return navn.isEmpty() ? azp : navn;
    }

    private static List<PreAuthorizedApp> parse(String preAuthorizedApps) {
        if (preAuthorizedApps == null || preAuthorizedApps.isBlank()) {
            return List.of();
        }
        try {
            return List.copyOf(DefaultJsonMapper.getObjectMapper().readValue(preAuthorizedApps, new TypeReference<List<PreAuthorizedApp>>() {
            }));
        } catch (JsonProcessingException e) {
            log.warn("Klarte ikke å lese AZURE_APP_PRE_AUTHORIZED_APPS. Bruker klient-ID som systemnavn.");
            return List.of();
        }
    }
}
