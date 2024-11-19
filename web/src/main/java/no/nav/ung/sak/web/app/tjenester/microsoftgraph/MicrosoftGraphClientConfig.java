package no.nav.ung.sak.web.app.tjenester.microsoftgraph;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;

@Dependent
public class MicrosoftGraphClientConfig {
    private final GraphServiceClient graphClient;

    final String[] scopes = new String[] { "https://graph.microsoft.com/.default", "User.Read.All" };

    @Inject
    public MicrosoftGraphClientConfig(
        @KonfigVerdi(value = "AZURE_APP_CLIENT_ID") String clientId,
        @KonfigVerdi(value = "AZURE_APP_CLIENT_SECRET") String clientSecret,
        @KonfigVerdi(value = "AZURE_APP_TENANT_ID") String tenantId
    ) {

        final ClientSecretCredential credential = new ClientSecretCredentialBuilder()
            .tenantId(tenantId)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .build();

        this.graphClient = new GraphServiceClient(credential, scopes);
    }

    public GraphServiceClient getGraphClient() {
        return graphClient;
    }
}
