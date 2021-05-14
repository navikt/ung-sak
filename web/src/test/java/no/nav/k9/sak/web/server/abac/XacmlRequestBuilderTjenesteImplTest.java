package no.nav.k9.sak.web.server.abac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import no.nav.k9.abac.BeskyttetRessursKoder;
import no.nav.k9.felles.sikkerhet.abac.AbacIdToken;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.NavAbacCommonAttributter;
import no.nav.k9.felles.sikkerhet.abac.PdpKlient;
import no.nav.k9.felles.sikkerhet.abac.PdpRequest;
import no.nav.k9.felles.sikkerhet.pdp.PdpConsumer;
import no.nav.k9.felles.sikkerhet.pdp.PdpKlientImpl;
import no.nav.k9.felles.sikkerhet.pdp.Xacml10JsonProfile;
import no.nav.k9.felles.sikkerhet.pdp.Xacml10JsonProfile.XacmlResponse;
import no.nav.k9.felles.sikkerhet.pdp.xacml.Category;
import no.nav.k9.felles.sikkerhet.pdp.xacml.XacmlRequestBuilder;
import no.nav.k9.felles.sikkerhet.pdp.xacml.XacmlResponseWrapper;

public class XacmlRequestBuilderTjenesteImplTest {

    public static final String JWT_TOKEN = "eyAidHlwIjogIkpXVCIsICJraWQiOiAiU0gxSWVSU2sxT1VGSDNzd1orRXVVcTE5VHZRPSIsICJhbGciOiAiUlMyNTYiIH0.eyAiYXRfaGFzaCI6ICIyb2c1RGk5ZW9LeFhOa3VPd0dvVUdBIiwgInN1YiI6ICJzMTQyNDQzIiwgImF1ZGl0VHJhY2tpbmdJZCI6ICI1NTM0ZmQ4ZS03MmE2LTRhMWQtOWU5YS1iZmEzYThhMTljMDUtNjE2NjA2NyIsICJpc3MiOiAiaHR0cHM6Ly9pc3NvLXQuYWRlby5ubzo0NDMvaXNzby9vYXV0aDIiLCAidG9rZW5OYW1lIjogImlkX3Rva2VuIiwgImF1ZCI6ICJPSURDIiwgImNfaGFzaCI6ICJiVWYzcU5CN3dTdi0wVlN0bjhXLURnIiwgIm9yZy5mb3JnZXJvY2sub3BlbmlkY29ubmVjdC5vcHMiOiAiMTdhOGZiMzYtMGI0Ny00YzRkLWE4YWYtZWM4Nzc3Y2MyZmIyIiwgImF6cCI6ICJPSURDIiwgImF1dGhfdGltZSI6IDE0OTgwMzk5MTQsICJyZWFsbSI6ICIvIiwgImV4cCI6IDE0OTgwNDM1MTUsICJ0b2tlblR5cGUiOiAiSldUVG9rZW4iLCAiaWF0IjogMTQ5ODAzOTkxNSB9.S2DKQweQWZIfjaAT2UP9_dxrK5zqpXj8IgtjDLt5PVfLYfZqpWGaX-ckXG0GlztDVBlRK4ylmIYacTmEAUV_bRa_qWKRNxF83SlQRgHDSiE82SGv5WHOGEcAxf2w_d50XsgA2KDBCyv0bFIp9bCiKzP11uWPW0v4uIkyw2xVxMVPMCuiMUtYFh80sMDf9T4FuQcFd0LxoYcSFDEDlwCdRiF3ufw73qtMYBlNIMbTGHx-DZWkZV7CgukmCee79gwQIvGwdLrgaDrHFCJUDCbB1FFEaE3p3_BZbj0T54fCvL69aHyWm1zEd9Pys15yZdSh3oSSr4yVNIxhoF-nQ7gY-g;";
    private PdpKlientImpl pdpKlient;
    private PdpConsumer pdpConsumerMock;
    private AppXacmlRequestBuilderTjenesteImpl xamlRequestBuilderTjeneste;

    @BeforeEach
    public void setUp() {
        pdpConsumerMock = mock(PdpConsumer.class);
        xamlRequestBuilderTjeneste = new AppXacmlRequestBuilderTjenesteImpl();
        pdpKlient = new PdpKlientImpl(pdpConsumerMock, xamlRequestBuilderTjeneste);
    }

    @Test
    public void kallPdpMedSamlTokenNårIdTokenErSamlToken() throws Exception {
        AbacIdToken idToken = AbacIdToken.withSamlToken("SAML");
        XacmlResponseWrapper responseWrapper = createResponse("xacmlresponse.json");
        ArgumentCaptor<XacmlRequestBuilder> captor = ArgumentCaptor.forClass(XacmlRequestBuilder.class);

        when(pdpConsumerMock.evaluate(captor.capture())).thenReturn(responseWrapper);
        PdpRequest pdpRequest = lagPdpRequest();
        pdpRequest.put(NavAbacCommonAttributter.RESOURCE_FELLES_PERSON_FNR, Collections.singleton("12345678900"));
        pdpRequest.put(PdpKlient.ENVIRONMENT_AUTH_TOKEN, idToken);
        pdpKlient.forespørTilgang(pdpRequest);

        assertThat(captor.getValue().build().toString().contains(NavAbacCommonAttributter.ENVIRONMENT_FELLES_SAML_TOKEN)).isTrue();
    }

