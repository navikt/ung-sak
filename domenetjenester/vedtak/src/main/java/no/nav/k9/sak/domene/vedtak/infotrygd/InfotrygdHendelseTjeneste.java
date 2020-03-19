package no.nav.k9.sak.domene.vedtak.infotrygd;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.FeedDto;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.FeedElement;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class InfotrygdHendelseTjeneste {

    private static final Logger log = LoggerFactory.getLogger(InfotrygdHendelseTjeneste.class);
    private static final String ENDPOINT_KEY = "infotrygd.hendelser.api.url";
    private static final String FOM_DATO = "fomDato";
    private static final String AKTØR_ID = "aktorId";

    private OidcRestClient oidcRestClient;
    private URI endpoint;
    private InfotrygdHendelseMapper mapper;

    InfotrygdHendelseTjeneste() {
        //CDI
    }

    @Inject
    public InfotrygdHendelseTjeneste(@KonfigVerdi(ENDPOINT_KEY) URI endpoint,
                                         OidcRestClient oidcRestClient) {
        this.endpoint = endpoint;
        this.oidcRestClient = oidcRestClient;
        this.mapper = new InfotrygdHendelseMapper();
    }

    public List<InfotrygdHendelse> hentHendelsesListFraInfotrygdFeed(Behandling behandling, LocalDate fom) {
        List<InfotrygdHendelse> hendelseList = new ArrayList<>();
        String fomStr = fom.toString();
        URI request = request(fomStr, behandling.getAktørId().getId());

        FeedDto feed = oidcRestClient.get(request, FeedDto.class);

        log.debug("Fått response fra Infotrygd");

        if (feed == null) {
            log.warn("Kunne ikke hente infotrygdFeed for endpoint={}", request); // NOSONAR
            throw new IllegalStateException("Kunne ikke hente InfotrygdFeed");
        }

        if (feed.getElementer() != null && !feed.getElementer().isEmpty()) {
            List<FeedElement> feedElementList = feed.getElementer();
            for (FeedElement feedElement : feedElementList) {
                hendelseList.add(mapper.mapFraFeedTilInfotrygdHendelse(feedElement));
            }
            log.info("Hendelser som ble lest fra InfotrygdFeed {} med Sekvensnummer {}; Behandling: {}",
                hendelseList.stream().map(InfotrygdHendelse::getType).collect(Collectors.toList()),
                hendelseList.stream().map(InfotrygdHendelse::getSekvensnummer).collect(Collectors.toList()),
                behandling.getId());
            return hendelseList;
        }
        log.info("InfotrygdFeed inneholder ingen hendelser fra og med {}; Behandling: {}", fomStr, behandling.getId());
        return Collections.emptyList();
    }

    private URI request(String fomDato, String aktørId) {
        try {
            return new URIBuilder(endpoint)
                .addParameter(FOM_DATO, fomDato)
                .addParameter(AKTØR_ID, aktørId)
                .build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
