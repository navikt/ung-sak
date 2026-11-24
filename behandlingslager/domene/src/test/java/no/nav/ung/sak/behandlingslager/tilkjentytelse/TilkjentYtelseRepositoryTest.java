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
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
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

    private Behandling nyBehandling() {
        return behandlingBuilder.opprettNyBehandling(fagsak, BehandlingType.REVURDERING, BehandlingStatus.UTREDES);
    }

    private static KontrollertInntektPeriode kontrollperiode() {
        return KontrollertInntektPeriode.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 31)))
            .medInntekt(BigDecimal.TEN)
            .medKilde(KontrollertInntektKilde.REGISTER)
            .build();
    }
}