    @Test
    public void kallPdpUtenFnrResourceHvisPersonlisteErTom() throws Exception {
        AbacIdToken idToken = AbacIdToken.withOidcToken(JWT_TOKEN);
        XacmlResponseWrapper responseWrapper = createResponse("xacmlresponse.json");
        ArgumentCaptor<XacmlRequestBuilder> captor = ArgumentCaptor.forClass(XacmlRequestBuilder.class);

        when(pdpConsumerMock.evaluate(captor.capture())).thenReturn(responseWrapper);

        PdpRequest pdpRequest = lagPdpRequest();
        pdpRequest.put(NavAbacCommonAttributter.RESOURCE_FELLES_PERSON_FNR, Collections.emptySet());
        pdpRequest.put(PdpKlient.ENVIRONMENT_AUTH_TOKEN, idToken);
        pdpKlient.forespørTilgang(pdpRequest);

        assertThat(captor.getValue().build().toString().contains(NavAbacCommonAttributter.RESOURCE_FELLES_PERSON_FNR)).isFalse();
    }

    @Test
    public void kallPdpMedJwtTokenBodyNårIdTokenErJwtToken() throws Exception {
        AbacIdToken idToken = AbacIdToken.withOidcToken(JWT_TOKEN);
        XacmlResponseWrapper responseWrapper = createResponse("xacmlresponse.json");
        ArgumentCaptor<XacmlRequestBuilder> captor = ArgumentCaptor.forClass(XacmlRequestBuilder.class);

        when(pdpConsumerMock.evaluate(captor.capture())).thenReturn(responseWrapper);

        PdpRequest pdpRequest = lagPdpRequest();
        pdpRequest.put(NavAbacCommonAttributter.RESOURCE_FELLES_PERSON_FNR, Collections.singleton("12345678900"));
        pdpRequest.put(PdpKlient.ENVIRONMENT_AUTH_TOKEN, idToken);
        pdpKlient.forespørTilgang(pdpRequest);

        assertThat(captor.getValue().build().toString().contains(NavAbacCommonAttributter.ENVIRONMENT_FELLES_OIDC_TOKEN_BODY)).isTrue();
    }

    @Test
    public void kallPdpMedFlereAttributtSettNårPersonlisteStørreEnn1() throws Exception {
        AbacIdToken idToken = AbacIdToken.withOidcToken(JWT_TOKEN);
        XacmlResponseWrapper responseWrapper = createResponse("xacml3response.json");
        ArgumentCaptor<XacmlRequestBuilder> captor = ArgumentCaptor.forClass(XacmlRequestBuilder.class);

        when(pdpConsumerMock.evaluate(captor.capture())).thenReturn(responseWrapper);
        Set<String> personnr = new HashSet<>();
        personnr.add("12345678900");
        personnr.add("00987654321");
        personnr.add("15151515151");

        PdpRequest pdpRequest = lagPdpRequest();
        pdpRequest.put(NavAbacCommonAttributter.RESOURCE_FELLES_PERSON_FNR, personnr);
        pdpRequest.put(PdpKlient.ENVIRONMENT_AUTH_TOKEN, idToken);
        pdpKlient.forespørTilgang(pdpRequest);

        String xacmlRequestString = captor.getValue().build().toString();

        assertThat(xacmlRequestString.contains("12345678900")).isTrue();
        assertThat(xacmlRequestString.contains("00987654321")).isTrue();
        assertThat(xacmlRequestString.contains("15151515151")).isTrue();
    }

    @Test
    public void sporingsloggListeSkalHaSammeRekkefølgePåidenterSomXacmlRequest() throws Exception {
        AbacIdToken idToken = AbacIdToken.withOidcToken(JWT_TOKEN);
        XacmlResponseWrapper responseWrapper = createResponse("xacml3response.json");
        ArgumentCaptor<XacmlRequestBuilder> captor = ArgumentCaptor.forClass(XacmlRequestBuilder.class);

        when(pdpConsumerMock.evaluate(captor.capture())).thenReturn(responseWrapper);
        Set<String> personnr = new HashSet<>();
        personnr.add("12345678900");
        personnr.add("00987654321");
        personnr.add("15151515151");

        PdpRequest pdpRequest = lagPdpRequest();
        pdpRequest.put(NavAbacCommonAttributter.RESOURCE_FELLES_PERSON_FNR, personnr);
        pdpRequest.put(PdpKlient.ENVIRONMENT_AUTH_TOKEN, idToken);
        pdpKlient.forespørTilgang(pdpRequest);

        var xacmlRequest = captor.getValue().build();
        var resourceArray = xacmlRequest.categories().get(Category.Resource);

        List<String> personer = pdpRequest.getListOfString(NavAbacCommonAttributter.RESOURCE_FELLES_PERSON_FNR);

        for (int i = 0; i < personer.size(); i++) {
            assertThat(resourceArray.toString().contains(personer.get(i))).isTrue();
        }
    }

    private PdpRequest lagPdpRequest() {
        PdpRequest request = new PdpRequest();
        request.put(NavAbacCommonAttributter.RESOURCE_FELLES_DOMENE, "omsorgspenger");
        request.put(AbacAttributter.XACML_1_0_ACTION_ACTION_ID, BeskyttetRessursActionAttributt.READ.getEksternKode());
        request.put(NavAbacCommonAttributter.RESOURCE_FELLES_RESOURCE_TYPE, BeskyttetRessursKoder.FAGSAK);
        return request;
    }

    private XacmlResponseWrapper createResponse(String jsonFile) throws Exception {
        File file = new File(getClass().getClassLoader().getResource(jsonFile).getFile());
        return new XacmlResponseWrapper(Xacml10JsonProfile.createMapper().readValue(file, XacmlResponse.class));
    }
}
