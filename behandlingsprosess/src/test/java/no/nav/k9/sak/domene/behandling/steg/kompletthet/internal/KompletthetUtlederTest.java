package no.nav.k9.sak.domene.behandling.steg.kompletthet.internal;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.beregningsgrunnlag.kompletthet.Vurdering;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.ytelse.beregning.grunnlag.KompletthetPeriode;

class KompletthetUtlederTest {

    private KompletthetUtleder utleder = new KompletthetUtleder();

    @Test
    void skal_fortsette_hvis_komplett() {
        var perioderTilVurdering = new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now())));
        Map<DatoIntervallEntitet, List<ManglendeVedlegg>> manglendeVedleggPerPeriode = Map.of();
        var vurderingDetSkalTasHensynTil = Set.of(Vurdering.KAN_FORTSETTE);

        var input = new VurdererInput(perioderTilVurdering, perioderTilVurdering, manglendeVedleggPerPeriode, null, vurderingDetSkalTasHensynTil);

        var aksjon = utleder.utled(input);

        assertThat(aksjon).isNotNull();
        assertThat(aksjon.kanFortsette()).isTrue();
    }

    @Test
    void skal_gi_uavklart_hvis_ikke_komplett() {
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(1), LocalDate.now());
        var perioderTilVurdering = new TreeSet<>(Set.of(periode));
        Map<DatoIntervallEntitet, List<ManglendeVedlegg>> manglendeVedleggPerPeriode = Map.of(periode, List.of(new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, Arbeidsgiver.virksomhet("000000000"))));
        var vurderingDetSkalTasHensynTil = Set.of(Vurdering.KAN_FORTSETTE);

        var input = new VurdererInput(perioderTilVurdering, perioderTilVurdering, manglendeVedleggPerPeriode, null, vurderingDetSkalTasHensynTil);

        var aksjon = utleder.utled(input);

        assertThat(aksjon).isNotNull();
        assertThat(aksjon.kanFortsette()).isFalse();
        assertThat(aksjon.erUavklart()).isTrue();
    }

    @Test
    void skal_gi_fortsett_hvis_ikke_komplett_men_tatt_stilling_til() {
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(1), LocalDate.now());
        var perioderTilVurdering = new TreeSet<>(Set.of(periode));
        Map<DatoIntervallEntitet, List<ManglendeVedlegg>> manglendeVedleggPerPeriode = Map.of(periode, List.of(new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, Arbeidsgiver.virksomhet("000000000"))));
        var vurderingDetSkalTasHensynTil = Set.of(Vurdering.KAN_FORTSETTE);

        var input = new VurdererInput(perioderTilVurdering, perioderTilVurdering, manglendeVedleggPerPeriode, List.of(new KompletthetPeriode(Vurdering.KAN_FORTSETTE, periode.getFomDato(), "asdf")), vurderingDetSkalTasHensynTil);

        var aksjon = utleder.utled(input);

        assertThat(aksjon).isNotNull();
        assertThat(aksjon.kanFortsette()).isTrue();
        assertThat(aksjon.erUavklart()).isFalse();
    }

    @Test
    void skal_gi_uavklart_hvis_ikke_komplett_men_tatt_stilling_til_og_manuell_revurdering_uten_aksjonspunkt_tidligere() {
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(1), LocalDate.now());
        var perioderTilVurdering = new TreeSet<>(Set.of(periode));
        Map<DatoIntervallEntitet, List<ManglendeVedlegg>> manglendeVedleggPerPeriode = Map.of(periode, List.of(new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, Arbeidsgiver.virksomhet("000000000"))));
        var vurderingDetSkalTasHensynTil = Set.of(Vurdering.KAN_FORTSETTE);

        var input = new VurdererInput(true, true,perioderTilVurdering, perioderTilVurdering, manglendeVedleggPerPeriode, List.of(new KompletthetPeriode(Vurdering.KAN_FORTSETTE, periode.getFomDato(), "asdf")), vurderingDetSkalTasHensynTil);

        var aksjon = utleder.utled(input);

        assertThat(aksjon).isNotNull();
        assertThat(aksjon.kanFortsette()).isFalse();
        assertThat(aksjon.erUavklart()).isTrue();
    }

    @Test
    void skal_ikke_gi_uavklart_hvis_ikke_komplett_men_avslått_på_søknadsfirst() {
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(1), LocalDate.now());
        var perioderTilVurdering = new TreeSet<>(Set.of(periode));
        Map<DatoIntervallEntitet, List<ManglendeVedlegg>> manglendeVedleggPerPeriode = Map.of(periode, List.of(new ManglendeVedlegg(DokumentTypeId.INNTEKTSMELDING, Arbeidsgiver.virksomhet("000000000"))));
        var vurderingDetSkalTasHensynTil = Set.of(Vurdering.KAN_FORTSETTE);

        var input = new VurdererInput(perioderTilVurdering, new TreeSet<>(), manglendeVedleggPerPeriode, null, vurderingDetSkalTasHensynTil);

        var aksjon = utleder.utled(input);

        assertThat(aksjon).isNotNull();
        assertThat(aksjon.kanFortsette()).isTrue();
    }
}
