package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.util.Collections;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.output.KalkulusResultat;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.request.v1.StartBeregningRequest;
import no.nav.folketrygdloven.kalkulus.response.v1.TilstandResponse;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;

/**
 * KalkulusTjeneste sørger for at K9 kaller kalkulus på riktig format i henhold til no.nav.folketrygdloven.kalkulus.kontrakt (https://github.com/navikt/ft-kalkulus/)
 */
@ApplicationScoped
@FagsakYtelseTypeRef("FRISINN")
public class FrisinnKalkulusTjeneste extends KalkulusTjeneste {


    public FrisinnKalkulusTjeneste() {
    }

    @Inject
    public FrisinnKalkulusTjeneste(KalkulusRestTjeneste restTjeneste,
                                   BehandlingRepository behandlingRepository,
                                   FagsakRepository fagsakRepository,
                                   KalkulatorInputTjeneste kalkulatorInputTjeneste,
                                   InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                   ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        super(restTjeneste, behandlingRepository, fagsakRepository, kalkulatorInputTjeneste, inntektArbeidYtelseTjeneste, arbeidsgiverTjeneste);
    }




    @Override
    public KalkulusResultat startBeregning(BehandlingReferanse referanse, YtelsespesifiktGrunnlagDto ytelseGrunnlag) {
        StartBeregningRequest startBeregningRequest = initStartRequest(referanse, ytelseGrunnlag);
        if (startBeregningRequest.getKalkulatorInput().getOpptjeningAktiviteter().getPerioder().isEmpty()) {
            return new KalkulusResultat(Collections.emptyList()).medVilkårResulatat(false);
        }
        TilstandResponse tilstandResponse = restTjeneste.startBeregning(startBeregningRequest);
        return mapFraTilstand(tilstandResponse);
    }

}
