package no.nav.k9.sak.web.app.tjenester.saksbehandler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import no.nav.k9.sak.kontrakt.abac.InnloggetAnsattDto;
import no.nav.k9.sak.web.app.tjenester.saksbehandler.NavAnsattRestTjeneste;
import no.nav.vedtak.felles.integrasjon.ldap.LdapBruker;

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

    @Before
    public void setUp() {
        saksbehandlerTjeneste = new NavAnsattRestTjeneste(gruppenavnSaksbehandler, gruppenavnVeileder, gruppenavnBeslutter, gruppenavnOverstyrer, gruppenavnEgenAnsatt, gruppenavnKode6, gruppenavnKode7, skalViseDetaljerteFeilmeldinger);
    }

    @Test
    public void skalMappeSaksbehandlerGruppeTilKanSaksbehandleRettighet() {
        LdapBruker brukerUtenforSaksbehandlerGruppe = getTestBruker();
        LdapBruker brukerISaksbehandlerGruppe = getTestBruker(gruppenavnSaksbehandler);

        InnloggetAnsattDto innloggetBrukerUtenSaksbehandlerRettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerUtenforSaksbehandlerGruppe);
        InnloggetAnsattDto innloggetBrukerMedSaksbehandlerRettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerISaksbehandlerGruppe);

        assertThat(innloggetBrukerUtenSaksbehandlerRettighet.getKanSaksbehandle()).isFalse();
        assertThat(innloggetBrukerMedSaksbehandlerRettighet.getKanSaksbehandle()).isTrue();
    }

    @Test
    public void skalMappeVeilederGruppeTilKanVeiledeRettighet() {
        LdapBruker brukerUtenforVeilederGruppe = getTestBruker();
        LdapBruker brukerIVeilederGruppe = getTestBruker(gruppenavnVeileder);

        InnloggetAnsattDto innloggetBrukerUtenVeilederRettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerUtenforVeilederGruppe);
        InnloggetAnsattDto innloggetBrukerMedVeilederRettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerIVeilederGruppe);

        assertThat(innloggetBrukerUtenVeilederRettighet.getKanVeilede()).isFalse();
        assertThat(innloggetBrukerMedVeilederRettighet.getKanVeilede()).isTrue();
    }

    @Test
    public void skalMappeBeslutterGruppeTilKanBeslutteRettighet() {
        LdapBruker brukerUtenforBeslutterGruppe = getTestBruker();
        LdapBruker brukerIBeslutterGruppe = getTestBruker(gruppenavnBeslutter);

        InnloggetAnsattDto innloggetBrukerUtenBeslutterRettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerUtenforBeslutterGruppe);
        InnloggetAnsattDto innloggetBrukerMedBeslutterRettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerIBeslutterGruppe);

        assertThat(innloggetBrukerUtenBeslutterRettighet.getKanBeslutte()).isFalse();
        assertThat(innloggetBrukerMedBeslutterRettighet.getKanBeslutte()).isTrue();
    }

    @Test
    public void skalMappeOverstyrerGruppeTilKanOverstyreRettighet() {
        LdapBruker brukerUtenforOverstyrerGruppe = getTestBruker();
        LdapBruker brukerIOverstyrerGruppe = getTestBruker(gruppenavnOverstyrer);

        InnloggetAnsattDto innloggetBrukerUtenOverstyrerRettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerUtenforOverstyrerGruppe);
        InnloggetAnsattDto innloggetBrukerMedOverstyrerRettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerIOverstyrerGruppe);

        assertThat(innloggetBrukerUtenOverstyrerRettighet.getKanOverstyre()).isFalse();
        assertThat(innloggetBrukerMedOverstyrerRettighet.getKanOverstyre()).isTrue();
    }

    @Test
    public void skalMappeEgenAnsattGruppeTilKanBehandleEgenAnsattRettighet() {
        LdapBruker brukerUtenforEgenAnsattGruppe = getTestBruker();
        LdapBruker brukerIEgenAnsattGruppe = getTestBruker(gruppenavnEgenAnsatt);

        InnloggetAnsattDto innloggetBrukerUtenEgenAnsattRettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerUtenforEgenAnsattGruppe);
        InnloggetAnsattDto innloggetBrukerMedEgenAnsattRettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerIEgenAnsattGruppe);

        assertThat(innloggetBrukerUtenEgenAnsattRettighet.getKanBehandleKodeEgenAnsatt()).isFalse();
        assertThat(innloggetBrukerMedEgenAnsattRettighet.getKanBehandleKodeEgenAnsatt()).isTrue();
    }

    @Test
    public void skalMappeKode6GruppeTilKanBehandleKode6Rettighet() {
        LdapBruker brukerUtenforKode6Gruppe = getTestBruker();
        LdapBruker brukerIKode6Gruppe = getTestBruker(gruppenavnKode6);

        InnloggetAnsattDto innloggetBrukerUtenKode6Rettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerUtenforKode6Gruppe);
        InnloggetAnsattDto innloggetBrukerMedKode6Rettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerIKode6Gruppe);

        assertThat(innloggetBrukerUtenKode6Rettighet.getKanBehandleKode6()).isFalse();
        assertThat(innloggetBrukerMedKode6Rettighet.getKanBehandleKode6()).isTrue();
    }

    @Test
    public void skalMappeKode7GruppeTilKanBehandleKode7Rettighet() {
        LdapBruker brukerUtenforKode7Gruppe = getTestBruker();
        LdapBruker brukerIKode7Gruppe = getTestBruker(gruppenavnKode7);

        InnloggetAnsattDto innloggetBrukerUtenKode7Rettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerUtenforKode7Gruppe);
        InnloggetAnsattDto innloggetBrukerMedKode7Rettighet = saksbehandlerTjeneste.getInnloggetBrukerDto(null, brukerIKode7Gruppe);

        assertThat(innloggetBrukerUtenKode7Rettighet.getKanBehandleKode7()).isFalse();
        assertThat(innloggetBrukerMedKode7Rettighet.getKanBehandleKode7()).isTrue();
    }

    private static LdapBruker getTestBruker(String... grupper) {
        return new LdapBruker("Testbruker", List.of(grupper));
    }
}
