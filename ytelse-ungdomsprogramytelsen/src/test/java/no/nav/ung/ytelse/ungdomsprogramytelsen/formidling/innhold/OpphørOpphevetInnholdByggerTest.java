package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.trigger.ProsessTriggere;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto.OpphørOpphevetDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class OpphørOpphevetInnholdByggerTest {

    private static final Long BEHANDLING_ID = 1000L;
    private static final LocalDate TIDLIGERE_OPPHØRSDATO = LocalDate.of(2026, 10, 15);
    private static final LocalDate MAKSDATO = LocalDate.of(2027, 3, 1);

    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository = mock(UngdomsprogramPeriodeRepository.class);
    private final ProsessTriggereRepository prosessTriggereRepository = mock(ProsessTriggereRepository.class);
    private final OpphørOpphevetInnholdBygger bygger = new OpphørOpphevetInnholdBygger(ungdomsprogramPeriodeRepository, prosessTriggereRepository);

    @Test
    void skal_utlede_tidligere_sluttdato_fra_triggerperiode_selv_når_opphør_og_opphevelse_er_slått_sammen_på_samme_behandling() {
        // Simulerer sammenslåing: forrige behandling har IKKE fått persistert det opprinnelige opphøret
        // (dvs. periodegrunnlaget på forrige behandling ville gitt feil svar), men triggerperioden på
        // DENNE behandlingen reflekterer korrekt hvilken opphørsdato som oppheves.
        var behandling = mock(Behandling.class);
        when(behandling.getId()).thenReturn(BEHANDLING_ID);

        var trigger = new Trigger(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM,
            DatoIntervallEntitet.fraOgMedTilOgMed(TIDLIGERE_OPPHØRSDATO.plusDays(1), MAKSDATO));
        var prosessTriggere = mock(ProsessTriggere.class);
        when(prosessTriggere.getTriggere()).thenReturn(Set.of(trigger));
        when(prosessTriggereRepository.hentGrunnlag(BEHANDLING_ID)).thenReturn(Optional.of(prosessTriggere));

        var periodeGrunnlag = mock(UngdomsprogramPeriodeGrunnlag.class);
        when(periodeGrunnlag.getPeriodeMaksDato()).thenReturn(Optional.of(MAKSDATO));
        when(ungdomsprogramPeriodeRepository.hentGrunnlag(BEHANDLING_ID)).thenReturn(Optional.of(periodeGrunnlag));

        var resultat = bygger.bygg(behandling, LocalDateTimeline.empty());

        assertThat(resultat.templateType()).isEqualTo(TemplateType.OPPHOR_OPPHEVET);
        var dto = (OpphørOpphevetDto) resultat.templateInnholdDto();
        assertThat(dto.tidligereSluttdato()).isEqualTo(TIDLIGERE_OPPHØRSDATO);
        assertThat(dto.maksdato()).isEqualTo(MAKSDATO);
    }

    @Test
    void skal_kaste_feil_dersom_prosesstrigger_for_opphevelse_mangler() {
        var behandling = mock(Behandling.class);
        when(behandling.getId()).thenReturn(BEHANDLING_ID);
        when(prosessTriggereRepository.hentGrunnlag(BEHANDLING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bygger.bygg(behandling, LocalDateTimeline.empty()))
            .isInstanceOf(IllegalStateException.class);
    }

}
