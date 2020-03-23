package no.nav.k9.sak.mottak.kompletthettjeneste.psb;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;

import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadVedleggEntitet;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.dokument.arkiv.DokumentArkivTjeneste;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.mottak.kompletthettjeneste.KompletthetssjekkerTestUtil;
import no.nav.k9.sak.typer.Saksnummer;

public class KompletthetssjekkerSøknadRevurderingTest {

    private static final String INNTEKTSMELDING_OFFISIELL_KODE = DokumentTypeId.INNTEKTSMELDING.getOffisiellKode();

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    private final KompletthetssjekkerTestUtil testUtil = new KompletthetssjekkerTestUtil(repositoryProvider);

    private final DokumentArkivTjeneste dokumentArkivTjeneste = mock(DokumentArkivTjeneste.class);
    private final KompletthetssjekkerSøknadRevurderingImpl kompletthetssjekker = new KompletthetssjekkerSøknadRevurderingImpl(dokumentArkivTjeneste, repositoryProvider, Period.parse("P4W"));

    @Test
    public void skal_utlede_at_et_påkrevd_vedlegg_finnes_i_journal() {
        // Arrange
        var scenario = testUtil.opprettRevurderingsscenario();
        scenario.medSøknad()
            .medElektroniskRegistrert(true)
            .medSøknadsdato(LocalDate.now())
            .leggTilVedlegg(new SøknadVedleggEntitet.Builder().medSkjemanummer(INNTEKTSMELDING_OFFISIELL_KODE).medErPåkrevdISøknadsdialog(true).build())
            .build();
        Behandling behandling = scenario.lagre(repositoryProvider);

        // Matcher med søknad:
        Set<DokumentTypeId> dokumentListe = singleton(DokumentTypeId.INNTEKTSMELDING);
        when(dokumentArkivTjeneste.hentDokumentTypeIdForSak(any(Saksnummer.class), any())).thenReturn(dokumentListe);

        // Act
        List<ManglendeVedlegg> manglendeVedlegg = kompletthetssjekker.utledManglendeVedleggForSøknad(lagRef(behandling));

        // Assert
        assertThat(manglendeVedlegg).isEmpty();
    }

    @Test
    public void skal_utlede_at_et_påkrevd_vedlegg_ikke_finnes_i_journal() {
        // Arrange
        var scenario = testUtil.opprettRevurderingsscenario();
        scenario.medSøknad()
            .medElektroniskRegistrert(true)
            .medSøknadsdato(LocalDate.now())
            .leggTilVedlegg(new SøknadVedleggEntitet.Builder().medSkjemanummer(INNTEKTSMELDING_OFFISIELL_KODE).medErPåkrevdISøknadsdialog(true).build())
            .build();
        Behandling behandling = scenario.lagre(repositoryProvider);

        // Matcher ikke med søknad:
        Set<DokumentTypeId> dokumentListe = singleton(DokumentTypeId.LEGEERKLÆRING);
        when(dokumentArkivTjeneste.hentDokumentTypeIdForSak(any(Saksnummer.class), any())).thenReturn(dokumentListe);

        // Act
        List<ManglendeVedlegg> manglendeVedlegg = kompletthetssjekker.utledManglendeVedleggForSøknad(lagRef(behandling));

        // Assert
        assertThat(manglendeVedlegg).hasSize(1);
        assertThat(manglendeVedlegg.get(0).getDokumentType().getOffisiellKode()).isEqualTo(INNTEKTSMELDING_OFFISIELL_KODE);
    }

    @Test
    public void skal_utlede_at_et_påkrevd_vedlegg_ikke_finnes_i_journal_når_det_ble_mottatt_før_gjeldende_vedtak() {
        // Arrange
        var scenario = testUtil.opprettRevurderingsscenario();
        scenario.medSøknad()
            .medElektroniskRegistrert(true)
            .medSøknadsdato(LocalDate.now())
            .leggTilVedlegg(new SøknadVedleggEntitet.Builder().medSkjemanummer(INNTEKTSMELDING_OFFISIELL_KODE).medErPåkrevdISøknadsdialog(true).build())
            .build();
        Behandling revurdering = scenario.lagre(repositoryProvider);

        // Matcher med søknad, men er mottatt ifbm førstegangsbehandlingen:
        Set<DokumentTypeId> dokumentListe = new HashSet<>();
        dokumentListe.add(DokumentTypeId.LEGEERKLÆRING);
        when(dokumentArkivTjeneste.hentDokumentTypeIdForSak(any(Saksnummer.class), any())).thenReturn(dokumentListe);

        // Act
        List<ManglendeVedlegg> manglendeVedlegg = kompletthetssjekker.utledManglendeVedleggForSøknad(lagRef(revurdering));

        // Assert
        assertThat(manglendeVedlegg).hasSize(1);
        assertThat(manglendeVedlegg.get(0).getDokumentType().getOffisiellKode()).isEqualTo(INNTEKTSMELDING_OFFISIELL_KODE);
    }

