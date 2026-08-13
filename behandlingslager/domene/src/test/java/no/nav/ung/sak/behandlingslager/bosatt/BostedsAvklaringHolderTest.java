package no.nav.ung.sak.behandlingslager.bosatt;

import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BostedsAvklaringHolderTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 1, 31);
    private static final LocalDateTime VURDERT_TIDSPUNKT = LocalDateTime.of(2026, 1, 15, 10, 0);

    @Test
    void skal_erstatte_avklaring_under_arbeid_uten_a_splitte_den() {
        var holder = new BostedsAvklaringHolder();
        holder.leggTilEllerErstattPeriodeAvklaringerUnderArbeid(List.of(lagAvklaring(FOM, TOM)));

        var ny = lagAvklaring(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 20));
        holder.leggTilEllerErstattPeriodeAvklaringerUnderArbeid(List.of(ny));

        assertThat(holder.getPeriodeAvklaringer()).hasSize(1);
        assertThat(holder.getPeriodeAvklaringer().iterator().next().getReferanse()).isEqualTo(ny.getReferanse());
    }

    @Test
    void skal_ha_unik_referanse_blant_avklaringene_under_arbeid() {
        var holder = new BostedsAvklaringHolder();
        holder.leggTilEllerErstattPeriodeAvklaringerUnderArbeid(List.of(
            lagAvklaring(FOM, LocalDate.of(2026, 1, 15)),
            lagAvklaring(LocalDate.of(2026, 1, 16), TOM)));

        var referanser = holder.hentAvklaringerMedStatus(AvklaringStatus.AVKLARES).stream()
            .map(BostedsPeriodeAvklaring::getReferanse)
            .toList();

        assertThat(referanser).doesNotHaveDuplicates();
    }

    @Test
    void skal_beholde_ferdigstilte_avklaringer_nar_nye_avklaringer_under_arbeid_lagres() {
        var holder = new BostedsAvklaringHolder();
        var ferdigstilt = lagAvklaring(FOM, LocalDate.of(2026, 1, 15));
        holder.leggTilEllerErstattPeriodeAvklaringerUnderArbeid(List.of(ferdigstilt));
        holder.settAlleAvklaringerTilFerdig();

        holder.leggTilEllerErstattPeriodeAvklaringerUnderArbeid(List.of(lagAvklaring(LocalDate.of(2026, 1, 16), TOM)));

        assertThat(holder.hentAvklaringerMedStatus(AvklaringStatus.FERDIG))
            .extracting(BostedsPeriodeAvklaring::getReferanse)
            .containsExactly(ferdigstilt.getReferanse());
        assertThat(holder.hentAvklaringerMedStatus(AvklaringStatus.AVKLARES)).hasSize(1);
    }

    @Test
    void skal_beholde_referansen_pa_begge_segmenter_nar_ferdigstilt_avklaring_splittes() {
        var holder = new BostedsAvklaringHolder();
        var opprinnelig = lagAvklaring(FOM, TOM);
        holder.leggTilEllerErstattPeriodeAvklaringerUnderArbeid(List.of(opprinnelig));
        holder.settAlleAvklaringerTilFerdig();

        var overlappende = lagAvklaring(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 20));
        holder.leggTilEllerErstattPeriodeAvklaringerUnderArbeid(List.of(overlappende));
        holder.settAlleAvklaringerTilFerdig();

        var ferdigstilte = holder.hentAvklaringerMedStatus(AvklaringStatus.FERDIG).stream()
            .sorted(Comparator.comparing(a -> a.getPeriode().getFomDato()))
            .toList();

        // Ferdigstilte avklaringer splittes, og segmentene deler referanse for sporbarhet mot
        // varselet som ble sendt. Det etterlyses aldri uttalelse på en ferdigstilt avklaring.
        assertThat(ferdigstilte).hasSize(3);
        assertThat(ferdigstilte.get(0).getReferanse()).isEqualTo(opprinnelig.getReferanse());
        assertThat(ferdigstilte.get(1).getReferanse()).isEqualTo(overlappende.getReferanse());
        assertThat(ferdigstilte.get(2).getReferanse()).isEqualTo(opprinnelig.getReferanse());
    }

    private static BostedsPeriodeAvklaring lagAvklaring(LocalDate fom, LocalDate tom) {
        return new BostedsPeriodeAvklaring(
            DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom),
            BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM,
            "begrunnelse",
            false,
            null,
            "begrunnelse for hvorfor det ikke varsles",
            "saksbehandler1",
            VURDERT_TIDSPUNKT,
            Avklaringtype.AVSLAG);
    }
}
