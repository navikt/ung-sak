package no.nav.ung.sak.domene.registerinnhenting.impl.startpunkt;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.trigger.ProsessTriggere;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StartpunktUtlederProsessTriggereTest {

    private static final Long ELDSTE_ID = 1L;
    private static final Long NYESTE_ID = 2L;
    private static final LocalDate FOM = LocalDate.of(2025, 3, 1);
    private static final LocalDate TOM = LocalDate.of(2025, 6, 30);

    private ProsessTriggereRepository repository;
    private StartpunktUtlederProsessTriggere utleder;
    private BehandlingReferanse ref;

    @BeforeEach
    void setUp() {
        repository = mock(ProsessTriggereRepository.class);
        utleder = new StartpunktUtlederProsessTriggere(repository);
        ref = mock(BehandlingReferanse.class);
    }

    @Test
    void ingen_endringer_gir_udefinert() {
        var trigger = new Trigger(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM));
        mockGrunnlag(ELDSTE_ID, Set.of(trigger));
        mockGrunnlag(NYESTE_ID, Set.of(trigger));

        var resultat = utleder.utledStartpunkt(ref, NYESTE_ID, ELDSTE_ID);

        assertThat(resultat).isEqualTo(StartpunktType.UDEFINERT);
    }

    @Test
    void kontroll_inntekt_trigger_gir_vurder_kompletthet() {
        mockGrunnlag(ELDSTE_ID, Set.of());
        mockGrunnlag(NYESTE_ID, Set.of(
            new Trigger(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))
        ));

        var resultat = utleder.utledStartpunkt(ref, NYESTE_ID, ELDSTE_ID);

        assertThat(resultat).isEqualTo(StartpunktType.VURDER_KOMPLETTHET);
    }

    @Test
    void rapportering_inntekt_trigger_gir_vurder_kompletthet() {
        mockGrunnlag(ELDSTE_ID, Set.of());
        mockGrunnlag(NYESTE_ID, Set.of(
            new Trigger(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))
        ));

        var resultat = utleder.utledStartpunkt(ref, NYESTE_ID, ELDSTE_ID);

        assertThat(resultat).isEqualTo(StartpunktType.VURDER_KOMPLETTHET);
    }

    @Test
    void uttalelse_fra_bruker_trigger_gir_vurder_kompletthet() {
        mockGrunnlag(ELDSTE_ID, Set.of());
        mockGrunnlag(NYESTE_ID, Set.of(
            new Trigger(BehandlingÅrsakType.UTTALELSE_FRA_BRUKER, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))
        ));

        var resultat = utleder.utledStartpunkt(ref, NYESTE_ID, ELDSTE_ID);

        assertThat(resultat).isEqualTo(StartpunktType.VURDER_KOMPLETTHET);
    }

    @Test
    void sats_regulering_trigger_gir_beregning() {
        mockGrunnlag(ELDSTE_ID, Set.of());
        mockGrunnlag(NYESTE_ID, Set.of(
            new Trigger(BehandlingÅrsakType.RE_SATS_REGULERING, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))
        ));

        var resultat = utleder.utledStartpunkt(ref, NYESTE_ID, ELDSTE_ID);

        assertThat(resultat).isEqualTo(StartpunktType.BEREGNING);
    }

    private void mockGrunnlag(Long id, Set<Trigger> triggere) {
        var grunnlag = mock(ProsessTriggere.class);
        when(grunnlag.getTriggere()).thenReturn(triggere);
        when(repository.hentGrunnlagBasertPåId(id)).thenReturn(Optional.of(grunnlag));
    }
}
