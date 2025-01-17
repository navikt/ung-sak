package no.nav.ung.domenetjenester.oppgave.behandlendeenhet;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.arbeidsfordeling.rest.ArbeidsfordelingRequest;
import no.nav.k9.felles.integrasjon.arbeidsfordeling.rest.ArbeidsfordelingRestKlient;

import no.nav.ung.fordel.kodeverdi.Tema;
import no.nav.ung.kodeverk.behandling.BehandlingTema;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.sak.behandlingslager.aktør.GeografiskTilknytning;

@Dependent
public class EnhetsTjeneste {

    private ArbeidsfordelingRestKlient arbeidsfordelingTjeneste;

    public EnhetsTjeneste() {
        // For CDI proxy
    }

    @Inject
    public EnhetsTjeneste(ArbeidsfordelingRestKlient arbeidsfordelingTjeneste) {
        this.arbeidsfordelingTjeneste = arbeidsfordelingTjeneste;
    }

    List<OrganisasjonsEnhet> hentFordelingEnhetId(Tema tema, BehandlingTema behandlingsTema, GeografiskTilknytning geo) {
        var request = ArbeidsfordelingRequest.ny()
                .medTema(tema.getOffisiellKode())
                .medBehandlingstema(behandlingsTema == null ? null : behandlingsTema.getOffisiellKode())
                .medDiskresjonskode(geo.getDiskresjonskode().getKode())
                .medGeografiskOmraade(geo.getTilknytning())
                .build();

        return arbeidsfordelingTjeneste
                .finnEnhet(request)
                .stream()
                .filter(response -> "AKTIV".equalsIgnoreCase(response.getStatus()))
                .map(r -> new OrganisasjonsEnhet(r.getEnhetNr(), r.getEnhetNavn()))
                .collect(Collectors.toList());
    }
}
