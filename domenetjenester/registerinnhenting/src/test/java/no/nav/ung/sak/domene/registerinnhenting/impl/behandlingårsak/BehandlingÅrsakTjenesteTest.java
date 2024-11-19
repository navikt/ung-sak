package no.nav.ung.sak.domene.registerinnhenting.impl.behandlingårsak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.time.LocalDate;
import java.util.Set;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandling.Skjæringstidspunkt;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.diff.DiffResult;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.ung.sak.domene.registerinnhenting.BehandlingÅrsakTjeneste;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class BehandlingÅrsakTjenesteTest {

    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider ;

    private BehandlingÅrsakTjeneste tjeneste;

    private Behandling behandling;

    @Inject
    @Any
    private Instance<BehandlingÅrsakUtleder> utledere;

    @Mock
    private DiffResult diffResult;

    private Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();

    @BeforeEach
    public void setup() {

        repositoryProvider = new BehandlingRepositoryProvider(entityManager);

        initMocks(this);

        tjeneste = new BehandlingÅrsakTjeneste(utledere);
        var scenario = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        behandling = scenario.lagre(repositoryProvider);
    }

    @Test
    public void test_skal_ikke_returnere_behandlingsårsaker_hvis_ikke_endringer() {
        when(diffResult.isEmpty()).thenReturn(true); // Indikerer at det ikke finnes diff

        EndringsresultatDiff endringsresultat = EndringsresultatDiff.opprett();
        endringsresultat.leggTilSporetEndring(EndringsresultatDiff.medDiff(PersonInformasjonEntitet.class, 1L, 1L), () -> diffResult);
        endringsresultat.leggTilSporetEndring(EndringsresultatDiff.medDiff(InntektArbeidYtelseGrunnlag.class, 1L, 1L), () -> diffResult);

        // Act/Assert
        assertThat(tjeneste.utledBehandlingÅrsakerBasertPåDiff(lagRef(behandling), endringsresultat)).isEmpty();
    }

    @Test
    public void test_behandlingsårsaker_når_endring_dødsdato_søker() {
        final LocalDate dødsdato = LocalDate.now().minusDays(10);
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(null);
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(dødsdato);

        EndringsresultatDiff endringsresultat = EndringsresultatDiff.opprett();
        when(diffResult.isEmpty()).thenReturn(false); // Indikerer at det finnes diff
        endringsresultat.leggTilSporetEndring(EndringsresultatDiff.medDiff(PersonInformasjonEntitet.class, personopplysningGrunnlag1.getId(), personopplysningGrunnlag2.getId()), () -> diffResult);

        Set<BehandlingÅrsakType> behandlingÅrsaker = tjeneste.utledBehandlingÅrsakerBasertPåDiff(lagRef(behandling), endringsresultat);
        assertThat(behandlingÅrsaker).hasSize(1);
        assertThat(behandlingÅrsaker).as("Forventer behandlingsårsak").contains(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_DØD);
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling, skjæringstidspunkt);
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlag(LocalDate dødsdato) {
        PersonopplysningRepository personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        Long behandlingId = behandling.getId();
        var builder = personopplysningRepository.opprettBuilderFraEksisterende(behandlingId, PersonopplysningVersjonType.REGISTRERT);
        var personopplysningBuilder = builder.getPersonopplysningBuilder(behandling.getAktørId());
        personopplysningBuilder.medDødsdato(dødsdato);
        builder.leggTil(personopplysningBuilder);
        personopplysningRepository.lagre(behandlingId, builder);
        return personopplysningRepository.hentPersonopplysninger(behandlingId);
    }
}
