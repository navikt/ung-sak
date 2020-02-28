package no.nav.foreldrepenger.domene.medlem;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;

public class MedlemEndringIdentifisererTest {

    @Test
    public void skal_indikere_endring_før_stp() {
        MedlemskapAggregat aggregat = new MedlemskapAggregat(Collections.emptySet(), null, null);
        final HashSet<MedlemskapPerioderEntitet> set = new HashSet<>();
        final MedlemskapPerioderBuilder builder = new MedlemskapPerioderBuilder();
        builder.medPeriode(LocalDate.now().minusMonths(5), LocalDate.now().plusMonths(5));
        set.add(builder.build());
        MedlemskapAggregat aggregat1 = new MedlemskapAggregat(set, null, null);
        final MedlemEndringIdentifiserer identifiserer = new MedlemEndringIdentifiserer();

        assertThat(identifiserer.erEndretFørSkjæringstidspunkt(aggregat, aggregat1, LocalDate.now())).isTrue();
    }

    @Test
    public void skal_indikere_endring_etter_stp() {
        final HashSet<MedlemskapPerioderEntitet> set = new HashSet<>();
        final MedlemskapPerioderBuilder builder = new MedlemskapPerioderBuilder();
        builder.medPeriode(LocalDate.now().minusMonths(5), LocalDate.now().plusMonths(5));
        set.add(builder.build());
        MedlemskapAggregat aggregat = new MedlemskapAggregat(set, null, null);
        final HashSet<MedlemskapPerioderEntitet> setEtter = new HashSet<>();
        final MedlemskapPerioderBuilder builder1 = new MedlemskapPerioderBuilder();
        builder1.medPeriode(LocalDate.now().plusMonths(5), LocalDate.now().plusMonths(15));
        setEtter.add(builder.build());
        setEtter.add(builder1.build());
        MedlemskapAggregat aggregat1 = new MedlemskapAggregat(setEtter, null, null);
        final MedlemEndringIdentifiserer identifiserer = new MedlemEndringIdentifiserer();

        assertThat(identifiserer.erEndretFørSkjæringstidspunkt(aggregat, aggregat1, LocalDate.now())).isFalse();
    }
}
