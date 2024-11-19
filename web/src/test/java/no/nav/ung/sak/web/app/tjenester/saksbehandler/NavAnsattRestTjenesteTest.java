package no.nav.ung.sak.web.app.tjenester.saksbehandler;

import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.User;
import no.nav.ung.sak.kontrakt.abac.InnloggetAnsattDto;
import no.nav.ung.sak.web.app.tjenester.microsoftgraph.MSGraphBruker;
import no.nav.ung.sak.web.app.tjenester.microsoftgraph.MicrosoftGraphTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class NavAnsattRestTjenesteTest {
    private static final String gruppenavnSaksbehandler = "Saksbehandler";
    private static final String gruppenavnVeileder = "Veileder";
    private static final String gruppenavnBeslutter = "Beslutter";
    private static final String gruppenavnOverstyrer = "Overstyrer";
    private static final String gruppenavnEgenAnsatt = "EgenAnsatt";
    private static final String gruppenavnKode6 = "Kode6";
    private static final String gruppenavnKode7 = "Kode7";
    private static final Boolean skalViseDetaljerteFeilmeldinger = true;
    private NavAnsattRestTjeneste saksbehandlerTjeneste;

    private MicrosoftGraphTjeneste microsoftGraphTjeneste = mock(MicrosoftGraphTjeneste.class);

    @BeforeEach
    public void setUp() {
        saksbehandlerTjeneste = new NavAnsattRestTjeneste(gruppenavnSaksbehandler, gruppenavnVeileder, gruppenavnBeslutter, gruppenavnOverstyrer, gruppenavnEgenAnsatt, gruppenavnKode6, gruppenavnKode7, skalViseDetaljerteFeilmeldinger, microsoftGraphTjeneste);
    }

    @Test
    public void skalMappeSaksbehandlerGruppeTilKanSaksbehandleRettighet() {
        MSGraphBruker brukerUtenforSaksbehandlerGruppe = getTestBruker();
        MSGraphBruker brukerISaksbehandlerGruppe = getTestBruker(gruppenavnSaksbehandler);

        InnloggetAnsattDto innloggetBrukerUtenSaksbehandlerRettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerUtenforSaksbehandlerGruppe);
        InnloggetAnsattDto innloggetBrukerMedSaksbehandlerRettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerISaksbehandlerGruppe);

        assertThat(innloggetBrukerUtenSaksbehandlerRettighet.getKanSaksbehandle()).isFalse();
        assertThat(innloggetBrukerMedSaksbehandlerRettighet.getKanSaksbehandle()).isTrue();
    }

    @Test
    public void skalMappeVeilederGruppeTilKanVeiledeRettighet() {
        MSGraphBruker brukerUtenforVeilederGruppe = getTestBruker();
        MSGraphBruker brukerIVeilederGruppe = getTestBruker(gruppenavnVeileder);

        InnloggetAnsattDto innloggetBrukerUtenVeilederRettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerUtenforVeilederGruppe);
        InnloggetAnsattDto innloggetBrukerMedVeilederRettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerIVeilederGruppe);

        assertThat(innloggetBrukerUtenVeilederRettighet.getKanVeilede()).isFalse();
        assertThat(innloggetBrukerMedVeilederRettighet.getKanVeilede()).isTrue();
    }

    @Test
    public void skalMappeBeslutterGruppeTilKanBeslutteRettighet() {
        MSGraphBruker brukerUtenforBeslutterGruppe = getTestBruker();
        MSGraphBruker brukerIBeslutterGruppe = getTestBruker(gruppenavnBeslutter);

        InnloggetAnsattDto innloggetBrukerUtenBeslutterRettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerUtenforBeslutterGruppe);
        InnloggetAnsattDto innloggetBrukerMedBeslutterRettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerIBeslutterGruppe);

        assertThat(innloggetBrukerUtenBeslutterRettighet.getKanBeslutte()).isFalse();
        assertThat(innloggetBrukerMedBeslutterRettighet.getKanBeslutte()).isTrue();
    }

    @Test
    public void skalMappeOverstyrerGruppeTilKanOverstyreRettighet() {
        MSGraphBruker brukerUtenforOverstyrerGruppe = getTestBruker();
        MSGraphBruker brukerIOverstyrerGruppe = getTestBruker(gruppenavnOverstyrer);

        InnloggetAnsattDto innloggetBrukerUtenOverstyrerRettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerUtenforOverstyrerGruppe);
        InnloggetAnsattDto innloggetBrukerMedOverstyrerRettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerIOverstyrerGruppe);

        assertThat(innloggetBrukerUtenOverstyrerRettighet.getKanOverstyre()).isFalse();
        assertThat(innloggetBrukerMedOverstyrerRettighet.getKanOverstyre()).isTrue();
    }

    @Test
    public void skalMappeEgenAnsattGruppeTilKanBehandleEgenAnsattRettighet() {
        MSGraphBruker brukerUtenforEgenAnsattGruppe = getTestBruker();
        MSGraphBruker brukerIEgenAnsattGruppe = getTestBruker(gruppenavnEgenAnsatt);

        InnloggetAnsattDto innloggetBrukerUtenEgenAnsattRettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerUtenforEgenAnsattGruppe);
        InnloggetAnsattDto innloggetBrukerMedEgenAnsattRettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerIEgenAnsattGruppe);

        assertThat(innloggetBrukerUtenEgenAnsattRettighet.getKanBehandleKodeEgenAnsatt()).isFalse();
        assertThat(innloggetBrukerMedEgenAnsattRettighet.getKanBehandleKodeEgenAnsatt()).isTrue();
    }

    @Test
    public void skalMappeKode6GruppeTilKanBehandleKode6Rettighet() {
        MSGraphBruker brukerUtenforKode6Gruppe = getTestBruker();
        MSGraphBruker brukerIKode6Gruppe = getTestBruker(gruppenavnKode6);

        InnloggetAnsattDto innloggetBrukerUtenKode6Rettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerUtenforKode6Gruppe);
        InnloggetAnsattDto innloggetBrukerMedKode6Rettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerIKode6Gruppe);

        assertThat(innloggetBrukerUtenKode6Rettighet.getKanBehandleKode6()).isFalse();
        assertThat(innloggetBrukerMedKode6Rettighet.getKanBehandleKode6()).isTrue();
    }

    @Test
    public void skalMappeKode7GruppeTilKanBehandleKode7Rettighet() {
        MSGraphBruker brukerUtenforKode7Gruppe = getTestBruker();
        MSGraphBruker brukerIKode7Gruppe = getTestBruker(gruppenavnKode7);

        InnloggetAnsattDto innloggetBrukerUtenKode7Rettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerUtenforKode7Gruppe);
        InnloggetAnsattDto innloggetBrukerMedKode7Rettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerIKode7Gruppe);

        assertThat(innloggetBrukerUtenKode7Rettighet.getKanBehandleKode7()).isFalse();
        assertThat(innloggetBrukerMedKode7Rettighet.getKanBehandleKode7()).isTrue();
    }

    private static MSGraphBruker getTestBruker(String... grupper) {
        User user = new User();
        user.setDisplayName("Testbruker");
        return new MSGraphBruker(user, Arrays.stream(grupper).map(g -> {
            Group group = new Group();
            group.setDisplayName(g);
            return group;
        }).toList());
    }
}
