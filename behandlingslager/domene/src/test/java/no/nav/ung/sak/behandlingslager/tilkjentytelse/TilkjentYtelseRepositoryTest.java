package no.nav.ung.sak.behandlingslager.tilkjentytelse;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;
import no.nav.ung.sak.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.diff.DiffResult;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class TilkjentYtelseRepositoryTest {

    private static final DatoIntervallEntitet AUGUST = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 31));
    private static final DatoIntervallEntitet SEPTEMBER = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 30));

    @Inject
    private EntityManager entityManager;
    private TilkjentYtelseRepository repository;
    private BasicBehandlingBuilder behandlingBuilder;
    private Fagsak fagsak;

    @BeforeEach
    void setUp() {
        repository = new TilkjentYtelseRepository(entityManager);
        behandlingBuilder = new BasicBehandlingBuilder(entityManager);
        fagsak = Fagsak.opprettNy(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAK1"), LocalDate.now(), null);
        new FagsakRepository(entityManager).opprettNy(fagsak);
    }

    @Test
    void erstatting_av_kontrollperioder_deaktiverer_målet_når_kilden_ikke_har_kontrollperioder() {
        var fra = nyBehandling();
        var mål = nyBehandling();
        var utenKontrollperioder = nyBehandling();
        repository.lagre(fra.getId(), List.of(kontrollperiode()));

        repository.erstattKontrollPerioderMedKopiFra(fra.getId(), mål.getId());

        assertThat(repository.hentKontrollertInntektPerioder(mål.getId()).orElseThrow())
            .isSameAs(repository.hentKontrollertInntektPerioder(fra.getId()).orElseThrow());

        repository.erstattKontrollPerioderMedKopiFra(utenKontrollperioder.getId(), mål.getId());

        assertThat(repository.hentKontrollertInntektPerioder(mål.getId())).isEmpty();
        assertThat(repository.hentKontrollertInntektPerioder(fra.getId())).isPresent();
    }

    @Test
    void diff_av_kontrollperioder_ser_bort_fra_rekkefølge_og_skala_og_fanger_endret_inntekt() {
        var august = nyBehandling();
        var augustOgSeptember = nyBehandling();
        var septemberOgAugust = nyBehandling();
        var endretInntekt = nyBehandling();
        repository.lagre(august.getId(), List.of(kontrollperiode(AUGUST, BigDecimal.TEN)));
        repository.lagre(augustOgSeptember.getId(), List.of(kontrollperiode(AUGUST, BigDecimal.TEN), kontrollperiode(SEPTEMBER, BigDecimal.ONE)));
        repository.lagre(septemberOgAugust.getId(), List.of(kontrollperiode(SEPTEMBER, BigDecimal.ONE), kontrollperiode(AUGUST, new BigDecimal("10.00"))));
        repository.lagre(endretInntekt.getId(), List.of(kontrollperiode(AUGUST, BigDecimal.TEN), kontrollperiode(SEPTEMBER, BigDecimal.TWO)));

        assertThat(diffKontrollperioder(augustOgSeptember, septemberOgAugust).isEmpty()).isTrue();
        assertThat(diffKontrollperioder(augustOgSeptember, endretInntekt).isEmpty()).isFalse();
        assertThat(diffKontrollperioder(august, augustOgSeptember).isEmpty()).isFalse();
    }

    private DiffResult diffKontrollperioder(Behandling før, Behandling etter) {
        var id1 = (Long) repository.finnAktivKontrollperioderId(før.getId()).getGrunnlagRef();
        var id2 = (Long) repository.finnAktivKontrollperioderId(etter.getId()).getGrunnlagRef();
        assertThat(id1).isNotEqualTo(id2);
        return repository.diffKontrollperioder(EndringsresultatDiff.medDiff(KontrollertInntektPerioder.class, id1, id2));
    }

    private Behandling nyBehandling() {
        return behandlingBuilder.opprettNyBehandling(fagsak, BehandlingType.REVURDERING, BehandlingStatus.UTREDES);
    }

    private static KontrollertInntektPeriode kontrollperiode() {
        return kontrollperiode(AUGUST, BigDecimal.TEN);
    }

    private static KontrollertInntektPeriode kontrollperiode(DatoIntervallEntitet periode, BigDecimal inntekt) {
        return KontrollertInntektPeriode.ny()
            .medPeriode(periode)
            .medInntekt(inntekt)
            .medKilde(KontrollertInntektKilde.REGISTER)
            .build();
    }
}
