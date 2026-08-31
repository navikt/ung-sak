package no.nav.ung.sak.behandlingslager.vilkårsavklaring;

import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VilkårAvklaringHolderTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 1, 31);
    private static final LocalDateTime VURDERT_TIDSPUNKT = LocalDateTime.of(2026, 1, 15, 10, 0);

    @Test
    void skal_beholde_ferdigstilte_avklaringer_uendret_nar_ferdigstilling_gjores_pa_nytt_uten_endring() {
        var holder = new VilkårAvklaringHolder();
        var foreslått = lagAvklaring(FOM, TOM);
        holder.ferdigstillAvklaringer(List.of(foreslått));

        var ferdigstiltFørsteGang = holder.hentFerdigstilteAvklaringer().iterator().next();

        // Idempotent: ferdigstiller samme avklaring på nytt uten at noe splittes eller endres
        holder.ferdigstillAvklaringer(List.of(foreslått));

        assertThat(holder.hentFerdigstilteAvklaringer()).hasSize(1);
        var ferdigstiltAndreGang = holder.hentFerdigstilteAvklaringer().iterator().next();
        assertThat(ferdigstiltAndreGang.getReferanse()).isEqualTo(ferdigstiltFørsteGang.getReferanse());
    }

    @Test
    void skal_splitte_overlappende_ferdigstilte_avklaringer_og_beholde_referanse_pa_begge_segmenter() {
        var holder = new VilkårAvklaringHolder();
        var opprinnelig = lagAvklaring(FOM, TOM);
        holder.ferdigstillAvklaringer(List.of(opprinnelig));

        var overlappende = lagAvklaring(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 20));
        holder.ferdigstillAvklaringer(List.of(overlappende));

        var ferdigstilte = holder.hentFerdigstilteAvklaringer().stream()
            .sorted(Comparator.comparing(a -> a.getPeriode().getFomDato()))
            .toList();

        // Ferdigstilte avklaringer splittes, og segmentene deler referanse for sporbarhet mot varselet som ble
        // sendt. Det etterlyses aldri uttalelse på en ferdigstilt avklaring.
        assertThat(ferdigstilte).hasSize(3);
        assertThat(ferdigstilte.get(0).getReferanse()).isEqualTo(opprinnelig.getReferanse());
        assertThat(ferdigstilte.get(1).getReferanse()).isEqualTo(overlappende.getReferanse());
        assertThat(ferdigstilte.get(2).getReferanse()).isEqualTo(opprinnelig.getReferanse());
    }

    @Test
    void kopi_skal_gi_en_ny_instans_med_likt_innhold() {
        var holder = new VilkårAvklaringHolder();
        var ferdigstilt = lagAvklaring(FOM, TOM);
        holder.ferdigstillAvklaringer(List.of(ferdigstilt));

        var kopi = VilkårAvklaringHolder.lagKopi(holder);

        assertThat(kopi).isNotSameAs(holder);
        assertThat(kopi).isEqualTo(holder);
        assertThat(kopi.hentFerdigstilteAvklaringer())
            .extracting(VilkårPeriodeAvklaring::getReferanse)
            .containsExactly(ferdigstilt.getReferanse());
    }

    @Test
    void lagKopi_av_null_skal_gi_tom_holder() {
        var kopi = VilkårAvklaringHolder.lagKopi(null);

        assertThat(kopi.hentFerdigstilteAvklaringer()).isEmpty();
    }

    private static VilkårPeriodeAvklaringForeslått lagAvklaring(LocalDate fom, LocalDate tom) {
        return new VilkårPeriodeAvklaringForeslått(
            DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom),
            "IKKE_OPPFYLT_KODE",
            "begrunnelse",
            false,
            null,
            "begrunnelse for hvorfor det ikke varsles",
            "saksbehandler1",
            VURDERT_TIDSPUNKT,
            Avklaringtype.AVSLAG);
    }
}
