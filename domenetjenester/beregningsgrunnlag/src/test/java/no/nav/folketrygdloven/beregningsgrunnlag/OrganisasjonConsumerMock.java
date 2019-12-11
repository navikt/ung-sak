package no.nav.folketrygdloven.beregningsgrunnlag;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;

import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.OrganisasjonsDetaljer;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.OrgnrForOrganisasjon;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.UstrukturertNavn;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.Virksomhet;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.VirksomhetDetaljer;
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentOrganisasjonResponse;
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentVirksomhetsOrgnrForJuridiskOrgnrBolkResponse;
import no.nav.vedtak.felles.integrasjon.felles.ws.DateUtil;
import no.nav.vedtak.felles.integrasjon.organisasjon.OrganisasjonConsumer;
import no.nav.vedtak.felles.integrasjon.organisasjon.hent.HentOrganisasjonForJuridiskRequest;
import no.nav.vedtak.felles.integrasjon.organisasjon.hent.HentOrganisasjonRequest;

@ApplicationScoped
@Alternative
public class OrganisasjonConsumerMock implements OrganisasjonConsumer {

    private static final LocalDate NOW = LocalDate.now();
    private static final String MOCK_ORG = "EPLEHUSET AS";
    static final String MOCK_JURIDISK = "EPLEHUSET AS(jurdisk)";
    private static final LocalDate REGISTRERINGSDATO = NOW.minusMonths(1);

    @Override
    public HentOrganisasjonResponse hentOrganisasjon(HentOrganisasjonRequest var1) {
        HentOrganisasjonResponse response = new HentOrganisasjonResponse();
        Virksomhet virksomhet = new Virksomhet();
        UstrukturertNavn ustrukturertNavn = new UstrukturertNavn();
        ustrukturertNavn.getNavnelinje().add(MOCK_ORG);
        virksomhet.setOrgnummer(var1.getOrgnummer());
        virksomhet.setNavn(ustrukturertNavn);
        virksomhet.setOrganisasjonDetaljer(lagOrganisasjonsDetaljer());
        virksomhet.setVirksomhetDetaljer(lagVirksomhetDetaljer());
        response.setOrganisasjon(virksomhet);
        return response;
    }

    @Override
    public HentVirksomhetsOrgnrForJuridiskOrgnrBolkResponse hentOrganisajonerForJuridiskOrgnr(HentOrganisasjonForJuridiskRequest request) {
        HentVirksomhetsOrgnrForJuridiskOrgnrBolkResponse response = new HentVirksomhetsOrgnrForJuridiskOrgnrBolkResponse();
        OrgnrForOrganisasjon orgnrForOrganisasjon = new OrgnrForOrganisasjon();
        orgnrForOrganisasjon.setOrganisasjonsnummer(request.getOrgnummer());
        orgnrForOrganisasjon.setJuridiskOrganisasjonsnummer(MOCK_JURIDISK);
        response.getOrgnrForOrganisasjonListe().add(orgnrForOrganisasjon);
        return response;
    }

    private OrganisasjonsDetaljer lagOrganisasjonsDetaljer() {
        OrganisasjonsDetaljer detaljer = new OrganisasjonsDetaljer();

        detaljer.setRegistreringsDato(DateUtil
            .convertToXMLGregorianCalendar(REGISTRERINGSDATO));

        return detaljer;
    }

    private VirksomhetDetaljer lagVirksomhetDetaljer() {
        VirksomhetDetaljer detaljer = new VirksomhetDetaljer();

        detaljer.setOppstartsdato(DateUtil
            .convertToXMLGregorianCalendar(REGISTRERINGSDATO));

        return detaljer;
    }
}