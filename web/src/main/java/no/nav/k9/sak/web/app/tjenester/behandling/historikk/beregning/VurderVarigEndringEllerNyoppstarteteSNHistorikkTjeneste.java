package no.nav.k9.sak.web.app.tjenester.behandling.historikk.beregning;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.resultat.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.ToggleEndring;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.VarigEndretNæringVurdering;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltVerdiType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;


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
                                    List<OppdaterBeregningsgrunnlagResultat> alleEndringer, String begrunnelse) {
        for (var endringer : alleEndringer) {
            LocalDate skjæringstidspunkt = endringer.getSkjæringstidspunkt();
            endringer.getVarigEndretNæringVurdering()
                .ifPresent(varigEndretNæringVurdering -> lagHistorikkForVurderingAvVarigEndringOgSkjønnsfastsetting(
                    endringer,
                    skjæringstidspunkt,
                    varigEndretNæringVurdering)
                );
            historikkAdapter.tekstBuilder()
                .medBegrunnelse(begrunnelse, param.erBegrunnelseEndret())
                .medSkjermlenke(SkjermlenkeType.BEREGNING);
        }
    }

    private void lagHistorikkForVurderingAvVarigEndringOgSkjønnsfastsetting(OppdaterBeregningsgrunnlagResultat endringer, LocalDate skjæringstidspunkt, VarigEndretNæringVurdering varigEndretNæringVurdering) {
        ToggleEndring varigEndretNæringEndring = varigEndretNæringVurdering.getErVarigEndretNæringEndring();
        historikkAdapter.tekstBuilder().medNavnOgGjeldendeFra(HistorikkEndretFeltType.ENDRING_NÆRING, null, skjæringstidspunkt);
        if (varigEndretNæringEndring != null) {
            historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.ENDRING_NÆRING, null, konvertBooleanTilVarigEndringEndretVerdiType(varigEndretNæringEndring.getTilVerdi()));
        }
        ToggleEndring erNyoppstartetNæringEndring = varigEndretNæringVurdering.getErNyoppstartetNæringEndring();
        if (erNyoppstartetNæringEndring != null) {
            historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.SELVSTENDIG_NÆRINGSDRIVENDE, null, konvertBooleanTilNyoppstartetEndretVerdiType(erNyoppstartetNæringEndring.getTilVerdi()));
        }

        if (erTrue(varigEndretNæringEndring) || (erTrue(erNyoppstartetNæringEndring))) {
            lagHistorikkForInntekt(endringer, erTrue(varigEndretNæringEndring) ? varigEndretNæringEndring : erNyoppstartetNæringEndring);
        }
    }

    private boolean erTrue(ToggleEndring varigEndretNæringEndring) {
        return varigEndretNæringEndring != null && varigEndretNæringEndring.getTilVerdi();
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
                varigEndretNæringEndring.getFraVerdiEllerNull() != null && varigEndretNæringEndring.getFraVerdiEllerNull() ? inntektEndring.getFraBeløp().orElse(null) : null,
                inntektEndring.getTilBeløp()));
    }

    private HistorikkEndretFeltVerdiType konvertBooleanTilVarigEndringEndretVerdiType(Boolean endringNæring) {
        if (endringNæring == null) {
            return null;
        }
        return endringNæring ? HistorikkEndretFeltVerdiType.VARIG_ENDRET_NAERING : HistorikkEndretFeltVerdiType.INGEN_VARIG_ENDRING_NAERING;
    }

    private HistorikkEndretFeltVerdiType konvertBooleanTilNyoppstartetEndretVerdiType(Boolean endringNæring) {
        if (endringNæring == null) {
            return null;
        }
        return endringNæring ? HistorikkEndretFeltVerdiType.NYOPPSTARTET : HistorikkEndretFeltVerdiType.IKKE_NYOPPSTARTET;
    }


}
