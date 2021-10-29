package no.nav.k9.sak.web.app.tjenester.behandling.historikk;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.resultat.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.ToggleEndring;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.VarigEndretNæringVurdering;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltVerdiType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderVarigEndringEllerNyoppstartetSNDtoer;


@ApplicationScoped
public class VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste {

    private HistorikkTjenesteAdapter historikkAdapter;

    VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste() {
        // CDI
    }

    @Inject
    public VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste(HistorikkTjenesteAdapter historikkAdapter) {
        this.historikkAdapter = historikkAdapter;
    }

    public void lagHistorikkInnslag(AksjonspunktOppdaterParameter param,
                                    VurderVarigEndringEllerNyoppstartetSNDtoer dto,
                                    List<OppdaterBeregningsgrunnlagResultat> alleEndringer) {
        for (var endringer : alleEndringer) {
            LocalDate skjæringstidspunkt = endringer.getSkjæringstidspunkt();
            endringer.getVarigEndretNæringVurdering()
                .ifPresent(varigEndretNæringVurdering -> lagHistorikkForVurderingAvVarigEndringOgSkjønnsfastsetting(
                    endringer,
                    skjæringstidspunkt,
                    varigEndretNæringVurdering)
                );
            historikkAdapter.tekstBuilder()
                .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
                .medSkjermlenke(SkjermlenkeType.BEREGNING);
        }
    }

    private void lagHistorikkForVurderingAvVarigEndringOgSkjønnsfastsetting(OppdaterBeregningsgrunnlagResultat endringer, LocalDate skjæringstidspunkt, VarigEndretNæringVurdering varigEndretNæringVurdering) {
        ToggleEndring varigEndretNæringEndring = varigEndretNæringVurdering.getErVarigEndretNaeringEndring();
        historikkAdapter.tekstBuilder().medNavnOgGjeldendeFra(HistorikkEndretFeltType.ENDRING_NÆRING, null, skjæringstidspunkt);
        historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.ENDRING_NÆRING, null, konvertBooleanTilFaktaEndretVerdiType(varigEndretNæringEndring.getTilVerdi()));
        if (varigEndretNæringEndring.getTilVerdi()) {
            lagHistorikkForInntekt(endringer, varigEndretNæringEndring);
        }
    }

    private void lagHistorikkForInntekt(OppdaterBeregningsgrunnlagResultat endringer, ToggleEndring varigEndretNæringEndring) {
        var næringEndring = endringer.getBeregningsgrunnlagEndring().stream()
            .map(bgEndring -> bgEndring.getBeregningsgrunnlagPeriodeEndringer().get(0))
            .flatMap(p -> p.getBeregningsgrunnlagPrStatusOgAndelEndringer().stream())
            .filter(a -> a.getAktivitetStatus().erSelvstendigNæringsdrivende())
            .map(BeregningsgrunnlagPrStatusOgAndelEndring::getInntektEndring)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();

        næringEndring.ifPresent(inntektEndring ->
            historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.BRUTTO_NAERINGSINNTEKT,
                varigEndretNæringEndring.getFraVerdi() != null && varigEndretNæringEndring.getFraVerdi() ? inntektEndring.getFraInntekt().orElse(null) : null,
                inntektEndring.getTilInntekt()));
    }

    private HistorikkEndretFeltVerdiType konvertBooleanTilFaktaEndretVerdiType(Boolean endringNæring) {
        if (endringNæring == null) {
            return null;
        }
        return endringNæring ? HistorikkEndretFeltVerdiType.VARIG_ENDRET_NAERING : HistorikkEndretFeltVerdiType.INGEN_VARIG_ENDRING_NAERING;
    }


}
