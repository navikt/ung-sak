package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.behandlingslager.behandling.søknadsfrist.AvklartKravDokument;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;

class DefaultSøknadsfristPeriodeVurdererTest {

    private final DefaultSøknadsfristPeriodeVurderer vurderer = new DefaultSøknadsfristPeriodeVurderer();

    @Test
    void skal_avlså_etter_søknadsfrist() {
        var journalpostId = new JournalpostId("1");
        var kravDokument = new KravDokument(journalpostId, LocalDateTime.now(), KravDokumentType.SØKNAD);
        var søktPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(6), LocalDate.now().minusMonths(1));
        var søktePerioder = List.of(new SøktPeriode<>(søktPeriode, new Søknadsperiode(søktPeriode)));
        var timeline = new LocalDateTimeline<>(søktePerioder.stream().map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), it)).collect(Collectors.toList()));

        var vurderteSegmenter = vurderer.vurderPeriode(kravDokument, timeline, Optional.empty());

        assertThat(vurderteSegmenter.toSegments().stream().anyMatch(it -> Utfall.IKKE_VURDERT.equals(it.getValue().getUtfall()))).isTrue();
        var avklartKravDokument = new AvklartKravDokument(journalpostId, true, LocalDate.now().minusMonths(5));

        var avklarteVurderteSegmenter = vurderer.vurderPeriode(kravDokument, timeline, Optional.of(avklartKravDokument));

        assertThat(avklarteVurderteSegmenter.toSegments().stream().anyMatch(it -> Utfall.IKKE_VURDERT.equals(it.getValue().getUtfall()))).isFalse();
        assertThat(avklarteVurderteSegmenter.toSegments().stream().anyMatch(it -> Utfall.IKKE_OPPFYLT.equals(it.getValue().getUtfall()))).isTrue();
    }
}
