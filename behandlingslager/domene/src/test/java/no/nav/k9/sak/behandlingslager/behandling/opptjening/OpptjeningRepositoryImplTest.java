package no.nav.k9.sak.behandlingslager.behandling.opptjening;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class OpptjeningRepositoryImplTest {

    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider;
    private OpptjeningRepository opptjeningRepository;
    private BasicBehandlingBuilder behandlingBuilder;

    @BeforeEach
    public void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        opptjeningRepository = repositoryProvider.getOpptjeningRepository();
        behandlingBuilder = new BasicBehandlingBuilder(entityManager);
    }

    @Test
    public void skal_lagre_opptjeningsperiode() throws Exception {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        Behandling behandling = opprettBehandling();

        // Act
        Opptjening opptjeningsperiode = opptjeningRepository.lagreOpptjeningsperiode(behandling, today, tomorrow, false);

        // Assert
        assertThat(opptjeningsperiode.getFom()).isEqualTo(today);
        assertThat(opptjeningsperiode.getTom()).isEqualTo(tomorrow);

        assertThat(opptjeningsperiode.getOpptjeningAktivitet()).isEmpty();
        assertThat(opptjeningsperiode.getOpptjentPeriode()).isNull();

        // Act
        var funnet = opptjeningRepository.finnOpptjening(behandling.getId()).flatMap(it -> it.finnOpptjening(DatoIntervallEntitet.fraOgMedTilOgMed(today, tomorrow))).orElseThrow();

        // Assert
        assertThat(funnet).isEqualTo(opptjeningsperiode);
    }

    @Test
    public void kopierGrunnlagFraEksisterendeBehandling() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        Behandling behandling = opprettBehandling();
        Behandling revurdering = opprettBehandling();
        Opptjening opptjeningsperiode = opptjeningRepository.lagreOpptjeningsperiode(behandling, today, tomorrow, false);

        // Act
        opptjeningRepository.kopierGrunnlagFraEksisterendeBehandling(behandling.getId(), revurdering);
        var funnet = opptjeningRepository.finnOpptjening(revurdering.getId()).flatMap(it -> it.finnOpptjening(DatoIntervallEntitet.fraOgMedTilOgMed(today, tomorrow))).orElseThrow();

        // Assert
        assertThat(funnet).isEqualTo(opptjeningsperiode);
    }

    @Test
    public void deaktiverOpptjening() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        Behandling behandling = opprettBehandling();

        // Act
        opptjeningRepository.lagreOpptjeningsperiode(behandling, today, tomorrow, false);
        opptjeningRepository.deaktiverOpptjeningForPeriode(behandling, DatoIntervallEntitet.fraOgMed(tomorrow.plusDays(1)));

        // Assert
        var funnetOpt = opptjeningRepository.finnOpptjening(behandling.getId());
        assertThat(funnetOpt).isPresent().hasValueSatisfying(it -> assertThat(it.getOpptjeningPerioder()).hasSize(0));
    }

    @Test
    public void lagreOpptjeningResultat() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        Behandling behandling = opprettBehandling();
        List<OpptjeningAktivitet> aktiviteter = new ArrayList<>();
        OpptjeningAktivitet opptjeningAktivitet = new OpptjeningAktivitet(tomorrow.minusMonths(10),
            tomorrow,
            OpptjeningAktivitetType.ARBEID,
            OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT,
            "abc",
            ReferanseType.ORG_NR);
        aktiviteter.add(opptjeningAktivitet);

        // Act
        opptjeningRepository.lagreOpptjeningsperiode(behandling, today, tomorrow, false);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(today, tomorrow);
        opptjeningRepository.lagreOpptjeningResultat(behandling, tomorrow.plusDays(1), Period.ofDays(100), aktiviteter);

        // Assert
        var resultat = opptjeningRepository.finnOpptjening(behandling.getId()).orElseThrow();
        var funnet = resultat.getOpptjeningPerioder().stream().filter(it -> it.getOpptjeningPeriode().equals(periode)).findFirst().orElseThrow();
        assertThat(funnet.getOpptjeningAktivitet()).hasSize(1);
        OpptjeningAktivitet aktivitet = funnet.getOpptjeningAktivitet().get(0);
        assertThat(aktivitet.getFom()).isEqualTo(tomorrow.minusMonths(10));
        assertThat(aktivitet.getTom()).isEqualTo(tomorrow);
        assertThat(aktivitet.getAktivitetReferanseType()).isEqualTo(ReferanseType.ORG_NR);
        assertThat(aktivitet.getAktivitetType()).isEqualTo(OpptjeningAktivitetType.ARBEID);
        assertThat(aktivitet.getAktivitetReferanse()).isEqualTo("abc");
    }

    @Test
    public void finnAktivGrunnlagId() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        Behandling behandling = opprettBehandling();
        Opptjening opptjening = opptjeningRepository.lagreOpptjeningsperiode(behandling, today, tomorrow, false);

        // Act
        EndringsresultatSnapshot endringsresultatSnapshot = opptjeningRepository.finnAktivGrunnlagId(behandling);

        // Assert
        assertThat(endringsresultatSnapshot.getGrunnlagRef()).isEqualTo(opptjening.getOpptjeningResultat().getId());
    }

    private Behandling opprettBehandling() {
        Behandling behandling = behandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
        var vilkårResultat = Vilkårene.builder().build();
        behandlingBuilder.lagreVilkårResultat(behandling.getId(), vilkårResultat);
        return behandling;
    }

    @Test
    public void getOpptjeningAktivitetTypeForKode() {
        // Act
        String næringKode = OpptjeningAktivitetType.NÆRING.getKode();
        OpptjeningAktivitetType næring = OpptjeningAktivitetType.fraKode(næringKode);

        // Assert
        assertThat(næring.getKode()).isEqualTo(næringKode);
        assertThat(næring.getNavn()).isNotBlank();
    }

    @Test
    public void getOpptjeningAktivitetKlassifisering() {
        // Act
        String kode = OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT.getKode();
        OpptjeningAktivitetKlassifisering resultat = OpptjeningAktivitetKlassifisering.fraKode(kode);

        // Assert
        assertThat(resultat.getKode()).isEqualTo(kode);
        assertThat(resultat.getNavn()).isNotBlank();
    }
}
