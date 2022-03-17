package no.nav.k9.sak.domene.opptjening;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningResultat;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektFilter;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.OpptjeningsperioderTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;

/**
 * Henter inntekter, arbeid, og ytelser relevant for opptjening.
 */
@Dependent
public class OpptjeningInntektArbeidYtelseTjeneste {

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private OpptjeningRepository opptjeningRepository;
    private OpptjeningsperioderTjeneste opptjeningsperioderTjeneste;
    private OpptjeningAktivitetVurderingOpptjeningsvilkår vurderForOpptjeningsvilkår;

    OpptjeningInntektArbeidYtelseTjeneste() {
        // for CDI proxy
    }

    @Inject
    public OpptjeningInntektArbeidYtelseTjeneste(InntektArbeidYtelseTjeneste iayTjeneste,
                                                 OpptjeningRepository opptjeningRepository,
                                                 OpptjeningsperioderTjeneste opptjeningsperioderTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.opptjeningRepository = opptjeningRepository;
        this.opptjeningsperioderTjeneste = opptjeningsperioderTjeneste;
        this.vurderForOpptjeningsvilkår = new OpptjeningAktivitetVurderingOpptjeningsvilkår();
    }

    public OpptjeningResultat hentOpptjening(Long behandlingId) {
        Optional<OpptjeningResultat> optional = opptjeningRepository.finnOpptjening(behandlingId);
        return optional
            .orElseThrow(() -> new IllegalStateException("Utvikler-feil: Mangler Opptjening for Behandling: " + behandlingId));
    }

    /**
     * Hent alle inntekter for søker der det finnes arbeidsgiver
     */
    public NavigableMap<LocalDate, List<OpptjeningInntektPeriode>> hentRelevanteOpptjeningInntekterForVilkårVurdering(Long behandlingId, AktørId aktørId, Collection<LocalDate> skjæringstidspunkter) {
        var grunnlagOpt = iayTjeneste.finnGrunnlag(behandlingId);
        if (grunnlagOpt.isEmpty()) {
            return Collections.emptyNavigableMap();
        }
        var iayGrunnlag = grunnlagOpt.get();
        NavigableMap<LocalDate, List<OpptjeningInntektPeriode>> alle = new TreeMap<>();

        for (var stp : new TreeSet<>(skjæringstidspunkter)) {
            var filter = new InntektFilter(iayGrunnlag.getAktørInntektFraRegister(aktørId)).før(stp).filterPensjonsgivende();
            var result = filter.filter((inntekt, inntektspost) -> inntekt.getArbeidsgiver() != null)
                .mapInntektspost((inntekt, inntektspost) -> new OpptjeningInntektPeriode(inntektspost, new Opptjeningsnøkkel(null, inntekt.getArbeidsgiver())));
            alle.put(stp, List.copyOf(result));
        }
        return Collections.unmodifiableNavigableMap(alle);
    }

    public NavigableMap<DatoIntervallEntitet, List<OpptjeningAktivitetPeriode>> hentRelevanteOpptjeningAktiveterForVilkårVurdering(BehandlingReferanse ref,
                                                                                                                                   Collection<DatoIntervallEntitet> vilkårsPerioder) {
        Long behandlingId = ref.getBehandlingId();
        var grunnlagOpt = iayTjeneste.finnGrunnlag(behandlingId);
        if (grunnlagOpt.isEmpty() || vilkårsPerioder.isEmpty()) {
            return Collections.emptyNavigableMap();
        }
        var opptjeningsresultat = opptjeningRepository.finnOpptjening(behandlingId).orElseThrow(() -> new IllegalStateException("Kan ikke finne opptjening for behandling"));
        var iayGrunnlag = grunnlagOpt.get();
        NavigableMap<DatoIntervallEntitet, List<OpptjeningAktivitetPeriode>> alle = new TreeMap<>();

        for (var periode : new TreeSet<>(vilkårsPerioder)) {
            LocalDate stp = periode.getFomDato();
            var opptjening = opptjeningsresultat.finnOpptjening(stp).orElseThrow(() -> new IllegalStateException("Finner ikke opptjening for vilkårsperiode, stp=" + stp));
            var perioderForSaksbehandling = opptjeningsperioderTjeneste.mapPerioderForSaksbehandling(ref, iayGrunnlag, vurderForOpptjeningsvilkår, opptjening.getOpptjeningPeriode(), periode);
            var opptjeningAktivitetPerioder = perioderForSaksbehandling.stream().map(this::mapTilPerioder).collect(Collectors.toList());
            alle.put(periode, opptjeningAktivitetPerioder);
        }


        return Collections.unmodifiableNavigableMap(alle);
    }

    private OpptjeningAktivitetPeriode mapTilPerioder(OpptjeningsperiodeForSaksbehandling periode) {
        final OpptjeningAktivitetPeriode.Builder builder = OpptjeningAktivitetPeriode.Builder.ny();
        builder.medPeriode(periode.getPeriode())
            .medOpptjeningAktivitetType(periode.getOpptjeningAktivitetType())
            .medOrgnr(periode.getOrgnr())
            .medOpptjeningsnøkkel(periode.getOpptjeningsnøkkel())
            .medStillingsandel(periode.getStillingsprosent())
            .medVurderingsStatus(periode.getVurderingsStatus())
            .medBegrunnelse(periode.getBegrunnelse());
        return builder.build();
    }

}
