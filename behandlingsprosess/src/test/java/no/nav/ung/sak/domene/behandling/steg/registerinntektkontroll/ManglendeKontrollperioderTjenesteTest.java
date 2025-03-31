package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import jakarta.inject.Inject;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseSøktStartdato;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.perioder.UngdomsytelseSøknadsperiodeTjeneste;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.ytelse.KontrollerteInntektperioderTjeneste;
import no.nav.ung.sak.ytelseperioder.YtelseperiodeUtleder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class ManglendeKontrollperioderTjenesteTest {

    @Inject
    private ProsessTriggereRepository prosessTriggereRepository;
    @Inject
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    @Inject
    private TilkjentYtelseRepository tilkjentYtelseRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository;
    @Inject
    private FagsakRepository fagsakRepository;
    private Behandling behandling;
    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;
    private KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste;
    private YtelseperiodeUtleder ytelseperiodeUtleder;


    @BeforeEach
    void setUp() {
        final var ungdomsprogramPeriodeTjeneste = new UngdomsprogramPeriodeTjeneste(ungdomsprogramPeriodeRepository);
        final var ungdomsytelseSøknadsperiodeTjeneste = new UngdomsytelseSøknadsperiodeTjeneste(ungdomsytelseStartdatoRepository, ungdomsprogramPeriodeTjeneste, behandlingRepository);
        prosessTriggerPeriodeUtleder = new ProsessTriggerPeriodeUtleder(prosessTriggereRepository, ungdomsytelseSøknadsperiodeTjeneste);
        kontrollerteInntektperioderTjeneste = new KontrollerteInntektperioderTjeneste(tilkjentYtelseRepository);
        ytelseperiodeUtleder = new YtelseperiodeUtleder(ungdomsprogramPeriodeTjeneste, behandlingRepository);



        lagFagsakOgBehandling(LocalDate.now().minusMonths(6));




    }

    @Test
    void skal_ikke_legge_til_trigger_dersom_kun_en_måned() {

        var manglendeKontrollperioderTjeneste = new ManglendeKontrollperioderTjeneste(kontrollerteInntektperioderTjeneste, ytelseperiodeUtleder, prosessTriggerPeriodeUtleder, prosessTriggereRepository, 6);

        final var startdatoUngdomsprogram = LocalDate.now().minusMonths(2).withDayOfMonth(1);
        final var sluttdatoUngdomsprogram = LocalDate.now().minusMonths(2).with(TemporalAdjusters.lastDayOfMonth());

        ungdomsytelseStartdatoRepository.lagre(behandling.getId(), List.of(new UngdomsytelseSøktStartdato(startdatoUngdomsprogram, new JournalpostId(1L))));
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(startdatoUngdomsprogram, sluttdatoUngdomsprogram)));

        manglendeKontrollperioderTjeneste.leggTilManglendeKontrollTriggere(behandling.getId());

        final var prosessTriggere = prosessTriggereRepository.hentGrunnlag(behandling.getId());
        assertThat(prosessTriggere.isEmpty()).isTrue();
    }

    @Test
    void skal_ikke_legge_til_trigger_dersom_programdeltakelse_over_to_måneder() {

        var manglendeKontrollperioderTjeneste = new ManglendeKontrollperioderTjeneste(kontrollerteInntektperioderTjeneste, ytelseperiodeUtleder, prosessTriggerPeriodeUtleder, prosessTriggereRepository, 6);

        final var startdatoUngdomsprogram = LocalDate.now().minusMonths(3).withDayOfMonth(1);
        final var sluttdatoUngdomsprogram = LocalDate.now().minusMonths(2).with(TemporalAdjusters.lastDayOfMonth());

        ungdomsytelseStartdatoRepository.lagre(behandling.getId(), List.of(new UngdomsytelseSøktStartdato(startdatoUngdomsprogram, new JournalpostId(1L))));
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(startdatoUngdomsprogram, sluttdatoUngdomsprogram)));

        manglendeKontrollperioderTjeneste.leggTilManglendeKontrollTriggere(behandling.getId());

        final var prosessTriggere = prosessTriggereRepository.hentGrunnlag(behandling.getId());
        assertThat(prosessTriggere.isEmpty()).isTrue();
    }

    @Test
    void skal_legge_til_trigger_for_måned_nr_to_dersom_programdeltakelse_over_tre_måneder_og_passert_rapporteringsfrist() {

        var manglendeKontrollperioderTjeneste = new ManglendeKontrollperioderTjeneste(kontrollerteInntektperioderTjeneste, ytelseperiodeUtleder, prosessTriggerPeriodeUtleder, prosessTriggereRepository, 6);

        final var startdatoUngdomsprogram = LocalDate.now().minusMonths(4).withDayOfMonth(1);
        final var sluttdatoUngdomsprogram = LocalDate.now().minusMonths(2).with(TemporalAdjusters.lastDayOfMonth());

        ungdomsytelseStartdatoRepository.lagre(behandling.getId(), List.of(new UngdomsytelseSøktStartdato(startdatoUngdomsprogram, new JournalpostId(1L))));
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(startdatoUngdomsprogram, sluttdatoUngdomsprogram)));

        manglendeKontrollperioderTjeneste.leggTilManglendeKontrollTriggere(behandling.getId());

        final var prosessTriggere = prosessTriggereRepository.hentGrunnlag(behandling.getId());
        assertThat(prosessTriggere.isEmpty()).isFalse();
        final var triggere = prosessTriggere.get().getTriggere();
        assertThat(triggere.size()).isEqualTo(1);
        final var nyTrigger = triggere.iterator().next();
        final var månedNrTre = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(3).withDayOfMonth(1), LocalDate.now().minusMonths(3).with(TemporalAdjusters.lastDayOfMonth()));
        assertThat(nyTrigger.getPeriode()).isEqualTo(månedNrTre);
    }

    @Test
    void skal_ikke_legge_til_trigger_dersom_ikke_passert_rapporteringsfrist() {
        final var startdatoUngdomsprogram = LocalDate.now().minusMonths(2).withDayOfMonth(1);
        final var sluttdatoUngdomsprogram = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        var manglendeKontrollperioderTjeneste = new ManglendeKontrollperioderTjeneste(kontrollerteInntektperioderTjeneste, ytelseperiodeUtleder, prosessTriggerPeriodeUtleder, prosessTriggereRepository, LocalDate.now().getDayOfMonth());


        ungdomsytelseStartdatoRepository.lagre(behandling.getId(), List.of(new UngdomsytelseSøktStartdato(startdatoUngdomsprogram, new JournalpostId(1L))));
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(startdatoUngdomsprogram, sluttdatoUngdomsprogram)));

        manglendeKontrollperioderTjeneste.leggTilManglendeKontrollTriggere(behandling.getId());

        final var prosessTriggere = prosessTriggereRepository.hentGrunnlag(behandling.getId());
        assertThat(prosessTriggere.isEmpty()).isTrue();
    }


    @Test
    void skal_legge_til_trigger_dersom_passert_rapporteringsfrist_med_en_dag() {
        final var startdatoUngdomsprogram = LocalDate.now().minusMonths(2).withDayOfMonth(1);
        final var sluttdatoUngdomsprogram = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        var manglendeKontrollperioderTjeneste = new ManglendeKontrollperioderTjeneste(kontrollerteInntektperioderTjeneste, ytelseperiodeUtleder, prosessTriggerPeriodeUtleder, prosessTriggereRepository, LocalDate.now().getDayOfMonth()-1);


        ungdomsytelseStartdatoRepository.lagre(behandling.getId(), List.of(new UngdomsytelseSøktStartdato(startdatoUngdomsprogram, new JournalpostId(1L))));
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(startdatoUngdomsprogram, sluttdatoUngdomsprogram)));

        manglendeKontrollperioderTjeneste.leggTilManglendeKontrollTriggere(behandling.getId());

        final var prosessTriggere = prosessTriggereRepository.hentGrunnlag(behandling.getId());
        assertThat(prosessTriggere.isEmpty()).isFalse();
        final var triggere = prosessTriggere.get().getTriggere();
        assertThat(triggere.size()).isEqualTo(1);
        final var nyTrigger = triggere.iterator().next();
        final var månedNrTre = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(1).withDayOfMonth(1), LocalDate.now().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth()));
        assertThat(nyTrigger.getPeriode()).isEqualTo(månedNrTre);
    }

    @Test
    void skal_ikke_legge_til_trigger_dersom_allerede_kontrollert() {

        var manglendeKontrollperioderTjeneste = new ManglendeKontrollperioderTjeneste(kontrollerteInntektperioderTjeneste, ytelseperiodeUtleder, prosessTriggerPeriodeUtleder, prosessTriggereRepository, 6);

        final var startdatoUngdomsprogram = LocalDate.now().minusMonths(4).withDayOfMonth(1);
        final var sluttdatoUngdomsprogram = LocalDate.now().minusMonths(2).with(TemporalAdjusters.lastDayOfMonth());
        final var månedNrTre = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(3).withDayOfMonth(1), LocalDate.now().minusMonths(3).with(TemporalAdjusters.lastDayOfMonth()));

        ungdomsytelseStartdatoRepository.lagre(behandling.getId(), List.of(new UngdomsytelseSøktStartdato(startdatoUngdomsprogram, new JournalpostId(1L))));
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(startdatoUngdomsprogram, sluttdatoUngdomsprogram)));
        tilkjentYtelseRepository.lagre(behandling.getId(), List.of(KontrollertInntektPeriode.ny().medPeriode(månedNrTre).medKilde(KontrollertInntektKilde.BRUKER).medErManueltVurdert(false).build()));

        manglendeKontrollperioderTjeneste.leggTilManglendeKontrollTriggere(behandling.getId());

        final var prosessTriggere = prosessTriggereRepository.hentGrunnlag(behandling.getId());
        assertThat(prosessTriggere.isEmpty()).isTrue();
    }



    private Long lagFagsakOgBehandling(LocalDate fom) {
        final var fagsak = new Fagsak(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAKEN"), fom, fom.plusWeeks(52));
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling.getId();
    }

}
