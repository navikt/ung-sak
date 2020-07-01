package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulatorInputTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusRestTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.output.KalkulusResultat;
import no.nav.folketrygdloven.kalkulus.beregning.v1.FrisinnGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.request.v1.StartBeregningRequest;
import no.nav.folketrygdloven.kalkulus.response.v1.TilstandResponse;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
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
                                   FagsakRepository fagsakRepository,
                                   @FagsakYtelseTypeRef("FRISINN") KalkulatorInputTjeneste kalkulatorInputTjeneste,
                                   InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                   ArbeidsgiverTjeneste arbeidsgiverTjeneste,
                                   VilkårResultatRepository vilkårResultatRepository) {
        super(restTjeneste, fagsakRepository, vilkårResultatRepository, kalkulatorInputTjeneste, inntektArbeidYtelseTjeneste, arbeidsgiverTjeneste);
    }

    @Override
    public KalkulusResultat startBeregning(BehandlingReferanse referanse, YtelsespesifiktGrunnlagDto ytelseGrunnlag, UUID bgReferanse, LocalDate skjæringstidspunkt) {
        FrisinnGrunnlag frisinnGrunnlag = (FrisinnGrunnlag) ytelseGrunnlag;
        if (frisinnGrunnlag.getPerioderMedSøkerInfo().isEmpty()) {
            return new KalkulusResultat(Collections.emptyList()).medAvslåttVilkår(Avslagsårsak.INGEN_STØNADSDAGER_I_SØKNADSPERIODEN);
        }

        StartBeregningRequest startBeregningRequest = initStartRequest(referanse, ytelseGrunnlag, bgReferanse, skjæringstidspunkt);
        if (startBeregningRequest.getKalkulatorInput().getOpptjeningAktiviteter().getPerioder().isEmpty()) {
            if (frisinnGrunnlag.getSøkerYtelseForFrilans()) {
                return new KalkulusResultat(Collections.emptyList()).medAvslåttVilkår(Avslagsårsak.SØKT_FRILANS_UTEN_FRILANS_INNTEKT);
            }
            return new KalkulusResultat(Collections.emptyList()).medAvslåttVilkår(Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG);
        }

        TilstandResponse tilstandResponse = getKalkulusRestTjeneste().startBeregning(startBeregningRequest);
        return mapFraTilstand(tilstandResponse);
    }

}
