package no.nav.k9.sak.ytelse.pleiepengerbarn.beregninginput;

import java.util.Arrays;
import java.util.NavigableSet;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.felles.exception.ManglerTilgangException;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrInputForBeregningDto;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sikkerhet.context.SubjectHandler;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrInputForBeregningDto.class, adapter = AksjonspunktOppdaterer.class)
public class BeregningInputOppdaterer implements AksjonspunktOppdaterer<OverstyrInputForBeregningDto> {

    private BeregningInputLagreTjeneste beregningInputLagreTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BeregningInputHistorikkTjeneste beregningInputHistorikkTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste;

    private Environment environment;

    BeregningInputOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BeregningInputOppdaterer(BeregningInputLagreTjeneste beregningInputLagreTjeneste,
                                    InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                    BeregningInputHistorikkTjeneste beregningInputHistorikkTjeneste,
                                    @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste) {
        this.beregningInputLagreTjeneste = beregningInputLagreTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.beregningInputHistorikkTjeneste = beregningInputHistorikkTjeneste;
        this.environment = Environment.current();
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(OverstyrInputForBeregningDto dto, AksjonspunktOppdaterParameter param) {
        if (!harTillatelseTilÅLøseAksjonspunkt()) {
            throw new ManglerTilgangException("K9-IF-01", "Har ikke tilgang til å løse aksjonspunkt.");
        }
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(param.getBehandlingId());
        var inntektsmeldingerForSak = inntektArbeidYtelseTjeneste.hentUnikeInntektsmeldingerForSak(param.getRef().getSaksnummer());
        var perioderTilVurdering = getPerioderTilVurdering(param);
        beregningInputLagreTjeneste.lagreInputOverstyringer(param.getRef(), dto, iayGrunnlag, inntektsmeldingerForSak, perioderTilVurdering);
        beregningInputHistorikkTjeneste.lagHistorikk(param.getBehandlingId(), dto.getBegrunnelse());
        return OppdateringResultat.nyttResultat();
    }

    private NavigableSet<DatoIntervallEntitet> getPerioderTilVurdering(AksjonspunktOppdaterParameter param) {
        return getPerioderTilVurderingTjeneste(param.getRef().getFagsakYtelseType(), param.getRef().getBehandlingType())
            .utled(param.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
    }

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(FagsakYtelseType fagsakYtelseType, BehandlingType type) {
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, perioderTilVurderingTjeneste, fagsakYtelseType, type)
            .orElseThrow(() -> new UnsupportedOperationException("VilkårsPerioderTilVurderingTjeneste ikke implementert for ytelse [" + fagsakYtelseType + "]"));
    }


    private boolean harTillatelseTilÅLøseAksjonspunkt() {
        return Arrays.stream(environment.getProperty("INFOTRYGD_MIGRERING_TILLATELSER", "").split(",")).anyMatch(id -> id.equalsIgnoreCase(SubjectHandler.getSubjectHandler().getUid()));
    }

}
