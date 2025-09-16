package no.nav.ung.sak.behandlingslager.uttalelse;

import jakarta.inject.Inject;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.varsel.EndringType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
public class UttalelseRepositoryTest {

    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private UttalelseRepository repository;
    private Behandling behandling;

    @BeforeEach
    public void setUp() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAKEN"), LocalDate.now(), null);
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling.getId()));
    }

    @Test
    void skal_hente_grunnlag_bassert_på_behandling_id() {
        UttalelseGrunnlag uttalelseGrunnlag = lagreUttalelseGrunnlag();

        Optional<UttalelseGrunnlag> resultat = repository.hentUttalelseBassertPåId(behandling.getId());

        assertThat(resultat).isPresent();
        assertThat(resultat.get().getId()).isEqualTo(uttalelseGrunnlag.getId());
    }

    @Test
    void skal_ikke_sette_id(){
        UttalelseGrunnlag uttalelseGrunnlag = new UttalelseGrunnlag(behandling.getId());
        assertThat(uttalelseGrunnlag.getBehandlingId()).isNotNull();
        assertThat(uttalelseGrunnlag.getId()).isNull();
    }

    @Test
    void skal_lagre_uttalelse(){
        lagreUttalelseGrunnlag();
    }

    private UttalelseGrunnlag lagreUttalelseGrunnlag() {
        final var uttalelsegrunnlag = new UttalelseGrunnlag(behandling.getId());
        var uttalelse1 = new UttalelseV2(
            true,
            "Begrunnelse 1",
            DatoIntervallEntitet.fraOgMedTilOgMed(
                LocalDate.of(2025,8,1),
                LocalDate.of(2025,8,31)),
            null,
            EndringType.ENDRET_INNTEKT,
            uttalelsegrunnlag.getId());
        uttalelsegrunnlag.leggTilUttalelser(List.of(uttalelse1));
        repository.lagre(uttalelsegrunnlag.getId(), List.of(uttalelse1));
        return uttalelsegrunnlag;
    }
}
