package no.nav.folketrygdloven.beregningsgrunnlag.felles;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.felles.BeregningUtils;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.vedtak.felles.jpa.tid.DatoIntervallEntitet;

public class BeregningUtilsTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2019,1,1);
    private static final RelatertYtelseType AAP = RelatertYtelseType.ARBEIDSAVKLARINGSPENGER;
    @Test
    public void skal_finne_ytelse_med_korrekt_ytelsetype() {
        Ytelse aapYtelse = lagYtelse(AAP, SKJÆRINGSTIDSPUNKT.minusMonths(2), SKJÆRINGSTIDSPUNKT.minusMonths(1)).build();
        Ytelse dpYtelse = lagYtelse(RelatertYtelseType.DAGPENGER, SKJÆRINGSTIDSPUNKT.minusMonths(1), SKJÆRINGSTIDSPUNKT.minusDays(1)).build();

        YtelseFilter filter = new YtelseFilter(Arrays.asList(aapYtelse, dpYtelse));

        Optional<Ytelse> ytelse = BeregningUtils.sisteVedtakFørStpForType(filter, SKJÆRINGSTIDSPUNKT, Set.of(AAP));

        assertThat(ytelse).isPresent();
        assertThat(ytelse.get()).isEqualTo(aapYtelse);
    }

    @Test
    public void skal_finne_ytelse_med_vedtak_nærmest_skjæringstidspunkt() {
        Ytelse aapYtelseGammel = lagYtelse(AAP, SKJÆRINGSTIDSPUNKT.minusMonths(2), SKJÆRINGSTIDSPUNKT.minusMonths(1)).build();
        Ytelse aapYtelseNy = lagYtelse(AAP, SKJÆRINGSTIDSPUNKT.minusMonths(1), SKJÆRINGSTIDSPUNKT.minusDays(1)).build();

        YtelseFilter filter = new YtelseFilter(Arrays.asList(aapYtelseNy, aapYtelseGammel));

        Optional<Ytelse> ytelse = BeregningUtils.sisteVedtakFørStpForType(filter, SKJÆRINGSTIDSPUNKT, Set.of(AAP));

        assertThat(ytelse).isPresent();
        assertThat(ytelse.get()).isEqualTo(aapYtelseNy);
    }

    @Test
    public void skal_ikke_ta_med_vedtak_med_fom_etter_skjæringstidspunkt() {
        Ytelse aapYtelseGammel = lagYtelse(AAP, SKJÆRINGSTIDSPUNKT.minusMonths(2), SKJÆRINGSTIDSPUNKT.minusMonths(1)).build();
        Ytelse aapYtelseNy = lagYtelse(AAP, SKJÆRINGSTIDSPUNKT.minusMonths(1), SKJÆRINGSTIDSPUNKT.minusDays(15)).build();
        Ytelse aapYtelseEtterStp = lagYtelse(AAP, SKJÆRINGSTIDSPUNKT.plusDays(1), SKJÆRINGSTIDSPUNKT.plusMonths(1)).build();

        YtelseFilter filter = new YtelseFilter(Arrays.asList(aapYtelseNy, aapYtelseGammel, aapYtelseEtterStp));

        Optional<Ytelse> ytelse = BeregningUtils.sisteVedtakFørStpForType(filter, SKJÆRINGSTIDSPUNKT, Set.of(AAP));

        assertThat(ytelse).isPresent();
        assertThat(ytelse.get()).isEqualTo(aapYtelseNy);
    }

    @Test
    public void skal_finne_korrekt_meldekort_når_det_tilhører_nyeste_vedtak() {
        YtelseBuilder aapGammelBuilder = lagYtelse(AAP, SKJÆRINGSTIDSPUNKT.minusMonths(2), SKJÆRINGSTIDSPUNKT.minusMonths(1));
        YtelseBuilder aapYtelseNyBuilder = lagYtelse(AAP, SKJÆRINGSTIDSPUNKT.minusMonths(1), SKJÆRINGSTIDSPUNKT.minusDays(15));

        YtelseAnvist nyttMeldekort = lagMeldekort(BigDecimal.valueOf(100), SKJÆRINGSTIDSPUNKT.minusDays(45), SKJÆRINGSTIDSPUNKT.minusDays(31));
        YtelseAnvist gammeltMeldekort = lagMeldekort(BigDecimal.valueOf(100), SKJÆRINGSTIDSPUNKT.minusDays(60), SKJÆRINGSTIDSPUNKT.minusDays(46));

        Ytelse gammelYtelse = aapGammelBuilder.medYtelseAnvist(gammeltMeldekort).build();
        Ytelse nyYtelse = aapYtelseNyBuilder.medYtelseAnvist(nyttMeldekort).build();

        YtelseFilter filter = new YtelseFilter(Arrays.asList(gammelYtelse, nyYtelse));
        Optional<YtelseAnvist> ytelseAnvist = BeregningUtils.sisteHeleMeldekortFørStp(filter, nyYtelse, SKJÆRINGSTIDSPUNKT, Set.of(AAP));

        assertThat(ytelseAnvist).isPresent();
        assertThat(ytelseAnvist.get()).isEqualTo(nyttMeldekort);
    }

    @Test
    public void skal_finne_korrekt_meldekort_når_det_tilhører_eldste_vedtak() {
        YtelseBuilder aapGammelBuilder = lagYtelse(AAP, SKJÆRINGSTIDSPUNKT.minusMonths(2), SKJÆRINGSTIDSPUNKT.minusMonths(1));
        YtelseBuilder aapYtelseNyBuilder = lagYtelse(AAP, SKJÆRINGSTIDSPUNKT.minusMonths(1), SKJÆRINGSTIDSPUNKT.minusDays(15));

        YtelseAnvist nyttMeldekort = lagMeldekort(BigDecimal.valueOf(100), SKJÆRINGSTIDSPUNKT.minusDays(45), SKJÆRINGSTIDSPUNKT.minusDays(31));
        YtelseAnvist gammeltMeldekort = lagMeldekort(BigDecimal.valueOf(100), SKJÆRINGSTIDSPUNKT.minusDays(60), SKJÆRINGSTIDSPUNKT.minusDays(46));

        Ytelse gammelYtelse = aapGammelBuilder.medYtelseAnvist(nyttMeldekort).build();
        Ytelse nyYtelse = aapYtelseNyBuilder.medYtelseAnvist(gammeltMeldekort).build();

        YtelseFilter filter = new YtelseFilter(Arrays.asList(gammelYtelse, nyYtelse));
        Optional<YtelseAnvist> ytelseAnvist = BeregningUtils.sisteHeleMeldekortFørStp(filter, nyYtelse, SKJÆRINGSTIDSPUNKT, Set.of(AAP));

        assertThat(ytelseAnvist).isPresent();
        assertThat(ytelseAnvist.get()).isEqualTo(nyttMeldekort);
    }

    @Test
    public void skal_finne_meldekort_fra_nyeste_vedtak_når_to_vedtak_har_meldekort_med_samme_periode() {
        YtelseBuilder aapGammelBuilder = lagYtelse(AAP, SKJÆRINGSTIDSPUNKT.minusMonths(2), SKJÆRINGSTIDSPUNKT.minusMonths(1));
        YtelseBuilder aapYtelseNyBuilder = lagYtelse(AAP, SKJÆRINGSTIDSPUNKT.minusMonths(1), SKJÆRINGSTIDSPUNKT.minusDays(15));

        YtelseAnvist meldekortHundre = lagMeldekort(BigDecimal.valueOf(100), SKJÆRINGSTIDSPUNKT.minusDays(45), SKJÆRINGSTIDSPUNKT.minusDays(31));
        YtelseAnvist meldekortFemti = lagMeldekort(BigDecimal.valueOf(50), SKJÆRINGSTIDSPUNKT.minusDays(45), SKJÆRINGSTIDSPUNKT.minusDays(31));

        Ytelse gammelYtelse = aapGammelBuilder.medYtelseAnvist(meldekortHundre).build();
        Ytelse nyYtelse = aapYtelseNyBuilder.medYtelseAnvist(meldekortFemti).build();

        YtelseFilter filter = new YtelseFilter(Arrays.asList(gammelYtelse, nyYtelse));
        Optional<YtelseAnvist> ytelseAnvist = BeregningUtils.sisteHeleMeldekortFørStp(filter, nyYtelse, SKJÆRINGSTIDSPUNKT, Set.of(AAP));

        assertThat(ytelseAnvist).isPresent();
        assertThat(ytelseAnvist.get()).isEqualTo(meldekortFemti);
    }

    @Test
    public void skal_ikke_ta_med_meldekort_fra_vedtak_etter_stp() {
        YtelseBuilder aapGammelBuilder = lagYtelse(AAP, SKJÆRINGSTIDSPUNKT.minusMonths(2), SKJÆRINGSTIDSPUNKT.minusMonths(1));
        YtelseBuilder aapYtelseNyBuilder = lagYtelse(AAP, SKJÆRINGSTIDSPUNKT.minusMonths(1), SKJÆRINGSTIDSPUNKT.minusDays(15));
        YtelseBuilder aapYtelseEtterStpBuilder = lagYtelse(AAP, SKJÆRINGSTIDSPUNKT.minusMonths(1), SKJÆRINGSTIDSPUNKT.minusDays(15));

        YtelseAnvist meldekortGammel = lagMeldekort(BigDecimal.valueOf(50), SKJÆRINGSTIDSPUNKT.minusDays(45), SKJÆRINGSTIDSPUNKT.minusDays(31));
        YtelseAnvist meldekortNytt = lagMeldekort(BigDecimal.valueOf(50), SKJÆRINGSTIDSPUNKT.minusDays(30), SKJÆRINGSTIDSPUNKT.minusDays(16));
        YtelseAnvist meldekortNyest = lagMeldekort(BigDecimal.valueOf(50), SKJÆRINGSTIDSPUNKT.minusDays(2), SKJÆRINGSTIDSPUNKT.plusDays(12));

        Ytelse gammelYtelse = aapGammelBuilder.medYtelseAnvist(meldekortGammel).build();
        Ytelse nyYtelse = aapYtelseNyBuilder.medYtelseAnvist(meldekortNytt).build();
        Ytelse ytelseEtterStp = aapYtelseEtterStpBuilder.medYtelseAnvist(meldekortNyest).build();

        YtelseFilter filter = new YtelseFilter(Arrays.asList(gammelYtelse, nyYtelse, ytelseEtterStp));
        Optional<YtelseAnvist> ytelseAnvist = BeregningUtils.sisteHeleMeldekortFørStp(filter, nyYtelse, SKJÆRINGSTIDSPUNKT, Set.of(AAP));

        assertThat(ytelseAnvist).isPresent();
        assertThat(ytelseAnvist.get()).isEqualTo(meldekortNytt);
    }


    private YtelseAnvist lagMeldekort(BigDecimal utbetalingsgrad, LocalDate fom, LocalDate tom) {
        return YtelseBuilder.oppdatere(Optional.empty()).getAnvistBuilder()
            .medAnvistPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medUtbetalingsgradProsent(utbetalingsgrad).build();
    }


    private YtelseBuilder lagYtelse(RelatertYtelseType ytelsetype, LocalDate fom, LocalDate tom) {
        return YtelseBuilder.oppdatere(Optional.empty())
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
                .medYtelseType(ytelsetype)
                .medStatus(RelatertYtelseTilstand.AVSLUTTET);
    }


}
