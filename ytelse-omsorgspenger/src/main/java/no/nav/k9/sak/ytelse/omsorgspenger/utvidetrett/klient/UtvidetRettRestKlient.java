package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.validation.Validation;
import javax.validation.Validator;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
@Default
public class UtvidetRettRestKlient implements UtvidetRettKlient {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private OidcRestClient restKlient;
    private URI basisEndpoint;

    protected UtvidetRettRestKlient() {
        // for proxying
    }

    @Inject
    public UtvidetRettRestKlient(OidcRestClient restKlient, @KonfigVerdi(value = "k9.oms.utvidetrett.url") URI endpoint) {
        this.restKlient = restKlient;
        this.basisEndpoint = toUri(endpoint, "/k9/utvidetrett");
    }

    @Override
    public void forkast(FagsakYtelseType ytelseType, UUID behandlingUUID) {
        var subpath = getSubpath(ytelseType);
        var endpoint = URI.create(String.format("%s/%s/%s/forkastet", basisEndpoint, subpath, behandlingUUID));
        try {
            restKlient.patch(endpoint, Object.class);
        } catch (Exception e) {
            throw new UtvidetRettRestException("K9-901302", "Kunne ikke markere forkastet for : %s", e, endpoint);
        }
    }

    @Override
    public void avslått(FagsakYtelseType ytelseType, UUID behandlingUUID) {
        var subpath = getSubpath(ytelseType);
        var endpoint = URI.create(String.format("%s/%s/%s/avslått", basisEndpoint, subpath, behandlingUUID));
        try {
            restKlient.patch(endpoint, Object.class);
        } catch (Exception e) {
            throw new UtvidetRettRestException("K9-901301", "Kunne ikke markere avslått for : %s", e, endpoint);
        }
    }

    @Override
    public void oppgaveLøst(FagsakYtelseType ytelseType, UUID behandlingUUID, Oppgaver oppgaver) {
        var subpath = getSubpath(ytelseType);
        var endpoint = URI.create(String.format("%s/%s/%s/avslått", basisEndpoint, subpath, behandlingUUID));
        try {
            restKlient.patch(endpoint, oppgaver);
        } catch (Exception e) {
            throw new UtvidetRettRestException("K9-901301", "Kunne ikke markere avslått for : %s", e, endpoint);
        }
    }

    @Override
    public void innvilget(FagsakYtelseType ytelseType, UUID behandlingUUID) {
        var subpath = getSubpath(ytelseType);
        var endpoint = URI.create(String.format("%s/%s/%s/innvilget", basisEndpoint, subpath, behandlingUUID));
        try {
            restKlient.patch(endpoint, Object.class);
        } catch (Exception e) {
            throw new UtvidetRettRestException("K9-901300", "Kunne ikke markere innvilget for : %s", e, endpoint);
        }
    }

    @Override
    public UtvidetRettResultat hentResultat(FagsakYtelseType ytelseType, Saksnummer saksnummer, UUID behandlingUUID) {
        var subpath = getSubpath(ytelseType);
        var endpoint = URI.create(String.format("%s/%s/?saksnummer=%s&behandlingId=%s", basisEndpoint, subpath, saksnummer.getVerdi(), behandlingUUID));

        try {
            var result = restKlient.get(endpoint, UtvidetRettResultat.class);
            var constraints = VALIDATOR.validate(result);
            if (!constraints.isEmpty()) {
                throw new IllegalStateException("Ugyldig response fra " + endpoint + ", behandlingUUID=" + behandlingUUID + ": " + constraints);
            }
            return result;
        } catch (Exception e) {
            throw new UtvidetRettRestException("K9-901305", "Kunne ikke hente resultat for : %s", e, endpoint);
        }
    }

    private Object getSubpath(FagsakYtelseType ytelseType) {
        switch (ytelseType) {
            case OMSORGSPENGER_KS:
                return "kronisk-sykt-barn";
            case OMSORGSPENGER_MA:
                return "midlertidig-alene";
            default:
                throw new UnsupportedOperationException("Støtter ikke utvidet rett for ytelsetype=" + ytelseType);
        }
    }

    private URI toUri(URI baseUri, String relativeUri) {
        String uri = baseUri.toString() + relativeUri;
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig uri: " + uri, e);
        }
    }

}
