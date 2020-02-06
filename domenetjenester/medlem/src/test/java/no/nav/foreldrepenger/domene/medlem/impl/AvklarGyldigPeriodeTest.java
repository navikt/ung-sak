

package no.nav.foreldrepenger.domene.medlem.impl;

import static no.nav.foreldrepenger.domene.medlem.impl.MedlemResultat.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.medlem.MedlemskapPerioderTjeneste;
import no.nav.k9.kodeverk.medlem.MedlemskapDekningType;
import no.nav.k9.kodeverk.medlem.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class AvklarGyldigPeriodeTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider provider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    @Inject
    private MedlemskapPerioderTjeneste medlemskapPerioderTjeneste;

    @Inject
    private MedlemskapRepository medlemskapRepository;
    
    private AvklarGyldigPeriode avklarGyldigPeriode;

    @Before
    public void setUp() {
        this.avklarGyldigPeriode = new AvklarGyldigPeriode(medlemskapRepository, medlemskapPerioderTjeneste);
    }

    @Test
    public void skal_ikke_opprette_Aksjonspunkt_ved_gyldig_periode() {
        // Arrange
        LocalDate fødselsdato = LocalDate.now();
        MedlemskapPerioderEntitet gyldigPeriodeUnderFødsel = new MedlemskapPerioderBuilder()
            .medDekningType(MedlemskapDekningType.FTL_2_7_a) // hjemlet i bokstav a
            .medMedlemskapType(MedlemskapType.ENDELIG) // gyldig
            .medPeriode(fødselsdato, fødselsdato)
            .build();
        Set<MedlemskapPerioderEntitet> medlemskapPerioder = new HashSet<>();
        medlemskapPerioder.add(gyldigPeriodeUnderFødsel);
        var scenario = TestScenarioBuilder.builderMedSøknad();
        medlemskapPerioder.forEach(scenario::leggTilMedlemskapPeriode);
        Behandling behandling = scenario.lagre(provider);

        // Act
        Optional<MedlemResultat> medlemResultat = avklarGyldigPeriode.utled(behandling.getId(), fødselsdato);

        // Assert
        assertThat(medlemResultat).isEmpty();
    }

    @Test
    public void skalIkkeOppretteAksjonspunktVedIngenTreffMedl() {
        // Arrange
        LocalDate fødselsdato = LocalDate.now();
        var scenario = TestScenarioBuilder.builderMedSøknad();
        Behandling behandling = scenario.lagre(provider);

        // Act
        Optional<MedlemResultat> medlemResultat = avklarGyldigPeriode.utled(behandling.getId(), fødselsdato);

        // Assert
        assertThat(medlemResultat).isEmpty();
    }

    @Test
    public void skalIkkeOppretteAksjonspunktVedIngenUavklartPeriode() {
        // Arrange
        LocalDate fødselsdato = LocalDate.now();
        MedlemskapPerioderEntitet lukketPeriodeFørFødselsdato = new MedlemskapPerioderBuilder()
            .medDekningType(MedlemskapDekningType.FTL_2_7_b) // ikke hjemlet i bokstav a eller c
            .medMedlemskapType(MedlemskapType.ENDELIG)
            .medPeriode(fødselsdato, fødselsdato)
            .build();
        Set<MedlemskapPerioderEntitet> medlemskapPerioder = new HashSet<>();
        medlemskapPerioder.add(lukketPeriodeFørFødselsdato);
        var scenario = TestScenarioBuilder.builderMedSøknad();
        medlemskapPerioder.forEach(scenario::leggTilMedlemskapPeriode);
        Behandling behandling = scenario.lagre(provider);

        // Act
        Optional<MedlemResultat> medlemResultat = avklarGyldigPeriode.utled(behandling.getId(), fødselsdato);

        // Assert
        assertThat(medlemResultat).isEmpty();
    }

    @Test
    public void skalOppretteAksjonspunktVedUavklartPeriode() {
        // Arrange
        LocalDate fødselsdato = LocalDate.now();
        MedlemskapPerioderEntitet medlemskapPeriodeUnderAvklaring = new MedlemskapPerioderBuilder()
            .medDekningType(MedlemskapDekningType.FTL_2_7_a) // hjemlet i bokstav a
            .medMedlemskapType(MedlemskapType.UNDER_AVKLARING)
            .medPeriode(fødselsdato, fødselsdato)
            .build();
        Set<MedlemskapPerioderEntitet> medlemskapPerioder = new HashSet<>();
        medlemskapPerioder.add(medlemskapPeriodeUnderAvklaring);
        var scenario = TestScenarioBuilder.builderMedSøknad();
        medlemskapPerioder.forEach(scenario::leggTilMedlemskapPeriode);
        Behandling behandling = scenario.lagre(provider);

        // Act
        Optional<MedlemResultat> medlemResultat = avklarGyldigPeriode.utled(behandling.getId(), fødselsdato);

        // Assert
        assertThat(medlemResultat).contains(AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE);
    }

    @Test
    public void skalOppretteAksjonspunktVedÅpenPeriode() {
        // Arrange
        LocalDate fødselsdato = LocalDate.now();
        MedlemskapPerioderEntitet åpenPeriode = new MedlemskapPerioderBuilder()
            .medDekningType(MedlemskapDekningType.FTL_2_7_a) // hjemlet i bokstav a
            .medMedlemskapType(MedlemskapType.FORELOPIG)
            .medPeriode(fødselsdato, null) // åpen periode
            .build();
        Set<MedlemskapPerioderEntitet> medlemskapPerioder = new HashSet<>();
        medlemskapPerioder.add(åpenPeriode);
        var scenario = TestScenarioBuilder.builderMedSøknad();
        medlemskapPerioder.forEach(scenario::leggTilMedlemskapPeriode);
        Behandling behandling = scenario.lagre(provider);

        // Act
        Optional<MedlemResultat> medlemResultat = avklarGyldigPeriode.utled(behandling.getId(), fødselsdato);

        // Assert
        assertThat(medlemResultat).contains(AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE);
    }
}
