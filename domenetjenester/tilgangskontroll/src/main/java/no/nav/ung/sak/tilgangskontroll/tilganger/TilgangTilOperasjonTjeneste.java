package no.nav.ung.sak.tilgangskontroll.tilganger;

import jakarta.enterprise.context.Dependent;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.ung.sak.tilgangskontroll.TilgangsbeslutningInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@Dependent
public class TilgangTilOperasjonTjeneste {

    private final static Logger logger = LoggerFactory.getLogger(TilgangTilOperasjonTjeneste.class);

    public Set<IkkeTilgangÅrsak> sjekkTilgangTilOperasjon(TilgangerBruker tilganger, TilgangsbeslutningInput.Operasjon operasjon, TilgangsbeslutningInput.Saksinformasjon saksinformasjon) {
        try {
            return internSjekkTilgangTilOperasjon(tilganger, operasjon, saksinformasjon);
        } catch (Exception e) {
            logger.warn("Fikk teknisk feil i tilgangssjekk, gir derfor ikke tilgang til operasjonen", e);
            return Set.of(IkkeTilgangÅrsak.TEKNISK_FEIL);
        }
    }

    Set<IkkeTilgangÅrsak> internSjekkTilgangTilOperasjon(TilgangerBruker tilganger, TilgangsbeslutningInput.Operasjon operasjon, TilgangsbeslutningInput.Saksinformasjon saksinformasjon) {
        return switch (operasjon.getResource()) {
            case FAGSAK -> sjekkTilgangTilFagsak(tilganger, operasjon.getAction(), saksinformasjon);
            case VENTEFRIST -> sjekkTilgangTilVentefrist(tilganger);
            case APPLIKASJON -> sjekkTilgangTilApplikasjon(tilganger);
            case DRIFT -> sjekkTilgangTilDrift(tilganger);
        };
    }

    private Set<IkkeTilgangÅrsak> sjekkTilgangTilFagsak(TilgangerBruker tilganger, BeskyttetRessursActionAttributt action, TilgangsbeslutningInput.Saksinformasjon saksinformasjon) {
        return switch (action) {
            case READ -> sjekkTilgangTilLesFagsak(tilganger);
            case UPDATE -> sjekkTilgangTilOppdaterFagsak(tilganger, saksinformasjon);
            case CREATE -> sjekkTilgangTilOpprettFagsak(tilganger);
            case DELETE -> throw new IllegalArgumentException("Action DELETE er ikke i bruk");
            case DUMMY -> throw new IllegalArgumentException("Action DUMMY er ikke i bruk");
        };
    }

    private static Set<IkkeTilgangÅrsak> sjekkTilgangTilLesFagsak(TilgangerBruker tilganger) {
        return tilganger.kanVeilede() || tilganger.kanSaksbehandle() ? Set.of() : Set.of(IkkeTilgangÅrsak.HAR_IKKE_TILGANG_Å_LESE_FAGSAK);
    }

    private static Set<IkkeTilgangÅrsak> sjekkTilgangTilOppdaterFagsak(TilgangerBruker tilganger, TilgangsbeslutningInput.Saksinformasjon saksinformasjon) {
        if (!tilganger.kanSaksbehandle()) {
            return Set.of(IkkeTilgangÅrsak.ER_IKKE_SAKSBEHANDLER);
        }
        return switch (saksinformasjon.getBehandlingStatus()) {
            case FATTE_VEDTAK -> sjekkTilgangTilBeslutter(tilganger, saksinformasjon);
            case OPPRETTET, UTREDES -> sjekkTilgangTilSaksbehandling(tilganger, saksinformasjon);
            case null -> Set.of(IkkeTilgangÅrsak.BEHANDLINGEN_ER_LUKKET);
        };
    }

    private static Set<IkkeTilgangÅrsak> sjekkTilgangTilBeslutter(TilgangerBruker tilganger, TilgangsbeslutningInput.Saksinformasjon saksinformasjon) {
        if (!tilganger.kanBeslutte()) {
            return Set.of(IkkeTilgangÅrsak.ER_IKKE_BESLUTTER);
        }
        if (tilganger.getBrukernavn().equals(saksinformasjon.getIdentAnsvarligSaksbehandler())) {
            return Set.of(IkkeTilgangÅrsak.KAN_IKKE_BESLUTTE_EGEN_SAK);
        }
        if (saksinformasjon.getAksjonspunktType() != AksjonspunktType.MANUELL) {
            return Set.of(IkkeTilgangÅrsak.FEIL_AKSJONSPUNKT_FOR_BESLUTTER);
        } else {
            return Set.of();
        }
    }

    private static Set<IkkeTilgangÅrsak> sjekkTilgangTilSaksbehandling(TilgangerBruker tilganger, TilgangsbeslutningInput.Saksinformasjon saksinformasjon) {
        if (saksinformasjon.getAksjonspunktType() == AksjonspunktType.OVERSTYRING) {
            return tilganger.kanOverstyre() ? Set.of() : Set.of(IkkeTilgangÅrsak.ER_IKKE_OVERSTYRER);
        } else if (saksinformasjon.getAksjonspunktType() == AksjonspunktType.AUTOPUNKT) {
            return Set.of(IkkeTilgangÅrsak.FEIL_AKSJONSPUNKT_FOR_SAKSBEHANDLING);
        } else {
            return Set.of();
        }
    }

    private static Set<IkkeTilgangÅrsak> sjekkTilgangTilOpprettFagsak(TilgangerBruker tilganger) {
        return tilganger.kanSaksbehandle() ? Set.of() : Set.of(IkkeTilgangÅrsak.ER_IKKE_SAKSBEHANDLER);
    }

    private static Set<IkkeTilgangÅrsak> sjekkTilgangTilApplikasjon(TilgangerBruker tilganger) {
        return tilganger.kanVeilede() || tilganger.kanSaksbehandle() || tilganger.kanBeslutte() || tilganger.kanOverstyre() || tilganger.kanDrifte() ? Set.of() : Set.of(IkkeTilgangÅrsak.HAR_IKKE_TILGANG_TIL_APPLIKASJONEN);
    }

    private static Set<IkkeTilgangÅrsak> sjekkTilgangTilDrift(TilgangerBruker tilganger) {
        return tilganger.kanDrifte() ? Set.of() : Set.of(IkkeTilgangÅrsak.ER_IKKE_DRIFTER);
    }

    private static Set<IkkeTilgangÅrsak> sjekkTilgangTilVentefrist(TilgangerBruker tilganger) {
        return tilganger.kanVeilede() || tilganger.kanSaksbehandle() ? Set.of() : Set.of(IkkeTilgangÅrsak.ER_IKKE_VEILEDER_ELLER_SAKSBEHANDLER);
    }


}
