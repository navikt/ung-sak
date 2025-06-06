package no.nav.ung.sak.hendelsemottak.tjenester;

import jakarta.inject.Inject;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.hendelser.HendelseInfo;
import no.nav.ung.sak.kontrakt.hendelser.UngdomsprogramOpphørHendelse;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class HendelsemottakTjenesteTest {

    public static final AktørId AKTØR_ID = AktørId.dummy();
    public static final LocalDate FAGSAK_TOM = LocalDate.now().plusDays(50);
    @Inject
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;
    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private BehandlingRepository behandlingRepository;

    private final FagsakerTilVurderingUtleder fagsakerTilVurderingUtleder = mock(FagsakerTilVurderingUtleder.class);

    private HendelsemottakTjeneste hendelsemottakTjeneste;
    private Behandling behandling;
    private Fagsak fagsak;

    @BeforeEach
    void setUp() {
        hendelsemottakTjeneste = new HendelsemottakTjeneste(new UnitTestLookupInstanceImpl<>(fagsakerTilVurderingUtleder), behandlingRepository, fagsakProsessTaskRepository);
        fagsak = Fagsak.opprettNy(FagsakYtelseType.UNGDOMSYTELSE, AKTØR_ID, new Saksnummer("SAKEN"), LocalDate.now(), FAGSAK_TOM);
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling.getId()));
    }

    @Test
    void skal_ikke_opprette_revurdering_task_for_hendelse_som_ikke_er_relevant() {
        when(fagsakerTilVurderingUtleder.finnFagsakerTilVurdering(any())).thenReturn(Map.of());
        var opphørsdato = LocalDate.now().plusDays(10);
        var opphørHendelse = new UngdomsprogramOpphørHendelse(new HendelseInfo.Builder().medHendelseId("hendelse1").medOpprettet(LocalDateTime.now()).leggTilAktør(AKTØR_ID).build(),
            opphørsdato);

        hendelsemottakTjeneste.mottaHendelse(opphørHendelse);

        var åpneTasker = fagsakProsessTaskRepository.finnAlleÅpneTasksForAngittSøk(behandling.getFagsakId(), behandling.getId(), null);

        assertThat(åpneTasker.isEmpty()).isTrue();
    }

    @Test
    void skal_opprette_revurdering_task_for_hendelse_som_er_relevant() {
        var opphørsdato = LocalDate.now().plusDays(10);
        when(fagsakerTilVurderingUtleder.finnFagsakerTilVurdering(any())).thenReturn(Map.of(fagsak, new ÅrsakOgPeriode(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM, DatoIntervallEntitet.fraOgMedTilOgMed(opphørsdato, fagsak.getPeriode().getTomDato()))));
        var opphørHendelse = new UngdomsprogramOpphørHendelse(new HendelseInfo.Builder().medHendelseId("hendelse1").medOpprettet(LocalDateTime.now()).leggTilAktør(AKTØR_ID).build(),
            opphørsdato);

        hendelsemottakTjeneste.mottaHendelse(opphørHendelse);

        var åpneTasker = fagsakProsessTaskRepository.finnAlleÅpneTasksForAngittSøk(behandling.getFagsakId(), behandling.getId(), null);

        assertThat(åpneTasker.size()).isEqualTo(1);
        var task = åpneTasker.get(0);
        assertThat(task.getPropertyValue(OpprettRevurderingEllerOpprettDiffTask.PERIODE_FOM)).isEqualTo(opphørsdato.toString());
        assertThat(task.getPropertyValue(OpprettRevurderingEllerOpprettDiffTask.PERIODE_TOM)).isEqualTo(FAGSAK_TOM.toString());
        assertThat(task.getPropertyValue(OpprettRevurderingEllerOpprettDiffTask.BEHANDLING_ÅRSAK)).isEqualTo(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM.getKode());
    }

}
