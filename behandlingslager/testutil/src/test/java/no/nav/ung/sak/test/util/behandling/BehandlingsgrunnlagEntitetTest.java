package no.nav.ung.sak.test.util.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import no.nav.ung.kodeverk.person.NavBrukerKjønn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.db.util.Repository;
import no.nav.ung.sak.test.util.fagsak.FagsakBuilder;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class BehandlingsgrunnlagEntitetTest {

    @Inject
    private EntityManager entityManager;

    private Repository repository;
    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private PersonopplysningRepository personopplysningRepository;
    private Fagsak fagsak;
    private SøknadRepository søknadRepository;

    @BeforeEach
    public void setup() {
        repository = new Repository(entityManager);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        personopplysningRepository = repositoryProvider.getPersonopplysningRepository();

        fagsak = FagsakBuilder.nyFagsak(FagsakYtelseType.OMSORGSPENGER).build();
        søknadRepository = repositoryProvider.getSøknadRepository();

        repository.lagre(fagsak);
        repository.flush();
    }

    @Test
    public void skal_opprette_nytt_behandlingsgrunnlag_med_søknad() {
        // Arrange
        LocalDate søknadsdato = LocalDate.now();

        Behandling.Builder behandlingBuilder = Behandling.forFørstegangssøknad(fagsak);
        Behandling behandling = behandlingBuilder.build();

        lagreBehandling(behandling);

        SøknadEntitet.Builder søknadBuilder = new SøknadEntitet.Builder()
            .medJournalpostId(new JournalpostId(1L))
            .medStartdato(søknadsdato);
        søknadRepository.lagreOgFlush(behandling, søknadBuilder.build());
        repository.flush();

        // Assert
        SøknadEntitet søknad = søknadRepository.hentSøknad(behandling);
        assertThat(søknad).isNotNull();
        assertThat(søknad.getStartdato()).isEqualTo(søknadsdato);
    }

    @Test
    public void skal_kunne_lagre_personinfo_på_bruker() {
        LocalDate dødsdatoForelder1 = LocalDate.now();

        AktørId aktørId = fagsak.getAktørId();

        Behandling.Builder behandlingBuilder = Behandling.forFørstegangssøknad(fagsak);
        Behandling behandling = behandlingBuilder.build();
        lagreBehandling(behandling);

        Long behandlingId = behandling.getId();
        PersonInformasjonBuilder informasjonBuilder = new PersonInformasjonBuilder(PersonopplysningVersjonType.REGISTRERT);
        LocalDate fødselsdato = dødsdatoForelder1.minusYears(40);
        informasjonBuilder.leggTil(
                informasjonBuilder.getPersonopplysningBuilder(aktørId)
                    .medNavn("Navn")
                    .medKjønn(NavBrukerKjønn.KVINNE)
                    .medFødselsdato(fødselsdato)
                    .medDødsdato(dødsdatoForelder1));

        personopplysningRepository.lagre(behandlingId, informasjonBuilder);
        repository.flushAndClear();

        // Assert 1: Forelder 1 er lagret
        @SuppressWarnings("unused")
        Behandling opphentet = repository.hent(Behandling.class, behandlingId);
        PersonopplysningGrunnlagEntitet personopplysninger = hentSøkerPersonopplysninger(behandlingId);
        assertThat(personopplysninger).isNotNull();

        PersonInformasjonEntitet personInformasjon = personopplysninger.getGjeldendeVersjon();
        assertThat(personInformasjon.getPersonopplysninger()).hasSize(1);
        assertThat(personInformasjon.getRelasjoner()).isEmpty();

        // Assert på de øvrige attributter
        PersonopplysningEntitet personopplysning = personInformasjon.getPersonopplysninger().get(0);
        assertThat(personopplysning.getDødsdato()).isEqualTo(dødsdatoForelder1);
        assertThat(personopplysning.getNavn()).isEqualTo("Navn");
    }

    private void lagreBehandling(Behandling behandling) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
    }

    private PersonopplysningGrunnlagEntitet hentSøkerPersonopplysninger(Long behandlingId) {
        return personopplysningRepository.hentPersonopplysninger(behandlingId);
    }
}
