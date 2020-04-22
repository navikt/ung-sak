package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;

import no.nav.abakus.iaygrunnlag.IayGrunnlagJsonMapper;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningAktiviteter.OpptjeningPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningForBeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningsaktiviteterPerYtelse;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.abakus.mapping.IAYTilDtoMapper;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetVurderingBeregning;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.OpptjeningsperioderUtenOverstyringTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@FagsakYtelseTypeRef("FRISINN")
@ApplicationScoped
public class FrisinnOpptjeningForBeregningTjeneste implements OpptjeningForBeregningTjeneste {

    private OpptjeningAktivitetVurderingBeregning vurderOpptjening = new OpptjeningAktivitetVurderingBeregning();
    private OpptjeningsperioderUtenOverstyringTjeneste opptjeningsperioderTjeneste;

    private OpptjeningsaktiviteterPerYtelse opptjeningsaktiviteter = new OpptjeningsaktiviteterPerYtelse(Set.of(
        OpptjeningAktivitetType.VIDERE_ETTERUTDANNING,
        OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD,
        OpptjeningAktivitetType.ARBEIDSAVKLARING));

    protected FrisinnOpptjeningForBeregningTjeneste() {
        // For proxy
    }

    @Inject
    public FrisinnOpptjeningForBeregningTjeneste(OpptjeningsperioderUtenOverstyringTjeneste opptjeningsperioderTjeneste) {
        this.opptjeningsperioderTjeneste = opptjeningsperioderTjeneste;
    }

    @Override
    public OpptjeningAktiviteter hentEksaktOpptjeningForBeregning(BehandlingReferanse ref,
                                                                  InntektArbeidYtelseGrunnlag iayGrunnlag) {
        LocalDate stp = ref.getUtledetSkjæringstidspunkt();
        LocalDate fom = LocalDate.of(2017, 3, 1);
        Optional<OpptjeningAktiviteter> opptjeningAktiviteter = hentOpptjeningForBeregning(ref, iayGrunnlag, stp, fom);

        if (opptjeningAktiviteter.isEmpty()) {
            String iayDump = dumpForDebug(ref, iayGrunnlag);
            throw new IllegalStateException(String.format("Forventer opptjening!!! ref=%s, stp=%s, fom=%s: iay=", ref, stp, fom, iayDump));
        }
        return opptjeningAktiviteter.get();
    }

    private String dumpForDebug(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var dto = new IAYTilDtoMapper(ref.getAktørId(), UUID.randomUUID(), UUID.randomUUID()).mapTilDto(iayGrunnlag);
        String iayDump;
        try {
            iayDump = IayGrunnlagJsonMapper.getMapper().writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            iayDump = e.getMessage();
        }
        return iayDump;
    }

    private List<OpptjeningsperiodeForSaksbehandling> hentRelevanteOpptjeningsaktiviteterForBeregningFrisinn(BehandlingReferanse behandlingReferanse,
                                                                                                             InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                                                             LocalDate stp, LocalDate fomDato) {

        var aktiviteter = opptjeningsperioderTjeneste.mapPerioderForSaksbehandling(behandlingReferanse, iayGrunnlag, vurderOpptjening, DatoIntervallEntitet.fraOgMed(fomDato)); // TODO (OJR):
        return aktiviteter.stream()
            .filter(oa -> oa.getPeriode().getFomDato().isBefore(stp))
            .filter(oa -> !oa.getPeriode().getTomDato().isBefore(fomDato))
            .filter(oa -> opptjeningsaktiviteter.erInkludert(oa.getOpptjeningAktivitetType()))
            .collect(Collectors.toList());
    }

    private Optional<OpptjeningAktiviteter> hentOpptjeningForBeregning(BehandlingReferanse ref,
                                                                       InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                       LocalDate stp,
                                                                       LocalDate fom) {
        var opptjeningsPerioder = hentRelevanteOpptjeningsaktiviteterForBeregningFrisinn(ref, iayGrunnlag, stp, fom).stream()
            .map(this::mapOpptjeningPeriode).collect(Collectors.toList());
        if (opptjeningsPerioder.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new OpptjeningAktiviteter(opptjeningsPerioder));
    }

    private OpptjeningPeriode mapOpptjeningPeriode(OpptjeningsperiodeForSaksbehandling ops) {
        var periode = new Periode(ops.getPeriode().getFomDato(), ops.getPeriode().getTomDato());
        var arbeidsgiver = ops.getArbeidsgiver();
        var orgnummer = arbeidsgiver == null ? null : arbeidsgiver.getOrgnr();
        var aktørId = arbeidsgiver == null ? null : (arbeidsgiver.getAktørId() == null ? null : arbeidsgiver.getAktørId().getId());
        var arbeidsforholdId = Optional.ofNullable(ops.getOpptjeningsnøkkel())
            .flatMap(Opptjeningsnøkkel::getArbeidsforholdRef)
            .orElse(null);
        return OpptjeningAktiviteter.nyPeriode(ops.getOpptjeningAktivitetType(), periode, orgnummer, aktørId, arbeidsforholdId);
    }

}
