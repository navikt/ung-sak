package no.nav.ung.sak.domene.person.personopplysning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.ung.kodeverk.person.RelasjonsRolleType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.personopplysning.PersonInformasjon;
import no.nav.ung.sak.typer.AktørId;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PersonopplysningTjenesteImplTest {

    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider;
    private TestScenarioBuilder scenario;

    private PersonopplysningTjeneste personopplysningTjeneste;

    @BeforeEach
    public void before() {

        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        scenario = TestScenarioBuilder.builderMedSøknad();

        personopplysningTjeneste = new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository());
    }

    @Test
    public void skal_hente_gjeldende_personinformasjon() {


        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();

        PersonInformasjon personInformasjon = scenario
            .opprettBuilderForRegisteropplysninger()
            .medPersonas()
            .kvinne(søkerAktørId)
            .relasjonTil(AktørId.dummy(), RelasjonsRolleType.BARN)
            .build();

        scenario.medRegisterOpplysninger(personInformasjon);

        Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        PersonopplysningerAggregat personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(BehandlingReferanse.fra(behandling));
        // Assert
        assertThat(personopplysningerAggregat.getRelasjoner()).isNotEmpty();
    }

}
