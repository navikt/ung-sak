package no.nav.k9.sak.ytelse.frisinn.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.EgenNæringBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittFrilansBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittFrilansOppdragBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class OppgittOpptjeningFilterTest {

    private final DatoIntervallEntitet periodeApril = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2020, 3, 30), LocalDate.of(2020, 4, 30));
    private final DatoIntervallEntitet periodeMai = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31));

    @Test
    public void skal_legge_til_tilkommet_periode_for_SN_FRISINN() {
        // Arrange
        var oppgittOpptjening = OppgittOpptjeningBuilder.ny();
        var april = EgenNæringBuilder.ny();
        april.medBruttoInntekt(BigDecimal.TEN).medPeriode(periodeApril);

        var mai = EgenNæringBuilder.ny();
        mai.medBruttoInntekt(BigDecimal.TEN).medPeriode(periodeMai);

        oppgittOpptjening.leggTilEgneNæringer(List.of(april, mai));

        var overstryt = OppgittOpptjeningBuilder.ny();
        var aprilOverstyrt = EgenNæringBuilder.ny();
        aprilOverstyrt.medBruttoInntekt(BigDecimal.ZERO).medPeriode(periodeApril);
        overstryt.leggTilEgneNæringer(List.of(aprilOverstyrt));

        // Act
        var filter = new OppgittOpptjeningFilter(oppgittOpptjening.build(), overstryt.build());
        var oppgittOpptjeningFrisinn = filter.getOppgittOpptjeningFrisinn();


        // Assert
        assertThat(oppgittOpptjeningFrisinn.getEgenNæring()).hasSize(2);
        assertThat(oppgittOpptjeningFrisinn.getEgenNæring().stream().filter(oppgittEgenNæring -> oppgittEgenNæring.getPeriode().equals(periodeApril)).collect(Collectors.toList()).get(0).getBruttoInntekt()).isEqualTo(BigDecimal.ZERO);
        assertThat(oppgittOpptjeningFrisinn.getEgenNæring().stream().filter(oppgittEgenNæring -> oppgittEgenNæring.getPeriode().equals(periodeMai)).collect(Collectors.toList()).get(0).getBruttoInntekt()).isEqualTo(BigDecimal.TEN);
    }

    @Test
    public void skal_legge_til_tilkommet_periode_for_FL_FRISINN() {
        OppgittOpptjeningBuilder oppgittOpptjening = OppgittOpptjeningBuilder.ny();
        var frilans = OppgittFrilansBuilder.ny();
        var aprilOppdrag = OppgittFrilansOppdragBuilder.ny();
        aprilOppdrag.medInntekt(BigDecimal.TEN).medPeriode(periodeApril);

        var maiOppdrag = OppgittFrilansOppdragBuilder.ny();
        maiOppdrag.medInntekt(BigDecimal.TEN).medPeriode(periodeMai);

        frilans.medFrilansOppdrag(List.of(aprilOppdrag.build(), maiOppdrag.build()));
        oppgittOpptjening.leggTilFrilansOpplysninger(frilans.build());

        var overstryt = OppgittOpptjeningBuilder.ny();
        var frilansOverstryt = OppgittFrilansBuilder.ny();
        var aprilOppdragOverstryt = OppgittFrilansOppdragBuilder.ny();
        aprilOppdragOverstryt.medInntekt(BigDecimal.ZERO).medPeriode(periodeApril);
        frilansOverstryt.medFrilansOppdrag(List.of(aprilOppdragOverstryt.build()));
        overstryt.leggTilFrilansOpplysninger(frilansOverstryt.build());

        var filter = new OppgittOpptjeningFilter(oppgittOpptjening.build(), overstryt.build());

        var oppgittOpptjeningFrisinn = filter.getOppgittOpptjeningFrisinn();

        assertThat(oppgittOpptjeningFrisinn.getFrilans().get().getFrilansoppdrag()).hasSize(2);
        assertThat(oppgittOpptjeningFrisinn.getFrilans().get().getFrilansoppdrag().stream().filter(frilansOpp -> frilansOpp.getPeriode().equals(periodeApril)).collect(Collectors.toList()).get(0).getInntekt()).isEqualTo(BigDecimal.ZERO);
        assertThat(oppgittOpptjeningFrisinn.getFrilans().get().getFrilansoppdrag().stream().filter(frilansOpp -> frilansOpp.getPeriode().equals(periodeMai)).collect(Collectors.toList()).get(0).getInntekt()).isEqualTo(BigDecimal.TEN);
    }

    @Test
    public void skal_legge_til_tilkommet_periode_for_FL_og_SN_der_det_bare_var_FL_fra_før_FRISINN() {
        OppgittOpptjeningBuilder oppgittOpptjening = OppgittOpptjeningBuilder.ny();
        var frilans = OppgittFrilansBuilder.ny();
        var aprilOppdrag = OppgittFrilansOppdragBuilder.ny();
        aprilOppdrag.medInntekt(BigDecimal.TEN).medPeriode(periodeApril);

        var maiOppdrag = OppgittFrilansOppdragBuilder.ny();
        maiOppdrag.medInntekt(BigDecimal.TEN).medPeriode(periodeMai);

        frilans.medFrilansOppdrag(List.of(aprilOppdrag.build()));
        oppgittOpptjening.leggTilFrilansOpplysninger(frilans.build());

        var mai = EgenNæringBuilder.ny();
        mai.medBruttoInntekt(BigDecimal.TEN).medPeriode(periodeMai);

        frilans.medFrilansOppdrag(List.of(maiOppdrag.build()));
        oppgittOpptjening.leggTilFrilansOpplysninger(frilans.build());
        oppgittOpptjening.leggTilEgneNæringer(List.of(mai));

        var overstryt = OppgittOpptjeningBuilder.ny();
        var frilansOverstryt = OppgittFrilansBuilder.ny();
        var aprilOppdragOverstryt = OppgittFrilansOppdragBuilder.ny();
        aprilOppdragOverstryt.medInntekt(BigDecimal.ZERO).medPeriode(periodeApril);
        frilansOverstryt.medFrilansOppdrag(List.of(aprilOppdragOverstryt.build()));
        overstryt.leggTilFrilansOpplysninger(frilansOverstryt.build());

        var filter = new OppgittOpptjeningFilter(oppgittOpptjening.build(), overstryt.build());

        var oppgittOpptjeningFrisinn = filter.getOppgittOpptjeningFrisinn();

        assertThat(oppgittOpptjeningFrisinn.getFrilans().get().getFrilansoppdrag()).hasSize(2);
        assertThat(oppgittOpptjeningFrisinn.getFrilans().get().getFrilansoppdrag().stream().filter(frilansOpp -> frilansOpp.getPeriode().equals(periodeApril)).collect(Collectors.toList()).get(0).getInntekt()).isEqualTo(BigDecimal.ZERO);
        assertThat(oppgittOpptjeningFrisinn.getFrilans().get().getFrilansoppdrag().stream().filter(frilansOpp -> frilansOpp.getPeriode().equals(periodeMai)).collect(Collectors.toList()).get(0).getInntekt()).isEqualTo(BigDecimal.TEN);

        assertThat(oppgittOpptjeningFrisinn.getEgenNæring()).hasSize(1);
        assertThat(oppgittOpptjeningFrisinn.getEgenNæring()).hasSize(1);
        assertThat(oppgittOpptjeningFrisinn.getEgenNæring().stream().filter(oppgittEgenNæring -> oppgittEgenNæring.getPeriode().equals(periodeMai)).collect(Collectors.toList()).get(0).getBruttoInntekt()).isEqualTo(BigDecimal.TEN);
    }

    @Test
    public void skal_legge_til_tilkommet_periode_for_FL_STANDARD() {
        OppgittOpptjeningBuilder oppgittOpptjening = OppgittOpptjeningBuilder.ny();
        var frilans = OppgittFrilansBuilder.ny();
        var aprilOppdrag = OppgittFrilansOppdragBuilder.ny();
        aprilOppdrag.medInntekt(BigDecimal.TEN).medPeriode(periodeApril);

        var maiOppdrag = OppgittFrilansOppdragBuilder.ny();
        maiOppdrag.medInntekt(BigDecimal.TEN).medPeriode(periodeMai);

        frilans.medFrilansOppdrag(List.of(aprilOppdrag.build(), maiOppdrag.build()));
        oppgittOpptjening.leggTilFrilansOpplysninger(frilans.build());

        var aprilOverstyrt = OppgittFrilansBuilder.ny();

        var overstrytMaiOppdrag = OppgittFrilansOppdragBuilder.ny();
        overstrytMaiOppdrag.medInntekt(BigDecimal.TEN).medPeriode(periodeMai);
        aprilOverstyrt.leggTilFrilansOppdrag(overstrytMaiOppdrag.build());

        OppgittOpptjeningBuilder overstryt = OppgittOpptjeningBuilder.ny();
        overstryt.leggTilFrilansOpplysninger(aprilOverstyrt.build());

        var filter = new OppgittOpptjeningFilter(oppgittOpptjening.build(), overstryt.build());

        var oppgittOpptjeningFrisinn = filter.getOppgittOpptjeningStandard();

        assertThat(oppgittOpptjeningFrisinn).isPresent();
        assertThat(oppgittOpptjeningFrisinn.get().getFrilans().isPresent());
        assertThat(oppgittOpptjeningFrisinn.get().getFrilans().get().getFrilansoppdrag()).hasSize(1);
    }
}


