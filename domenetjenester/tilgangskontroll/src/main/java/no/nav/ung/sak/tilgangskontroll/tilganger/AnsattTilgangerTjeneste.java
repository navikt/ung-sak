package no.nav.ung.sak.tilgangskontroll.tilganger;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.k9.sikkerhet.oidc.token.internal.JwtUtil;

import java.util.List;

@Dependent
public class AnsattTilgangerTjeneste {

    private final String gruppeIdSaksbehandler;
    private final String gruppeIdVeileder;
    private final String gruppeIdBeslutter;
    private final String gruppeIdOverstyrer;
    private final String gruppeIdEgenAnsatt;
    private final String gruppeIdKode6;
    private final String gruppeIdKode7;
    private final String gruppeIdDrift;

    @Inject
    public AnsattTilgangerTjeneste(@KonfigVerdi(value = "bruker.gruppe.id.saksbehandler") String gruppeIdSaksbehandler,
                                   @KonfigVerdi(value = "bruker.gruppe.id.veileder") String gruppeIdVeileder,
                                   @KonfigVerdi(value = "bruker.gruppe.id.beslutter") String gruppeIdBeslutter,
                                   @KonfigVerdi(value = "bruker.gruppe.id.overstyrer") String gruppeIdOverstyrer,
                                   @KonfigVerdi(value = "bruker.gruppe.id.egenansatt") String gruppeIdEgenAnsatt,
                                   @KonfigVerdi(value = "bruker.gruppe.id.kode6") String gruppeIdKode6,
                                   @KonfigVerdi(value = "bruker.gruppe.id.kode7") String gruppeIdKode7,
                                   @KonfigVerdi(value = "bruker.gruppe.id.drift") String gruppeIdDrift
    ) {
        this.gruppeIdSaksbehandler = gruppeIdSaksbehandler;
        this.gruppeIdVeileder = gruppeIdVeileder;
        this.gruppeIdBeslutter = gruppeIdBeslutter;
        this.gruppeIdOverstyrer = gruppeIdOverstyrer;
        this.gruppeIdEgenAnsatt = gruppeIdEgenAnsatt;
        this.gruppeIdKode6 = gruppeIdKode6;
        this.gruppeIdKode7 = gruppeIdKode7;
        this.gruppeIdDrift = gruppeIdDrift;
    }

    public TilgangerBruker tilgangerForInnloggetBruker() {
        String ident = SubjectHandler.getSubjectHandler().getUid();
        String token = SubjectHandler.getSubjectHandler().getInternSsoToken();
        JwtUtil.CachedClaims claims = JwtUtil.CachedClaims.forToken(token);
        List<String> groupIds = claims.getGroups();

        return tilgangerForBruker(ident, groupIds);
    }

    public TilgangerBruker tilgangerForBruker(String ident, List<String> groupIds) {
        return TilgangerBruker.builder()
            .medBrukernavn(ident)
            .medKanBehandleKode6(groupIds.contains(gruppeIdKode6))
            .medKanBehandleKode7(groupIds.contains(gruppeIdKode7))
            .medKanBehandleEgenAnsatt(groupIds.contains(gruppeIdEgenAnsatt))
            .medKanBeslutte(groupIds.contains(gruppeIdBeslutter))
            .medKanOverstyre(groupIds.contains(gruppeIdOverstyrer))
            .medKanSaksbehandle(groupIds.contains(gruppeIdSaksbehandler))
            .medKanVeilede(groupIds.contains(gruppeIdVeileder))
            .medKanDrifte(groupIds.contains(gruppeIdDrift))
            .build();
    }
}
