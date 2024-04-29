package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;


import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

/** Interface for å plugge inn ytelsespesifikk utregning av opptjeningaktiviteteter. */
public interface OpptjeningForBeregningTjeneste {

    static OpptjeningForBeregningTjeneste finnTjeneste(Instance<OpptjeningForBeregningTjeneste> instances, FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(OpptjeningForBeregningTjeneste.class, instances, ytelseType)
            .orElseThrow(() -> new IllegalStateException("Har ikke tjeneste for ytelseType=" + ytelseType));
    }


    Optional<OppgittOpptjening> finnOppgittOpptjening(BehandlingReferanse referanse, InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDate stp);

    Optional<OpptjeningAktiviteter> hentEksaktOpptjeningForBeregning(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag, DatoIntervallEntitet vilkårsperiode);

}
