package no.nav.ung.sak.økonomi.tilbakekreving.modell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class TilbakekrevingRepositoryImplTest {

    @Inject
    private EntityManager entityManager;

    private TilbakekrevingRepository repository;
    private BasicBehandlingBuilder behandlingBuilder;

    @BeforeEach
    public void setup() {
        repository = new TilbakekrevingRepository(entityManager);
        behandlingBuilder = new BasicBehandlingBuilder(entityManager);
    }

    @Test
    public void skal_gi_empty_når_det_hentes_for_behandling_som_ikke_har_tilbakekrevingsvalg() {
        Behandling behandling = Mockito.mock(Behandling.class);
        when(behandling.getId()).thenReturn(2L);
        assertThat(repository.hent(2L)).isEmpty();
    }

    @Test
    public void skal_lagre_og_hente_tilbakekrevingsvalg() {
        Behandling behandling = opprettBehandling();

        TilbakekrevingValg valg = TilbakekrevingValg.utenMulighetForInntrekk(TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING, "Varsel");
        repository.lagre(behandling, valg);

        Optional<TilbakekrevingValg> lagretResultat = repository.hent(behandling.getId());
        assertThat(lagretResultat).isPresent();
        TilbakekrevingValg resultat = lagretResultat.get();
        assertThat(resultat.getVidereBehandling()).isEqualTo(TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING);
        assertThat(resultat.getErTilbakekrevingVilkårOppfylt()).isNull();
        assertThat(resultat.getGrunnerTilReduksjon()).isNull();
    }

    @Test
    public void skal_oppdatere_tilbakekrevingsvalg() {
        Behandling behandling = opprettBehandling();

        TilbakekrevingValg valg1 = TilbakekrevingValg.utenMulighetForInntrekk(TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING, "Varseltekst");
        repository.lagre(behandling, valg1);

        TilbakekrevingValg valg2 = TilbakekrevingValg.medMulighetForInntrekk(true, false, TilbakekrevingVidereBehandling.INNTREKK);
        repository.lagre(behandling, valg2);

        Optional<TilbakekrevingValg> lagretResultat = repository.hent(behandling.getId());
        assertThat(lagretResultat).isPresent();
        TilbakekrevingValg resultat = lagretResultat.get();
        assertThat(resultat.getVidereBehandling()).isEqualTo(TilbakekrevingVidereBehandling.INNTREKK);
        assertThat(resultat.getErTilbakekrevingVilkårOppfylt()).isTrue();
        assertThat(resultat.getGrunnerTilReduksjon()).isFalse();
    }

    @Test
    public void lagrer_tilbakekreving_inntrekk() {
        // Arrange
        Behandling behandling = opprettBehandling();

        // Act
        repository.lagre(behandling, true);
        Optional<TilbakekrevingInntrekkEntitet> tilbakekrevingInntrekkEntitet = repository.hentTilbakekrevingInntrekk(behandling.getId());

        // Assert
        assertThat(tilbakekrevingInntrekkEntitet).hasValueSatisfying(TilbakekrevingInntrekkEntitet::isAvslåttInntrekk);
    }

    @Test
    public void oppdaterer_tilbakekreving_inntrekk() {
        // Arrange
        Behandling behandling = opprettBehandling();

        // Act
        repository.lagre(behandling, true);
        repository.lagre(behandling, false);
        Optional<TilbakekrevingInntrekkEntitet> tilbakekrevingInntrekkEntitet = repository.hentTilbakekrevingInntrekk(behandling.getId());

        // Assert
        assertThat(tilbakekrevingInntrekkEntitet).isPresent();
        assertThat(tilbakekrevingInntrekkEntitet.get().isAvslåttInntrekk()).isFalse();
    }

    private Behandling opprettBehandling() {
        return behandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
    }

}
