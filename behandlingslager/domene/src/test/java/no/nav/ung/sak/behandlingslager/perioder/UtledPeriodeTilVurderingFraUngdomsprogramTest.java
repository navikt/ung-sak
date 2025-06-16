package no.nav.ung.sak.behandlingslager.perioder;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class UtledPeriodeTilVurderingFraUngdomsprogramTest {

    @Inject
    private ProsessTriggereRepository prosessTriggereRepository;
    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private BehandlingRepository behandlingRepository;

    private UtledPeriodeTilVurderingFraUngdomsprogram utledPeriodeTilVurderingFraUngdomsprogram;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        utledPeriodeTilVurderingFraUngdomsprogram = new UtledPeriodeTilVurderingFraUngdomsprogram(prosessTriggereRepository);
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAKEN"), LocalDate.now(), null);
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling.getId()));
    }

    @Test
    void skal_ikke_finne_perioder() {
        var tidslinje = utledPeriodeTilVurderingFraUngdomsprogram.finnTidslinje(behandling.getId());
        assertThat(tidslinje.isEmpty()).isTrue();
    }

    @Test
    void skal_finne_en_periode_dersom_det_finnes_en_trigger_for_opphør() {
        var fom = LocalDate.now();
        var tom = fom.plusDays(10);
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(new Trigger(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM, DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))));

        var tidslinje = utledPeriodeTilVurderingFraUngdomsprogram.finnTidslinje(behandling.getId());

        assertThat(tidslinje.equals(new LocalDateTimeline<>(fom, tom, true))).isTrue();
    }

    @Test
    void skal_finne_tom_tidslinje_dersom_det_ikke_finnes_en_trigger_for_opphør_men_for_en_annen_årsak() {
        var fom = LocalDate.now();
        var tom = fom.plusDays(10);
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(new Trigger(BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER, DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))));

        var tidslinje = utledPeriodeTilVurderingFraUngdomsprogram.finnTidslinje(behandling.getId());

        assertThat(tidslinje.isEmpty()).isTrue();
    }

    @Test
    void skal_finne_en_periode_for_to_overlappende_triggere() {
        var fom = LocalDate.now();
        var tom = fom.plusDays(10);
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(
            new Trigger(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM, DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(5))),
            new Trigger(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM, DatoIntervallEntitet.fraOgMedTilOgMed(fom.plusDays(2), tom))
            ));

        var tidslinje = utledPeriodeTilVurderingFraUngdomsprogram.finnTidslinje(behandling.getId());

        assertThat(tidslinje.equals(new LocalDateTimeline<>(fom, tom, true))).isTrue();
    }

}
