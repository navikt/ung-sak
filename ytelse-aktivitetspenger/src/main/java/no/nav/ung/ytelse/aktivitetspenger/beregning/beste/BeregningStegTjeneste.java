package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseAggregat;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.InntektFilter;
import no.nav.ung.sak.domene.iay.modell.Inntektspost;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class BeregningStegTjeneste {

    private BesteBeregningGrunnlagRepository besteBeregningGrunnlagRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    BeregningStegTjeneste() {
    }

    @Inject
    public BeregningStegTjeneste(BesteBeregningGrunnlagRepository besteBeregningGrunnlagRepository,
                                 InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.besteBeregningGrunnlagRepository = besteBeregningGrunnlagRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public void utførBesteberegning(Long behandlingId, LocalDate virkningstidspunkt) {
        var inntektsposter = hentSigrunInntektsposter(behandlingId);
        var resultat = new BesteBeregning(virkningstidspunkt).avgjørBestePGI(inntektsposter);
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

