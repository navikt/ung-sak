package no.nav.foreldrepenger.inngangsvilkaar.medlemskap;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.MedisinskGrunnlagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon.Builder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.impl.InngangsvilkårOversetter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjenesteImpl;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.medlem.MedlemskapDekningType;
import no.nav.k9.kodeverk.medlem.MedlemskapManuellVurderingType;
import no.nav.k9.kodeverk.medlem.MedlemskapType;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.kodeverk.person.SivilstandType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class MedlemskapsvilkårTest {

    public static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private BasisPersonopplysningTjeneste personopplysningTjeneste;

    @Inject
    private SkjæringstidspunktTjenesteImpl skjæringstidspunktTjeneste;

    private InngangsvilkårOversetter oversetter;

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private MedisinskGrunnlagRepository medisinskGrunnlagRepository = new MedisinskGrunnlagRepository(repoRule.getEntityManager());
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private InngangsvilkårMedlemskap vurderMedlemskapsvilkarEngangsstonad;
    private YrkesaktivitetBuilder yrkesaktivitetBuilder;
    private DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT);

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Before
    public void before() throws Exception {
        this.oversetter = new InngangsvilkårOversetter(medisinskGrunnlagRepository,
            personopplysningTjeneste, iayTjeneste, repositoryProvider.getMedlemskapRepository());
        this.vurderMedlemskapsvilkarEngangsstonad = new InngangsvilkårMedlemskap(oversetter);
    }

    /**
     * Input:
     * - bruker manuelt avklart som ikke medlem (FP VK 2.13) = JA
     * <p>
     * Forventet: Ikke oppfylt, avslagsid 1020
     */
    @Test
    public void skal_vurdere_manuell_avklart_ikke_medlem_som_vilkår_ikke_oppfylt() {
        // Arrange
        var scenario = lagTestScenario(MedlemskapDekningType.FTL_2_7_a, Landkoder.NOR, PersonstatusType.BOSA);
        scenario.medMedlemskap().medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.UNNTAK);
        Behandling behandling = lagre(scenario);

        // Act
        VilkårData vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling), periode);

        // Assert
        assertThat(vilkårData.getVilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.getUtfallType()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(vilkårData.getVilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1020);
    }

    /**
     * Input:
     * - bruker registrert som ikke medlem (FP VK 2.13) = JA
     * <p>
     * Forventet: Ikke oppfylt, avslagsid 1020
     */
    @Test
    public void skal_vurdere_maskinelt_avklart_ikke_medlem_som_vilkår_ikke_oppfylt() {
        // Arrange
        var scenario = lagTestScenario(MedlemskapDekningType.FTL_2_6, Landkoder.NOR, PersonstatusType.BOSA);
        Behandling behandling = lagre(scenario);

        // Act
        VilkårData vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling), periode);

        // Assert
        assertThat(vilkårData.getVilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.getUtfallType()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(vilkårData.getVilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1020);
    }

    /**
     * Input:
     * - bruker registrert som ikke medlem (FP VK 2.13) = NEI
     * - bruker avklart som pliktig eller frivillig medlem (FP VK 2.2) = JA
     * <p>
     * Forventet: Oppfylt
     *
     * @throws IOException
     * @throws JsonProcessingException
     */
    @Test
    public void skal_vurdere_avklart_pliktig_medlem_som_vilkår_oppfylt() throws JsonProcessingException, IOException {
        // Arrange
        var scenario = lagTestScenario(MedlemskapDekningType.FTL_2_7_a, Landkoder.NOR, PersonstatusType.BOSA);
        scenario.medMedlemskap().medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.MEDLEM);
        leggTilSøker(scenario, PersonstatusType.BOSA, Region.UDEFINERT, Landkoder.SWE);
        Behandling behandling = lagre(scenario);

        // Act
        VilkårData vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling), periode);

        ObjectMapper om = new ObjectMapper();
        JsonNode jsonNode = om.readTree(vilkårData.getRegelInput());
        String personStatusType = jsonNode.get("personStatusType").asText();

        // Assert
        assertThat(vilkårData.getVilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.getUtfallType()).isEqualTo(Utfall.OPPFYLT);
        assertThat(vilkårData.getRegelInput()).isNotEmpty();
        assertThat(personStatusType).isEqualTo("BOSA");
    }

    /**
     * Input:
     * - bruker registrert som ikke medlem (FP VK 2.13) = NEI
     * - bruker avklart som pliktig eller frivillig medlem (FP VK 2.2) = NEI
     * - bruker registrert som utvandret (FP VK 2.1) = JA
     * - bruker har relevant arbeidsforhold og inntekt som dekker skjæringstidspunkt (FP_VK_2.2.1) = NEI
     * <p>
     * Forventet: Ikke oppfylt, avslagsid 1021
     */
    @Test
    public void skal_vurdere_utvandret_som_vilkår_ikke_oppfylt_ingen_relevant_arbeid_og_inntekt() {
        // Arrange
        var scenario = lagTestScenario(MedlemskapDekningType.UNNTATT, Landkoder.NOR, PersonstatusType.UTVA);
        scenario.medMedlemskap().medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.IKKE_RELEVANT);
        Behandling behandling = lagre(scenario);

        // Act
        VilkårData vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling), periode);

        // Assert
        assertThat(vilkårData.getVilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.getUtfallType()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(vilkårData.getVilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1021);
    }

    /**
     * Input:
     * - bruker registrert som ikke medlem (FP VK 2.13) = NEI
     * - bruker avklart som pliktig eller frivillig medlem (FP VK 2.2) = NEI
     * - bruker registrert som utvandret (FP VK 2.1) = JA
     * - bruker har relevant arbeidsforhold og inntekt som dekker skjæringstidspunkt (FP_VK_2.2.1) = JA
     * <p>
     * Forventet: Ikke oppfylt, avslagsid 1021
     */
    @Test
    public void skal_vurdere_utvandret_som_vilkår_oppfylt_når_relevant_arbeid_og_inntekt_finnes() {
        // Arrange
        var scenario = lagTestScenario(MedlemskapDekningType.UNNTATT, Landkoder.NOR, PersonstatusType.UTVA);
        scenario.medMedlemskap().medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.IKKE_RELEVANT);
        Behandling behandling = lagre(scenario);

        opprettArbeidOgInntektForBehandling(behandling, SKJÆRINGSTIDSPUNKT.minusMonths(5), SKJÆRINGSTIDSPUNKT.plusDays(2));
        // Act
        VilkårData vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling), periode);

        // Assert
        assertThat(vilkårData.getVilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.getUtfallType()).isEqualTo(Utfall.OPPFYLT);
    }

    /**
     * Input:
     * - bruker registrert som ikke medlem (FP VK 2.13) = NEI
     * - bruker avklart som pliktig eller frivillig medlem (FP VK 2.2) = NEI
     * - bruker registrert som utvandret (FP VK 2.1) = NEI
     * - bruker avklart som ikke bosatt = JA
     * - bruker har relevant arbeidsforhold og inntekt som dekker skjæringstidspunkt (FP_VK_2.2.1) = JA
     * <p>
     * Forventet: Ikke oppfylt, avslagsid 1025
     */
    @Test
    public void skal_vurdere_avklart_ikke_bosatt_som_vilkår_når_bruker_har_relevant_arbeid_og_inntekt() {
        // Arrange
        Landkoder landkode = Landkoder.fraKode("POL");
        var scenario = lagTestScenario(MedlemskapDekningType.UNNTATT, landkode, PersonstatusType.BOSA);
        scenario.medMedlemskap().medBosattVurdering(false).medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.IKKE_RELEVANT);
        Behandling behandling = lagre(scenario);
        opprettArbeidOgInntektForBehandling(behandling, SKJÆRINGSTIDSPUNKT.minusMonths(5), SKJÆRINGSTIDSPUNKT.plusDays(2));

        // Act
        VilkårData vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling), periode);

        // Assert
        assertThat(vilkårData.getVilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.getUtfallType()).isEqualTo(Utfall.OPPFYLT);
    }

    /**
     * Input:
     * - bruker registrert som ikke medlem (FP VK 2.13) = NEI
     * - bruker avklart som pliktig eller frivillig medlem (FP VK 2.2) = NEI
     * - bruker registrert som utvandret (FP VK 2.1) = NEI
     * - bruker avklart som ikke bosatt = JA
     * - bruker har relevant arbeidsforhold og inntekt som dekker skjæringstidspunkt (FP_VK_2.2.1) = NEI
     * <p>
     * Forventet: Ikke oppfylt, avslagsid 1025
     */
    @Test
    public void skal_vurdere_avklart_ikke_bosatt_som_vilkår_når_bruker_har_ingen_relevant_arbeid_og_inntekt() {
        // Arrange
        Landkoder landkode = Landkoder.fraKode("POL");
        var scenario = lagTestScenario(MedlemskapDekningType.UNNTATT, landkode, PersonstatusType.BOSA);
        scenario.medMedlemskap().medBosattVurdering(false).medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.IKKE_RELEVANT);
        Behandling behandling = lagre(scenario);

        // Act
        VilkårData vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling), periode);

        // Assert
        assertThat(vilkårData.getVilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.getUtfallType()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(vilkårData.getVilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1025);
    }

    /**
     * Input:
     * - bruker registrert som ikke medlem (FP VK 2.13) = NEI
     * - bruker avklart som pliktig eller frivillig medlem (FP VK 2.2) = NEI
     * - bruker registrert som utvandret (FP VK 2.1) = NEI
     * - bruker avklart som ikke bosatt = NEI
     * - bruker oppgir opphold i norge (FP VK 2.3) = JA
     * - bruker oppgir opphold norge minst 12 mnd (FP VK 2.5) = JA
     * - bruker norsk/nordisk statsborger i TPS (FP VK 2.11) = JA
     * <p>
     * Forventet: oppfylt
     */
    @Test
    public void skal_vurdere_norsk_nordisk_statsborger_som_vilkår_oppfylt() {
        // Arrange
        var scenario = lagTestScenario(MedlemskapDekningType.UDEFINERT, Landkoder.NOR, PersonstatusType.BOSA);
        leggTilSøker(scenario, PersonstatusType.BOSA, Region.UDEFINERT, Landkoder.NOR);
        scenario.medMedlemskap().medBosattVurdering(true);
        Behandling behandling = lagre(scenario);

        // Act
        VilkårData vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling), periode);

        // Assert
        assertThat(vilkårData.getVilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.getUtfallType()).isEqualTo(Utfall.OPPFYLT);
    }

    /**
     * Input:
     * - bruker registrert som ikke medlem (FP VK 2.13) = NEI
     * - bruker avklart som pliktig eller frivillig medlem (FP VK 2.2) = NEI
     * - bruker registrert som utvandret (FP VK 2.1) = NEI
     * - bruker avklart som ikke bosatt = NEI
     * - bruker oppgir opphold i norge (FP VK 2.3) = JA
     * - bruker oppgir opphold norge minst 12 mnd (FP VK 2.5) = JA
     * - bruker norsk/nordisk statsborger i TPS (FP VK 2.11) = NEI
     * - bruker EU/EØS statsborger = JA
     * - bruker har avklart oppholdsrett (FP VK 2.12) = JA
     * <p>
     * Forventet: oppfylt
     */
    @Test
    public void skal_vurdere_eøs_statsborger_med_oppholdsrett_som_vilkår_oppfylt() {
        // Arrange
        var scenario = lagTestScenario(Landkoder.NOR, PersonstatusType.BOSA);
        leggTilSøker(scenario, PersonstatusType.BOSA, Region.EOS, Landkoder.SWE);
        scenario.medMedlemskap().medBosattVurdering(true).medOppholdsrettVurdering(true);
        Behandling behandling = lagre(scenario);

        // Act
        VilkårData vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling), periode);

        // Assert
        assertThat(vilkårData.getVilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.getUtfallType()).isEqualTo(Utfall.OPPFYLT);
    }

    /**
     * Input:
     * - bruker registrert som ikke medlem (FP VK 2.13) = NEI
     * - bruker avklart som pliktig eller frivillig medlem (FP VK 2.2) = NEI
     * - bruker registrert som utvandret (FP VK 2.1) = NEI
     * - bruker avklart som ikke bosatt = NEI
     * - bruker oppgir opphold i norge (FP VK 2.3) = JA
     * - bruker oppgir opphold norge minst 12 mnd (FP VK 2.5) = JA
     * - bruker norsk/nordisk statsborger i TPS (FP VK 2.11) = NEI
     * - bruker EU/EØS statsborger = JA
     * - bruker har avklart oppholdsrett (FP VK 2.12) = NEI
     * <p>
     * Forventet: Ikke oppfylt, avslagsid 1024
     */
    @Test
    public void skal_vurdere_eøs_statsborger_uten_oppholdsrett_som_vilkår_ikke_oppfylt() {
        // Arrange
        Landkoder landkodeEOS = Landkoder.fraKode("POL");
        var scenario = lagTestScenario(landkodeEOS, PersonstatusType.BOSA);
        scenario.medMedlemskap().medBosattVurdering(true).medOppholdsrettVurdering(false);
        Behandling behandling = lagre(scenario);

        // Act
        VilkårData vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling), periode);

        // Assert
        assertThat(vilkårData.getVilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.getUtfallType()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(vilkårData.getVilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1024);
    }

    /**
     * Input:
     * - bruker registrert som ikke medlem (FP VK 2.13) = NEI
     * - bruker avklart som pliktig eller frivillig medlem (FP VK 2.2) = NEI
     * - bruker registrert som utvandret (FP VK 2.1) = NEI
     * - bruker avklart som ikke bosatt = NEI
     * - bruker oppgir opphold i norge (FP VK 2.3) = JA
     * - bruker oppgir opphold norge minst 12 mnd (FP VK 2.5) = JA
     * - bruker norsk/nordisk statsborger i TPS (FP VK 2.11) = NEI
     * - bruker EU/EØS statsborger = NEI
     * - bruker har avklart lovlig opphold (FP VK 2.12) = NEI
     * <p>
     * Forventet: Ikke oppfylt, avslagsid 1023
     */
    @Test
    public void skal_vurdere_annen_statsborger_uten_lovlig_opphold_som_vilkår_ikke_oppfylt() {
        // Arrange
        Landkoder land = Landkoder.fraKode("ARG");
        var scenario = lagTestScenario(MedlemskapDekningType.UNNTATT, land, PersonstatusType.BOSA);
        scenario.medMedlemskap().medBosattVurdering(true).medLovligOppholdVurdering(false)
            .medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.IKKE_RELEVANT);
        Behandling behandling = lagre(scenario);

        // Act
        VilkårData vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling), periode);

        // Assert
        assertThat(vilkårData.getVilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.getUtfallType()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(vilkårData.getVilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1023);
    }

    /**
     * Input:
     * - bruker registrert som ikke medlem (FP VK 2.13) = NEI
     * - bruker avklart som pliktig eller frivillig medlem (FP VK 2.2) = NEI
     * - bruker registrert som utvandret (FP VK 2.1) = NEI
     * - bruker avklart som ikke bosatt = NEI
     * - bruker oppgir opphold i norge (FP VK 2.3) = JA
     * - bruker oppgir opphold norge minst 12 mnd (FP VK 2.5) = JA
     * - bruker norsk/nordisk statsborger i TPS (FP VK 2.11) = NEI
     * - bruker EU/EØS statsborger = NEI
     * - bruker har avklart lovlig opphold (FP VK 2.12) = JA
     * <p>
     * Forventet: oppfylt
     */
    @Test
    public void skal_vurdere_annen_statsborger_med_lovlig_opphold_som_vilkår_oppfylt() {
        // Arrange
        var scenario = lagTestScenario(Landkoder.NOR, PersonstatusType.BOSA);
        leggTilSøker(scenario, PersonstatusType.BOSA, Region.UDEFINERT, Landkoder.USA);
        scenario.medMedlemskap().medBosattVurdering(true).medLovligOppholdVurdering(true);
        Behandling behandling = lagre(scenario);

        // Act
        VilkårData vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling), periode);

        // Assert
        assertThat(vilkårData.getVilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.getUtfallType()).isEqualTo(Utfall.OPPFYLT);
    }

    /**
     * - bruker har relevant arbeidsforhold og inntekt som dekker skjæringstidspunkt (FP_VK_2.2.1) = NEI
     */
    @Test
    public void skal_få_medlemskapsvilkåret_satt_til_ikke_oppfylt_når_saksbehandler_setter_personstatus_til_utvandert_og_ingen_relevant_arbeid_og_inntekt() {
        // Arrange

        var scenario = lagTestScenario(MedlemskapDekningType.FTL_2_9_1_c, Landkoder.NOR, PersonstatusType.UREG);
        scenario.medMedlemskap().medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.IKKE_RELEVANT);

        leggTilSøker(scenario, PersonstatusType.UREG, Region.NORDEN, Landkoder.SWE);

        Behandling behandling = lagre(scenario);

        Long behandlingId = behandling.getId();
        final PersonInformasjonBuilder personInformasjonBuilder = repositoryProvider.getPersonopplysningRepository().opprettBuilderForOverstyring(behandlingId);
        LocalDate utvandretDato = LocalDate.now().minusYears(10);
        personInformasjonBuilder.leggTil(personInformasjonBuilder.getPersonstatusBuilder(behandling.getAktørId(), DatoIntervallEntitet.fraOgMed(utvandretDato))
            .medPersonstatus(PersonstatusType.UTVA));
        repositoryProvider.getPersonopplysningRepository().lagre(behandlingId, personInformasjonBuilder);

        // Act
        VilkårData vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling), periode);

        // Assert
        assertThat(vilkårData.getVilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.getUtfallType()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(vilkårData.getVilkårUtfallMerknad()).isEqualTo(VilkårUtfallMerknad.VM_1021);
    }

    /**
     * - bruker har relevant arbeidsforhold og inntekt som dekker skjæringstidspunkt (FP_VK_2.2.1) = JA
     */
    @Test
    public void skal_få_medlemskapsvilkåret_satt_til_ikke_oppfylt_når_saksbehandler_setter_personstatus_til_utvandert_og_relevant_arbeid_og_inntekt_finnes() {
        // Arrange

        var scenario = lagTestScenario(MedlemskapDekningType.FTL_2_9_1_c, Landkoder.NOR, PersonstatusType.UREG);
        scenario.medMedlemskap().medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.IKKE_RELEVANT);

        leggTilSøker(scenario, PersonstatusType.UREG, Region.NORDEN, Landkoder.SWE);

        Behandling behandling = lagre(scenario);

        opprettArbeidOgInntektForBehandling(behandling, SKJÆRINGSTIDSPUNKT.minusMonths(5), SKJÆRINGSTIDSPUNKT.plusDays(2));

        Long behandlingId = behandling.getId();
        final PersonInformasjonBuilder personInformasjonBuilder = repositoryProvider.getPersonopplysningRepository().opprettBuilderForOverstyring(behandlingId);
        LocalDate utvandretDato = LocalDate.now().minusYears(10);
        personInformasjonBuilder.leggTil(personInformasjonBuilder.getPersonstatusBuilder(behandling.getAktørId(), DatoIntervallEntitet.fraOgMed(utvandretDato))
            .medPersonstatus(PersonstatusType.UTVA));
        repositoryProvider.getPersonopplysningRepository().lagre(behandlingId, personInformasjonBuilder);

        // Act
        VilkårData vilkårData = vurderMedlemskapsvilkarEngangsstonad.vurderVilkår(lagRef(behandling), periode);

        // Assert
        assertThat(vilkårData.getVilkårType()).isEqualTo(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårData.getUtfallType()).isEqualTo(Utfall.OPPFYLT);
    }

    /**
     * Lager minimalt testscenario med en medlemsperiode som indikerer om søker er medlem eller ikke.
     */
    private AbstractTestScenario<?> lagTestScenario(MedlemskapDekningType dekningType, Landkoder statsborgerskap,
                                                    PersonstatusType personstatusType) {
        return lagTestScenario(dekningType, statsborgerskap, personstatusType, Region.NORDEN);
    }

    private AbstractTestScenario<?> lagTestScenario(MedlemskapDekningType dekningType, Landkoder statsborgerskap,
                                                    PersonstatusType personstatusType, Region region) {
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medSøknad();
        if (dekningType != null) {
            scenario.leggTilMedlemskapPeriode(new MedlemskapPerioderBuilder()
                .medDekningType(dekningType)
                .medMedlemskapType(MedlemskapType.ENDELIG)
                .medPeriode(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))
                .build());
        }

        Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();

        PersonInformasjon søker = builderForRegisteropplysninger
            .medPersonas()
            .kvinne(søkerAktørId, SivilstandType.GIFT, region)
            .personstatus(personstatusType)
            .statsborgerskap(statsborgerskap)
            .build();
        scenario.medRegisterOpplysninger(søker);
        return scenario;
    }

    private void leggTilSøker(AbstractTestScenario<?> scenario, PersonstatusType personstatus, Region region, Landkoder statsborgerskapLand) {
        Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        AktørId barnAktørId = AktørId.dummy();
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();

        PersonInformasjon fødtBarn = builderForRegisteropplysninger
            .medPersonas()
            .barn(barnAktørId, LocalDate.now().plusDays(7))
            .relasjonTil(søkerAktørId, RelasjonsRolleType.MORA, null)
            .build();

        PersonInformasjon søker = builderForRegisteropplysninger
            .medPersonas()
            .kvinne(søkerAktørId, SivilstandType.GIFT, region)
            .statsborgerskap(statsborgerskapLand)
            .personstatus(personstatus)
            .relasjonTil(barnAktørId, RelasjonsRolleType.BARN, null)
            .build();
        scenario.medRegisterOpplysninger(søker);
        scenario.medRegisterOpplysninger(fødtBarn);
    }

    private AbstractTestScenario<?> lagTestScenario(Landkoder statsborgerskap, PersonstatusType personstatusType) {
        return lagTestScenario(null, statsborgerskap, personstatusType);
    }

    private void opprettArbeidOgInntektForBehandling(Behandling behandling, LocalDate fom, LocalDate tom) {

        String virksomhetOrgnr = "42";
        var aktørId = behandling.getAktørId();
        var aggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);

        lagAktørArbeid(aggregatBuilder, aktørId, virksomhetOrgnr, fom, tom, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, Optional.empty());
        for (LocalDate dt = fom; dt.isBefore(tom); dt = dt.plusMonths(1)) {
            lagInntekt(aggregatBuilder, aktørId, virksomhetOrgnr, dt, dt.plusMonths(1));
        }

        iayTjeneste.lagreIayAggregat(behandling.getId(), aggregatBuilder);
    }

    private AktørArbeid lagAktørArbeid(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder, AktørId aktørId, String virksomhetOrgnr,
                                       LocalDate fom, LocalDate tom, ArbeidType arbeidType, Optional<InternArbeidsforholdRef> arbeidsforholdRef) {
        var aktørArbeidBuilder = inntektArbeidYtelseAggregatBuilder
            .getAktørArbeidBuilder(aktørId);

        Opptjeningsnøkkel opptjeningsnøkkel;
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(virksomhetOrgnr);
        if (arbeidsforholdRef.isPresent()) {
            opptjeningsnøkkel = new Opptjeningsnøkkel(arbeidsforholdRef.get(), arbeidsgiver);
        } else {
            opptjeningsnøkkel = Opptjeningsnøkkel.forOrgnummer(virksomhetOrgnr);
        }

        yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(opptjeningsnøkkel, arbeidType);
        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();

        var aktivitetsAvtale = aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));

        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(aktivitetsAvtale)
            .medArbeidType(arbeidType)
            .medArbeidsgiver(arbeidsgiver);

        yrkesaktivitetBuilder.medArbeidsforholdId(arbeidsforholdRef.orElse(null));

        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørArbeid(aktørArbeidBuilder);
        return aktørArbeidBuilder.build();
    }

    private void lagInntekt(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder, AktørId aktørId, String virksomhetOrgnr,
                            LocalDate fom, LocalDate tom) {
        Opptjeningsnøkkel opptjeningsnøkkel = Opptjeningsnøkkel.forOrgnummer(virksomhetOrgnr);

        var aktørInntektBuilder = inntektArbeidYtelseAggregatBuilder.getAktørInntektBuilder(aktørId);

        Stream.of(InntektsKilde.INNTEKT_BEREGNING, InntektsKilde.INNTEKT_SAMMENLIGNING, InntektsKilde.INNTEKT_OPPTJENING).forEach(kilde -> {
            InntektBuilder inntektBuilder = aktørInntektBuilder.getInntektBuilder(kilde, opptjeningsnøkkel);
            InntektspostBuilder inntektspost = InntektspostBuilder.ny()
                .medBeløp(BigDecimal.valueOf(35000))
                .medPeriode(fom, tom)
                .medInntektspostType(InntektspostType.LØNN);
            inntektBuilder.leggTilInntektspost(inntektspost).medArbeidsgiver(yrkesaktivitetBuilder.build().getArbeidsgiver());
            aktørInntektBuilder.leggTilInntekt(inntektBuilder);
            inntektArbeidYtelseAggregatBuilder.leggTilAktørInntekt(aktørInntektBuilder);
        });
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
    }

}
