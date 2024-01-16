package no.nav.k9.sak.domene.behandling.steg.kompletthet.internal;

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

    private boolean erManueltOpprettetRevurdering;
    private boolean harIkkeFåttMulighetTilÅTaStillingPåNytt;
    private NavigableSet<DatoIntervallEntitet> perioderTilVurdering;
    private NavigableSet<DatoIntervallEntitet> perioderTilVurderingMedSøknadsfristOppfylt;
    private Map<DatoIntervallEntitet, List<ManglendeVedlegg>> manglendeVedleggPerPeriode;
    private List<KompletthetPeriode> kompletthetPerioder;
    private Set<Vurdering> vurderingDetSkalTasHensynTil;

    private boolean utvidetLogging;

    VurdererInput(NavigableSet<DatoIntervallEntitet> perioderTilVurdering,
                  NavigableSet<DatoIntervallEntitet> perioderTilVurderingMedSøknadsfristOppfylt,
                  Map<DatoIntervallEntitet, List<ManglendeVedlegg>> manglendeVedleggPerPeriode,
                  List<KompletthetPeriode> kompletthetPerioder,
                  Set<Vurdering> vurderingDetSkalTasHensynTil) {
        this(false, false, perioderTilVurdering, perioderTilVurderingMedSøknadsfristOppfylt, manglendeVedleggPerPeriode, kompletthetPerioder, vurderingDetSkalTasHensynTil);
    }

    VurdererInput(NavigableSet<DatoIntervallEntitet> perioderTilVurdering,
                  NavigableSet<DatoIntervallEntitet> perioderTilVurderingMedSøknadsfristOppfylt,
                  Map<DatoIntervallEntitet, List<ManglendeVedlegg>> kompletthetsVurderinger) {
        this(perioderTilVurdering, perioderTilVurderingMedSøknadsfristOppfylt, kompletthetsVurderinger, null, null);
    }

    public VurdererInput(boolean erManueltOpprettetRevurdering, boolean harIkkeFåttMulighetTilÅTaStillingPåNytt, NavigableSet<DatoIntervallEntitet> perioderTilVurdering,
                         NavigableSet<DatoIntervallEntitet> perioderTilVurderingMedSøknadsfristOppfylt,
                         Map<DatoIntervallEntitet, List<ManglendeVedlegg>> manglendeVedleggPerPeriode,
                         List<KompletthetPeriode> kompletthetPerioder,
                         Set<Vurdering> vurderingDetSkalTasHensynTil) {
        this.erManueltOpprettetRevurdering = erManueltOpprettetRevurdering;
        this.harIkkeFåttMulighetTilÅTaStillingPåNytt = harIkkeFåttMulighetTilÅTaStillingPåNytt;
        this.perioderTilVurdering = Objects.requireNonNull(perioderTilVurdering);
        this.perioderTilVurderingMedSøknadsfristOppfylt = perioderTilVurderingMedSøknadsfristOppfylt;
        this.manglendeVedleggPerPeriode = Objects.requireNonNull(manglendeVedleggPerPeriode);
        this.kompletthetPerioder = kompletthetPerioder != null ? kompletthetPerioder : List.of();
        this.vurderingDetSkalTasHensynTil = vurderingDetSkalTasHensynTil != null ? vurderingDetSkalTasHensynTil : Set.of();
    }

    NavigableSet<DatoIntervallEntitet> getPerioderTilVurdering() {
        return perioderTilVurdering;
    }

    Map<DatoIntervallEntitet, List<ManglendeVedlegg>> getManglendeVedleggPerPeriode() {
        return manglendeVedleggPerPeriode;
    }

    NavigableSet<DatoIntervallEntitet> getPerioderTilVurderingMedSøknadsfristOppfylt() {
        return perioderTilVurderingMedSøknadsfristOppfylt;
    }

    List<KompletthetPeriode> getKompletthetsPerioder() {
        return kompletthetPerioder;
    }

    Set<Vurdering> getVurderingDetSkalTasHensynTil() {
        return vurderingDetSkalTasHensynTil;
    }

    public boolean erManueltOpprettetRevurdering() {
        return erManueltOpprettetRevurdering;
    }

    public boolean harIkkeFåttMulighetTilÅTaStillingPåNytt() {
        return harIkkeFåttMulighetTilÅTaStillingPåNytt;
    }

    public boolean getUtvidetLogging() {
        return utvidetLogging;
    }

    public void setUtvidetLogging(boolean utvidetLogging) {
        this.utvidetLogging = utvidetLogging;
    }
}
