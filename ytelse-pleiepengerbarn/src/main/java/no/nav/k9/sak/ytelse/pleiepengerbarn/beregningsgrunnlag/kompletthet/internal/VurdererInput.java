package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet.internal;

import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;

import no.nav.k9.kodeverk.beregningsgrunnlag.kompletthet.Vurdering;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.ytelse.beregning.grunnlag.KompletthetPeriode;

class VurdererInput {

    private NavigableSet<DatoIntervallEntitet> perioderTilVurdering;
    private Map<DatoIntervallEntitet, List<ManglendeVedlegg>> manglendeVedleggPerPeriode;
    private List<KompletthetPeriode> kompletthetPerioder;
    private Set<Vurdering> vurderingDetSkalTasHensynTil;

    VurdererInput(NavigableSet<DatoIntervallEntitet> perioderTilVurdering,
                         Map<DatoIntervallEntitet, List<ManglendeVedlegg>> manglendeVedleggPerPeriode,
                         List<KompletthetPeriode> kompletthetPerioder,
                         Set<Vurdering> vurderingDetSkalTasHensynTil) {
        this.perioderTilVurdering = Objects.requireNonNull(perioderTilVurdering);
        this.manglendeVedleggPerPeriode = Objects.requireNonNull(manglendeVedleggPerPeriode);
        this.kompletthetPerioder = kompletthetPerioder != null ? kompletthetPerioder : List.of();
        this.vurderingDetSkalTasHensynTil = vurderingDetSkalTasHensynTil != null ? vurderingDetSkalTasHensynTil : Set.of();
    }

    VurdererInput(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Map<DatoIntervallEntitet, List<ManglendeVedlegg>> kompletthetsVurderinger) {
        this(perioderTilVurdering, kompletthetsVurderinger, null, null);
    }

    NavigableSet<DatoIntervallEntitet> getPerioderTilVurdering() {
        return perioderTilVurdering;
    }

    Map<DatoIntervallEntitet, List<ManglendeVedlegg>> getManglendeVedleggPerPeriode() {
        return manglendeVedleggPerPeriode;
    }

    List<KompletthetPeriode> getKompletthetsPerioder() {
        return kompletthetPerioder;
    }

    Set<Vurdering> getVurderingDetSkalTasHensynTil() {
        return vurderingDetSkalTasHensynTil;
    }
}
