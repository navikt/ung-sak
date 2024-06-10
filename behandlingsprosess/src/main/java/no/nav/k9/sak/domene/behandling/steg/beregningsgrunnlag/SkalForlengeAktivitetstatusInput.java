package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.util.List;
import java.util.NavigableSet;
import java.util.Set;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.ytelse.beregning.grunnlag.KompletthetPeriode;

public record SkalForlengeAktivitetstatusInput(
    BehandlingReferanse behandlingReferanse,
    BehandlingReferanse originalBehandlingReferanse,
    Set<Inntektsmelding> inntektsmeldinger,
    List<MottattDokument> mottatteInntektsmeldinger,
    NavigableSet<DatoIntervallEntitet> perioderForRevurderingAvBeregningFraProsesstrigger,
    NavigableSet<PeriodeTilVurdering> perioderTilVurderingIOpptjening,
    NavigableSet<PeriodeTilVurdering> perioderTilVurderingIBeregning,
    NavigableSet<DatoIntervallEntitet> innvilgedePerioderForrigeBehandling,
    Set<KompletthetPeriode> gjeldendeKompletthetsvurdering,
    Set<KompletthetPeriode> forrigeKompletthetsvurdering) {
}
