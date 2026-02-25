package no.nav.ung.sak.web.server.abac;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import no.nav.k9.felles.sikkerhet.abac.*;
import no.nav.sif.abac.kontrakt.abac.AbacFagsakYtelseType;
import no.nav.sif.abac.kontrakt.abac.ResourceType;
import no.nav.sif.abac.kontrakt.abac.dto.SaksinformasjonOgPersonerTilgangskontrollInputDto;

@Dependent
@Alternative
@Priority(1)
public class AppPdpKlient implements PdpKlient {

    private final SifAbacPdpRestKlient sifAbacPdpRestKlient;

    @Inject
    public AppPdpKlient(SifAbacPdpRestKlient sifAbacPdpRestKlient) {
        this.sifAbacPdpRestKlient = sifAbacPdpRestKlient;
    }

    @Override
    public Tilgangsbeslutning forespørTilgang(PdpRequest pdpRequest) {
        TilgangType tilgangType = TilgangType.INTERNBRUKER; //resterende tilgangtyper er håntdert i PepImpl

        SaksinformasjonOgPersonerTilgangskontrollInputDto tilgangskontrollInput = PdpRequestMapper.map(pdpRequest);

        if (tilgangskontrollInput.operasjon().resource() == ResourceType.APPLIKASJON) {
            //håndtert av azure.application.allowAllUsers=true som bare tillater kall for brukere som har rettighet til å kalle applikasjonen
            return new Tilgangsbeslutning(true, pdpRequest, tilgangType);
        }

        //bruker aktivitetspenger-domene i abac dersom fagsaken er for aktivitetspenger,
        //ellers defaulter vi til UNG-domene i abac (bl.a. drift-rolle er delt)

        boolean gjelderAktivitetspenger = tilgangskontrollInput.saksinformasjon() != null && tilgangskontrollInput.saksinformasjon().ytelseType() == AbacFagsakYtelseType.AKTIVITETSPENGER;
        no.nav.sif.abac.kontrakt.abac.resultat.Tilgangsbeslutning resultat = gjelderAktivitetspenger
            ? sifAbacPdpRestKlient.sjekkTilgangForInnloggetBrukerAktivitetspenger(tilgangskontrollInput)
            : sifAbacPdpRestKlient.sjekkTilgangForInnloggetBrukerUng(tilgangskontrollInput);
        return new Tilgangsbeslutning(resultat.harTilgang(), pdpRequest, tilgangType);
    }
}

