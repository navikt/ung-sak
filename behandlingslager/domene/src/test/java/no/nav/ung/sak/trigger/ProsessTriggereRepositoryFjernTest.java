package no.nav.ung.sak.trigger;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;

/**
 * Tester {@link ProsessTriggereRepository#fjern(Long, BehandlingÅrsakType, DatoIntervallEntitet)} mot en reell
 * (JPA-backet) database, siden metoden gjør oppslag/skriving via EntityManager på samme måte som {@code leggTil}.
 */
@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class ProsessTriggereRepositoryFjernTest {

    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private ProsessTriggereRepository prosessTriggereRepository;

    private Long behandlingId;

    @BeforeEach
    void setUp() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAKEN"), LocalDate.now(), null);
        fagsakRepository.opprettNy(fagsak);
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
        behandlingId = behandling.getId();
    }

    @Test
    void skal_fjerne_matchende_trigger_og_beholde_resten() {
        var periodeSomSkalFjernes = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
        var periodeSomSkalBeholdes = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29));
        prosessTriggereRepository.leggTil(behandlingId, Set.of(
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, periodeSomSkalFjernes),
            new Trigger(BehandlingÅrsakType.RE_HENDELSE_FØDSEL, periodeSomSkalBeholdes)
        ));

        prosessTriggereRepository.fjern(behandlingId, BehandlingÅrsakType.NY_SØKT_PERIODE, periodeSomSkalFjernes);

        var gjenværende = prosessTriggereRepository.hentGrunnlag(behandlingId).orElseThrow().getTriggere();
        assertThat(gjenværende).hasSize(1);
        assertThat(gjenværende.iterator().next().getÅrsak()).isEqualTo(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
    }

    @Test
    void skal_ikke_gjøre_noe_naar_ingen_trigger_matcher() {
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
        prosessTriggereRepository.leggTil(behandlingId, Set.of(new Trigger(BehandlingÅrsakType.RE_HENDELSE_FØDSEL, periode)));

        var annenPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));
        prosessTriggereRepository.fjern(behandlingId, BehandlingÅrsakType.NY_SØKT_PERIODE, annenPeriode);

        var gjenværende = prosessTriggereRepository.hentGrunnlag(behandlingId).orElseThrow().getTriggere();
        assertThat(gjenværende).hasSize(1);
    }

    @Test
    void skal_ikke_feile_naar_det_ikke_finnes_noe_grunnlag() {
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        prosessTriggereRepository.fjern(behandlingId, BehandlingÅrsakType.NY_SØKT_PERIODE, periode);

        assertThat(prosessTriggereRepository.hentGrunnlag(behandlingId)).isEmpty();
    }
}
