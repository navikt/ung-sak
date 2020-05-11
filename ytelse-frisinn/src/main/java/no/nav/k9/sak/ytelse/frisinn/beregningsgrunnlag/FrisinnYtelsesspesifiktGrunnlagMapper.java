package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulus.beregning.v1.FrisinnGrunnlag;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagYtelsespesifiktGrunnlagMapper;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;

@FagsakYtelseTypeRef("FRISINN")
@ApplicationScoped
public class FrisinnYtelsesspesifiktGrunnlagMapper implements BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<FrisinnGrunnlag> {

    private UttakRepository uttakRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    FrisinnYtelsesspesifiktGrunnlagMapper() {
    }

    @Inject
    public FrisinnYtelsesspesifiktGrunnlagMapper(UttakRepository uttakRepository, InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.uttakRepository = uttakRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    @Override
    public FrisinnGrunnlag lagYtelsespesifiktGrunnlag(BehandlingReferanse ref) {
        Optional<OppgittOpptjening> overstyrtOppgittOpptjeningOpt = inntektArbeidYtelseTjeneste.hentKunOverstyrtOppgittOpptjening(ref.getBehandlingId());
        boolean søkerYtelseForFrilans;
        boolean søkerYtelseForNæring;
        if (overstyrtOppgittOpptjeningOpt.isPresent()) {
            OppgittOpptjening oppgittOpptjening = overstyrtOppgittOpptjeningOpt.get();
            søkerYtelseForFrilans = oppgittOpptjening.getFrilans().isPresent();
            søkerYtelseForNæring = !oppgittOpptjening.getEgenNæring().isEmpty();

        } else {
            var fastsattUttak = uttakRepository.hentFastsattUttak(ref.getBehandlingId());
            var søknadsperiode = uttakRepository.hentOppgittSøknadsperioder(ref.getBehandlingId()).getMaksPeriode();

            søkerYtelseForFrilans = fastsattUttak.getPerioder().stream()
                    .anyMatch(p -> p.getPeriode().overlapper(søknadsperiode) && p.getAktivitetType() == UttakArbeidType.FRILANSER);

            søkerYtelseForNæring = fastsattUttak.getPerioder().stream()
                    .anyMatch(p -> p.getPeriode().overlapper(søknadsperiode) && p.getAktivitetType() == UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE);

        }
        return new FrisinnGrunnlag(søkerYtelseForFrilans, søkerYtelseForNæring);
    }
}
