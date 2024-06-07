package no.nav.k9.sak.forvaltning;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class DumpFeilImRepositoryTest {

    @Inject
    private DumpFeilImRepository dumpFeilImRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private FagsakRepository fagsakRepository;
    private Behandling behandling;

    @BeforeEach
    public void setup() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, new AktørId(123L), new Saksnummer("987"), LocalDate.now(), LocalDate.now());
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).medBehandlingStatus(BehandlingStatus.UTREDES).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
    }

    @Test
    void skal_lagre_dump_med_kun_vilkårsperioder() {
        dumpFeilImRepository.lagre(behandling.getId(), Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now())), Set.of());
        var dumpFeilIMS = dumpFeilImRepository.hentAlle();
        assertThat(dumpFeilIMS.size()).isEqualTo(1);
    }

    @Test
    void skal_lagre_dump_med_kun_fordelperioder() {
        dumpFeilImRepository.lagre(behandling.getId(), Set.of(), Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now())));
        var dumpFeilIMS = dumpFeilImRepository.hentAlle();
        assertThat(dumpFeilIMS.size()).isEqualTo(1);
    }

    @Test
    void skal_lagre_dump_med_fordelperioder_og_vilkårperioder() {
        dumpFeilImRepository.lagre(behandling.getId(), Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now())), Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now())));
        var dumpFeilIMS = dumpFeilImRepository.hentAlle();
        assertThat(dumpFeilIMS.size()).isEqualTo(1);
    }


}
