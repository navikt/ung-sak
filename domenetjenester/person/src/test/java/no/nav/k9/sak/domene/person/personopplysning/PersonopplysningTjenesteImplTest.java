package no.nav.k9.sak.domene.person.personopplysning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.person.SivilstandType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.test.util.behandling.personopplysning.PersonInformasjon;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

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
    public void skal_hente_gjeldende_personinformasjon_på_tidspunkt() {
        LocalDate tidspunkt = LocalDate.now();

        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();

        PersonInformasjon personInformasjon = scenario
            .opprettBuilderForRegisteropplysninger()
            .medPersonas()
            .kvinne(søkerAktørId, SivilstandType.SAMBOER)
            .statsborgerskap(Landkoder.NOR)
            .personstatus(PersonstatusType.BOSA)
            .build();

        scenario.medRegisterOpplysninger(personInformasjon);

        Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        PersonopplysningerAggregat personopplysningerAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunkt(behandling.getId(),
            behandling.getAktørId(), tidspunkt);
        // Assert
        assertThat(personopplysningerAggregat.getPersonstatuserFor(behandling.getAktørId())).isNotEmpty();
    }

}
