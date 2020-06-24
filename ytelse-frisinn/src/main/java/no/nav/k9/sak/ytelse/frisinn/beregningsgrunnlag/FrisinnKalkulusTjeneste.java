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
import no.nav.folketrygdloven.kalkulus.UuidDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.FrisinnGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.KalkulatorInputDto;
import no.nav.folketrygdloven.kalkulus.kodeverk.YtelseTyperKalkulusStøtterKontrakt;
import no.nav.folketrygdloven.kalkulus.request.v1.StartBeregningRequest;
import no.nav.folketrygdloven.kalkulus.response.v1.TilstandResponse;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

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
                                   KalkulatorInputTjeneste kalkulatorInputTjeneste,
                                   InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                   ArbeidsgiverTjeneste arbeidsgiverTjeneste,
                                   VilkårResultatRepository vilkårResultatRepository) {
        super(restTjeneste, fagsakRepository, vilkårResultatRepository, kalkulatorInputTjeneste, inntektArbeidYtelseTjeneste, arbeidsgiverTjeneste);
    }

    @Override
    public KalkulusResultat startBeregning(BehandlingReferanse referanse, YtelsespesifiktGrunnlagDto ytelseGrunnlag, UUID bgReferanse, LocalDate periodeStart) {
        FrisinnGrunnlag frisinnGrunnlag = (FrisinnGrunnlag) ytelseGrunnlag;
        if (frisinnGrunnlag.getPerioderMedSøkerInfo().isEmpty()) {
            return new KalkulusResultat(Collections.emptyList()).medAvslåttVilkår(Avslagsårsak.INGEN_STØNADSDAGER_I_SØKNADSPERIODEN);
        }

        StartBeregningRequest startBeregningRequest = initStartRequest(referanse, ytelseGrunnlag, bgReferanse, periodeStart);
        if (startBeregningRequest.getKalkulatorInput().getOpptjeningAktiviteter().getPerioder().isEmpty()) {
            if (frisinnGrunnlag.getSøkerYtelseForFrilans()) {
                return new KalkulusResultat(Collections.emptyList()).medAvslåttVilkår(Avslagsårsak.SØKT_FRILANS_UTEN_FRILANS_INNTEKT);
            }
            return new KalkulusResultat(Collections.emptyList()).medAvslåttVilkår(Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG);
        }

        TilstandResponse tilstandResponse = restTjeneste.startBeregning(startBeregningRequest);
        return mapFraTilstand(tilstandResponse);
    }


    @Override
    protected StartBeregningRequest initStartRequest(BehandlingReferanse referanse, YtelsespesifiktGrunnlagDto ytelseGrunnlag, UUID bgReferanse, LocalDate skjæringstidspunkt) {
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(referanse.getFagsakId());

        AktørIdPersonident aktør = new AktørIdPersonident(fagsak.getAktørId().getId());
        var vilkårsPeriode = vilkårResultatRepository.hent(referanse.getBehandlingId()).getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow()
            .finnPeriodeForSkjæringstidspunkt(skjæringstidspunkt)
            .getPeriode();
        KalkulatorInputDto kalkulatorInputDto = kalkulatorInputTjeneste.byggDto(referanse, ytelseGrunnlag,
            DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2020, 3, 1), vilkårsPeriode.getFomDato()));

        return new StartBeregningRequest(
            new UuidDto(bgReferanse),
            fagsak.getSaksnummer().getVerdi(),
            aktør,
            new YtelseTyperKalkulusStøtterKontrakt(referanse.getFagsakYtelseType().getKode()),
            kalkulatorInputDto);
    }

}
