package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.microsoftgraph.MicrosoftGraphRestKlient;
import no.nav.k9.felles.integrasjon.rest.SystemUserOidcRestClient;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.scenarioer.BrevScenarioerUtils;

import java.net.URI;
import java.util.Optional;

@ApplicationScoped
@Alternative
@Priority(value = 1)
public class MicrosoftGraphRestKlientFake extends MicrosoftGraphRestKlient {


    @Inject
    public MicrosoftGraphRestKlientFake(SystemUserOidcRestClient sysemuserRestClient,  @KonfigVerdi(value = "microsoft.graph.url", defaultVerdi = "https://graph.microsoft.com/v1.0") URI endpoint) {
        super(sysemuserRestClient, endpoint);
    }

    @Override
    public Optional<String> hentNavnForNavBruker(String identNavBruker) {
        return Optional.ofNullable(BrevScenarioerUtils.identNavnMap.get(identNavBruker));
    }

}
