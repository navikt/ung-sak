package no.nav.k9.sak.domene.person.personopplysning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.person.SivilstandType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.test.util.behandling.personopplysning.PersonInformasjon;
import no.nav.k9.sak.typer.AktørId;

public class PersonopplysningTjenesteImplTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    private PersonopplysningTjeneste personopplysningTjeneste;
    private TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad();

    @Before
    public void before() {
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
