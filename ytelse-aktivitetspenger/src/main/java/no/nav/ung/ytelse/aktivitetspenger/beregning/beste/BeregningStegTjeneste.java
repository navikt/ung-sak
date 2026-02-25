package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseAggregat;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.InntektFilter;
import no.nav.ung.sak.domene.iay.modell.Inntektspost;

import java.time.LocalDate;
import java.time.Year;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class BeregningStegTjeneste {

    private BeregningsgrunnlagRepository besteBeregningGrunnlagRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    BeregningStegTjeneste() {
    }

    @Inject
    public BeregningStegTjeneste(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                 InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.besteBeregningGrunnlagRepository = beregningsgrunnlagRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public void utførBesteberegning(Long behandlingId, LocalDate virkningsdato) {
        var inntektsposter = hentSigrunInntektsposter(behandlingId);
        var sistLignedeÅr = Year.of(virkningsdato.minusYears(1).getYear());  // TODO: Koble på utledning av siste tilgjengelige lignede år

        var resultat = BeregningTjeneste.avgjørBesteberegning(virkningsdato, sistLignedeÅr, inntektsposter);
        besteBeregningGrunnlagRepository.lagre(behandlingId, resultat);
    }


    private List<Inntektspost> hentSigrunInntektsposter(Long behandlingId) {
        var iayGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId);
        if (iayGrunnlag.isEmpty()) {
            return Collections.emptyList();
        }

        var inntekter = iayGrunnlag.get().getRegisterVersjon().map(InntektArbeidYtelseAggregat::getInntekter);
        Collection<Inntektspost> inntektsposter = new InntektFilter(inntekter).getInntektsposter(InntektsKilde.SIGRUN);
        return List.copyOf(inntektsposter);
    }
}

