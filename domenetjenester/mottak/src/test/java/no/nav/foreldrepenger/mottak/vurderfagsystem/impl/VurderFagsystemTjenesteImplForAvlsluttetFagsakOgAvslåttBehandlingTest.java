package no.nav.foreldrepenger.mottak.vurderfagsystem.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.FordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.DokumentPersistererTjeneste;
import no.nav.foreldrepenger.mottak.publiserer.publish.MottattDokumentPersistertPubliserer;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystem;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesTjeneste;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesUtils;
import no.nav.foreldrepenger.mottak.vurderfagsystem.fp.VurderFagsystemTjenesteImpl;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingTema;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

public class VurderFagsystemTjenesteImplForAvlsluttetFagsakOgAvslåttBehandlingTest {

    private static final Period FRIST_INNSENDING_PERIODE = Period.ofWeeks(6);
    
    private final LocalDate DATO_ETTER_FRISTEN = LocalDate.now().minus(FRIST_INNSENDING_PERIODE.plusDays(2));
    private final LocalDate DATO_FØR_FRISTEN = LocalDate.now().minus(FRIST_INNSENDING_PERIODE.minusDays(2));
    private final AktørId AKTØR_ID = AktørId.dummy();

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    
    private final EntityManager entityManager = repoRule.getEntityManager();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private VurderFagsystemFellesTjeneste vurderFagsystemFellesTjeneste;
    private VilkårResultatRepository vilkårResultatRepository = new VilkårResultatRepository(entityManager);
    private FordelingRepository fordelingrepository = new FordelingRepository(entityManager);

    @Before
    public void setUp() {
        MottatteDokumentRepository mottatteDokumentRepository = new MottatteDokumentRepository(entityManager);
        DokumentPersistererTjeneste dokumentPersistererTjeneste = new DokumentPersistererTjeneste(mock(MottattDokumentPersistertPubliserer.class));

        MottatteDokumentTjeneste mottatteDokumentTjeneste =
            new MottatteDokumentTjeneste(FRIST_INNSENDING_PERIODE, dokumentPersistererTjeneste, mottatteDokumentRepository, vilkårResultatRepository, fordelingrepository, repositoryProvider);

        VurderFagsystemFellesUtils fellesUtils = new VurderFagsystemFellesUtils(behandlingRepository, mottatteDokumentTjeneste, null);

        var fagsakTjeneste = new FagsakTjeneste(repositoryProvider, null);
        var vurderTjeneste = new VurderFagsystemTjenesteImpl(fellesUtils);

        vurderFagsystemFellesTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(vurderTjeneste));
    }

    @Test
    public void skalTilManuellVurderingHvisBehandlingErAvslåttPgaManglendeDokOgInnsendtDokErEtterFristForInnsending() {
        opprettBehandling(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingResultatType.AVSLÅTT, Avslagsårsak.MANGLENDE_DOKUMENTASJON, VedtakResultatType.AVSLAG, DATO_ETTER_FRISTEN);
        VurderFagsystem vfData = opprettVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL);
        vfData.setDokumentTypeId(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL);
        vfData.setStrukturertSøknad(false);

        //Act
        BehandlendeFagsystem resultat = vurderFagsystemFellesTjeneste.vurderFagsystem(vfData);

        //Assert
        assertThat(resultat.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
        assertThat(resultat.getSaksnummer()).isEmpty();
    }

    @Test
    public void skalReturnereVedtaksløsningMedSaksnummerVurderingHvisEttersendelsePåAngittSak() {
        //Arrange
        Behandling behandling = opprettBehandling(BehandlingType.REVURDERING, BehandlingResultatType.AVSLÅTT, Avslagsårsak.MANGLENDE_DOKUMENTASJON, VedtakResultatType.AVSLAG, DATO_FØR_FRISTEN);
        VurderFagsystem vfData = opprettVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL);
        vfData.setDokumentTypeId(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL);
        vfData.setStrukturertSøknad(false);
        vfData.setSaksnummer(behandling.getFagsak().getSaksnummer());

        //Act
        BehandlendeFagsystem resultat = vurderFagsystemFellesTjeneste.vurderFagsystem(vfData);

        //Assert
        assertThat(resultat.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(resultat.getSaksnummer()).isEqualTo(Optional.of(behandling.getFagsak().getSaksnummer()));
    }

    @Test
    public void skalTilManuellVurderingHvisBehandlingIkkeErAvslåttPgaManglendeDokOgInnsendtDokErFørFristForInnsending() {
        opprettBehandling(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingResultatType.AVSLÅTT, Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING, VedtakResultatType.AVSLAG, DATO_FØR_FRISTEN);
        VurderFagsystem vfData = opprettVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL);
        vfData.setStrukturertSøknad(false);
        vfData.setDokumentTypeId(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL);

        //Act
        BehandlendeFagsystem resultat = vurderFagsystemFellesTjeneste.vurderFagsystem(vfData);

        //Assert
        assertThat(resultat.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
        assertThat(resultat.getSaksnummer()).isEmpty();
    }

    @Test
    public void skalTilManuellVurderingHvisBehandlingIkkeErAvslåttPgaManglendeDokOgInnsendtDokErEtterFristForInnsending() {
        opprettBehandling(BehandlingType.REVURDERING, BehandlingResultatType.AVSLÅTT, Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING, VedtakResultatType.AVSLAG, DATO_ETTER_FRISTEN);
        VurderFagsystem vfData = opprettVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL);
        vfData.setStrukturertSøknad(false);
        vfData.setDokumentTypeId(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL);

        //Act
        BehandlendeFagsystem resultat = vurderFagsystemFellesTjeneste.vurderFagsystem(vfData);

        //Assert
        assertThat(resultat.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
        assertThat(resultat.getSaksnummer()).isEmpty();
    }

    private Behandling opprettBehandling(BehandlingType behandlingType, BehandlingResultatType behandlingResultatType, Avslagsårsak avslagsårsak, VedtakResultatType vedtakResultatType, LocalDate vedtaksdato) {
        var scenarioES = TestScenarioBuilder.builderMedSøknad().medBruker(AKTØR_ID)
            .medFagsakId(1234L)
            .medSaksnummer(new Saksnummer("2345"))
            .medBehandlingType(behandlingType);
        scenarioES.medBehandlingsresultat(Behandlingsresultat.builder()
            .medBehandlingResultatType(behandlingResultatType));
        scenarioES.medBehandlingVedtak()
            .medVedtakstidspunkt(vedtaksdato.atStartOfDay())
            .medVedtakResultatType(vedtakResultatType)
            .medAnsvarligSaksbehandler("fornavn etternavn");

        Behandling behandling = scenarioES.lagre(repositoryProvider);

        behandling.getFagsak().setAvsluttet();
        behandling.avsluttBehandling();

        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);

        return behandling;
    }

    private VurderFagsystem opprettVurderFagsystem(BehandlingTema behandlingTema) {
        VurderFagsystem vfData = new VurderFagsystem();
        vfData.setBehandlingTema(behandlingTema);
        vfData.setAktørId(AKTØR_ID);
        vfData.setStrukturertSøknad(true);
        return vfData;
    }
}