    @Test
    public void skal_utlede_at_et_påkrevd_vedlegg_som_finnes_i_mottatte_dokumenter_ikke_mangler_selv_om_vedlegget_fra_journal_har_mottatt_dato_null() {
        // Arrange
        var scenario = testUtil.opprettRevurderingsscenario();
        scenario.medSøknad()
            .medElektroniskRegistrert(true)
            .medSøknadsdato(LocalDate.now())
            .leggTilVedlegg(new SøknadVedleggEntitet.Builder().medSkjemanummer(INNTEKTSMELDING_OFFISIELL_KODE).medErPåkrevdISøknadsdialog(true).build())
            .build();
        Behandling revurdering = scenario.lagre(repositoryProvider);

        // Matcher med søknad, men mangler mottatt dato:
        Set<DokumentTypeId> dokumentListe = singleton(DokumentTypeId.INNTEKTSMELDING);
        when(dokumentArkivTjeneste.hentDokumentTypeIdForSak(any(Saksnummer.class), any())).thenReturn(dokumentListe);

        MottattDokument mottattDokument = new MottattDokument.Builder()
            .medFagsakId(revurdering.getFagsakId())
            .medBehandlingId(revurdering.getId())
            .medMottattDato(LocalDate.now())
            .build();
        repositoryProvider.getMottatteDokumentRepository().lagre(mottattDokument);

        // Act
        List<ManglendeVedlegg> manglendeVedlegg = kompletthetssjekker.utledManglendeVedleggForSøknad(lagRef(revurdering));

        // Assert
        assertThat(manglendeVedlegg).isEmpty();
    }

    @Test
    public void skal_utlede_at_et_påkrevd_vedlegg_som_ikke_finnes_i_mottatte_dokumenter_mangler_når_vedlegget_fra_journal_har_mottatt_dato_null() {
        // Arrange
        var scenario = testUtil.opprettRevurderingsscenario();
        scenario.medSøknad()
            .medElektroniskRegistrert(true)
            .medSøknadsdato(LocalDate.now())
            .leggTilVedlegg(new SøknadVedleggEntitet.Builder().medSkjemanummer(INNTEKTSMELDING_OFFISIELL_KODE).medErPåkrevdISøknadsdialog(true).build())
            .build();
        Behandling revurdering = scenario.lagre(repositoryProvider);

        // Matcher med søknad, men mangler mottatt dato:
        Set<DokumentTypeId> dokumentListe = new HashSet<>();
        dokumentListe.add(DokumentTypeId.INNTEKTSMELDING);
        when(dokumentArkivTjeneste.hentDokumentTypeIdForSak(any(Saksnummer.class), any())).thenReturn(Collections.emptySet());

        // Act
        List<ManglendeVedlegg> manglendeVedlegg = kompletthetssjekker.utledManglendeVedleggForSøknad(lagRef(revurdering));

        // Assert
        assertThat(manglendeVedlegg).hasSize(1);
        assertThat(manglendeVedlegg.get(0).getDokumentType().getOffisiellKode()).isEqualTo(INNTEKTSMELDING_OFFISIELL_KODE);
    }

    @Test
    public void skal_utlede_at_et_dokument_som_er_påkrevd_som_følger_av_utsettelse_finnes_i_journal() {
        // Arrange
        var scenario = testUtil.opprettRevurderingsscenario();
        Behandling behandling = scenario.lagre(repositoryProvider);

        // Matcher med utsettelse:
        Set<DokumentTypeId> dokumentListe = singleton(DokumentTypeId.INNTEKTSMELDING);
        when(dokumentArkivTjeneste.hentDokumentTypeIdForSak(any(Saksnummer.class), any())).thenReturn(dokumentListe);

        // Act
        List<ManglendeVedlegg> manglendeVedlegg = kompletthetssjekker.utledManglendeVedleggForSøknad(lagRef(behandling));

        // Assert
        assertThat(manglendeVedlegg).isEmpty();
    }

    @Test
    public void skal_utlede_at_et_dokument_som_er_påkrevd_finnes_ved_vedtak_samme_dag() {
        // Arrange
        LocalDate søknadsDato = LocalDate.now().minusWeeks(2);
        var scenario = testUtil.opprettRevurderingsscenario();
        Behandling behandling = scenario.lagre(repositoryProvider);
        testUtil.byggOgLagreSøknadMed(behandling, false, søknadsDato);

        // Matcher med utsettelse:
        Set<DokumentTypeId> dokumentListe = singleton(DokumentTypeId.INNTEKTSMELDING);
        when(dokumentArkivTjeneste.hentDokumentTypeIdForSak(eq(behandling.getFagsak().getSaksnummer()), eq(søknadsDato))).thenReturn(dokumentListe);

        // Act
        List<ManglendeVedlegg> manglendeVedlegg = kompletthetssjekker.utledManglendeVedleggForSøknad(lagRef(behandling));

        // Assert
        assertThat(manglendeVedlegg).isEmpty();
        verify(dokumentArkivTjeneste).hentDokumentTypeIdForSak(eq(behandling.getFagsak().getSaksnummer()), eq(søknadsDato));
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }

}
