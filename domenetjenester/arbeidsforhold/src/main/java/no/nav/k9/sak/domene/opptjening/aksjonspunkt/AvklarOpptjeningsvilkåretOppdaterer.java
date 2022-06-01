package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.util.Tuple;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltVerdiType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetVurderingOpptjeningsvilkår;
import no.nav.k9.sak.domene.opptjening.VurderingsStatus;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.opptjening.AvklarOpptjeningsvilkårDto;
import no.nav.k9.sak.kontrakt.vilkår.VilkårPeriodeVurderingDto;
import no.nav.k9.sak.typer.Periode;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarOpptjeningsvilkårDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarOpptjeningsvilkåretOppdaterer implements AksjonspunktOppdaterer<AvklarOpptjeningsvilkårDto> {

    private static final Logger log = LoggerFactory.getLogger(AvklarOpptjeningsvilkåretOppdaterer.class);

    private HistorikkTjenesteAdapter historikkAdapter;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private OpptjeningsperioderTjeneste opptjeningsperioderTjeneste;
    private OpptjeningAktivitetVurderingOpptjeningsvilkår vurderForOpptjeningsvilkår;

    AvklarOpptjeningsvilkåretOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarOpptjeningsvilkåretOppdaterer(HistorikkTjenesteAdapter historikkAdapter,
                                               InntektArbeidYtelseTjeneste iayTjeneste,
                                               OpptjeningsperioderTjeneste opptjeningsperioderTjeneste) {

        this.historikkAdapter = historikkAdapter;
        this.iayTjeneste = iayTjeneste;
        this.opptjeningsperioderTjeneste = opptjeningsperioderTjeneste;
        this.vurderForOpptjeningsvilkår = new OpptjeningAktivitetVurderingOpptjeningsvilkår();
    }

    @Override
    public OppdateringResultat oppdater(AvklarOpptjeningsvilkårDto dto, AksjonspunktOppdaterParameter param) {
        var builder = param.getVilkårResultatBuilder();
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET);

        List<Tuple<VilkårPeriodeVurderingDto, Periode>> perioder = sammenkoblePerioder(dto);

        for (Tuple<VilkårPeriodeVurderingDto, Periode> periode : perioder) {
            var vilkårPeriodeVurdering = periode.getElement1();
            var opptjeningPeriode = periode.getElement2();
            var innvilgelseMerknad = VilkårUtfallMerknad.fraKode(vilkårPeriodeVurdering.getInnvilgelseMerknadKode());

            Utfall nyttUtfall = vilkårPeriodeVurdering.isErVilkarOk() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
            HistorikkEndretFeltVerdiType tilVerdiHistorikk = finnTilVerdiHistorikk(vilkårPeriodeVurdering, innvilgelseMerknad);
            lagHistorikkInnslag(param, tilVerdiHistorikk, vilkårPeriodeVurdering.getBegrunnelse());

            if (nyttUtfall.equals(Utfall.OPPFYLT)) {
                sjekkOmVilkåretKanSettesTilOppfylt(param.getRef(), opptjeningPeriode, innvilgelseMerknad, vilkårPeriodeVurdering.getPeriode());
            }
            oppdaterUtfallOgLagre(nyttUtfall, vilkårPeriodeVurdering, vilkårBuilder);
        }
        builder.leggTil(vilkårBuilder);
        return OppdateringResultat.nyttResultat();
    }

    private HistorikkEndretFeltVerdiType finnTilVerdiHistorikk(VilkårPeriodeVurderingDto vilkårPeriodeVurdering, VilkårUtfallMerknad innvilgelseMerknad) {
        HistorikkEndretFeltVerdiType tilVerdiHistorikk;
        if (!vilkårPeriodeVurdering.isErVilkarOk()) {
            tilVerdiHistorikk = HistorikkEndretFeltVerdiType.IKKE_OPPFYLT;
        } else {
            if (innvilgelseMerknad != null && innvilgelseMerknad.equals(VilkårUtfallMerknad.VM_7847_A)) {
                tilVerdiHistorikk = HistorikkEndretFeltVerdiType.OPPFYLT_8_47_A;
            } else if (innvilgelseMerknad != null && innvilgelseMerknad.equals(VilkårUtfallMerknad.VM_7847_B)) {
                tilVerdiHistorikk = HistorikkEndretFeltVerdiType.OPPFYLT_8_47_B;
            } else {
                tilVerdiHistorikk = HistorikkEndretFeltVerdiType.OPPFYLT;
            }
        }
        return tilVerdiHistorikk;
    }

    private List<Tuple<VilkårPeriodeVurderingDto, Periode>> sammenkoblePerioder(AvklarOpptjeningsvilkårDto dto) {
        var vilkårPeriodeVurderinger = dto.getVilkårPeriodeVurderinger();
        var opptjeningPerioder = dto.getOpptjeningPerioder();

        var sammenkobledePerioder = vilkårPeriodeVurderinger.stream()
            .map(vilkårPeriodeVurderingDto -> new Tuple<>(vilkårPeriodeVurderingDto, finnTilhørendeOpptjeningsperiode(opptjeningPerioder, vilkårPeriodeVurderingDto.getPeriode())))
            .collect(Collectors.toList());

        validerKonsistens(sammenkobledePerioder);

        return sammenkobledePerioder;
    }

    private void validerKonsistens(List<Tuple<VilkårPeriodeVurderingDto, Periode>> sammenkobledePerioder) {
        if (sammenkobledePerioder.stream().anyMatch(it -> Objects.isNull(it.getElement2()))) {
            var perioderMedMangler = sammenkobledePerioder.stream()
                .filter(it -> Objects.isNull(it.getElement2()))
                .map(Tuple::getElement1)
                .map(VilkårPeriodeVurderingDto::getPeriode);

            log.info("Inkonsistens i data fra frontend, perioder som mangler informasjon om opptjeningsperioden :: {}", perioderMedMangler);
            throw new IllegalArgumentException("Inkonsistens i data fra frontend, det mangler data om opptjeningsperioder");
        }
    }

    private Periode finnTilhørendeOpptjeningsperiode(List<Periode> opptjeningPerioder, Periode periode) {
        var sisteDagIOpptjeningsperioden = periode.getFom().minusDays(1);
        return opptjeningPerioder.stream().filter(it -> Objects.equals(it.getTom(), sisteDagIOpptjeningsperioden)).findAny().orElse(null);
    }

    private void oppdaterUtfallOgLagre(Utfall utfallType, VilkårPeriodeVurderingDto vilkårPeriode, VilkårBuilder vilkårBuilder) {
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(vilkårPeriode.getPeriode().getFom(), vilkårPeriode.getPeriode().getTom())
            .medUtfallManuell(utfallType)
            .medBegrunnelse(vilkårPeriode.getBegrunnelse())
            .medMerknad(utfallType.equals(Utfall.OPPFYLT) ? VilkårUtfallMerknad.fraKode(vilkårPeriode.getInnvilgelseMerknadKode()) : null)
            .medAvslagsårsak(!utfallType.equals(Utfall.OPPFYLT) ? Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING : null));
    }

    private void sjekkOmVilkåretKanSettesTilOppfylt(BehandlingReferanse ref, Periode opptjeningPeriode, VilkårUtfallMerknad innvilgelseMerknad, Periode periode) {
        var opptjPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(opptjeningPeriode.getFom(), opptjeningPeriode.getTom());
        var stp = opptjPeriode.getTomDato().plusDays(1);
        var iayGrunnlag = iayTjeneste.finnGrunnlag(ref.getBehandlingId()).orElseThrow();
        var yrkesaktivitetFilter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(ref.getAktørId())).før(periode.getFom());
        var opptjeningAktiviteter = opptjeningsperioderTjeneste.mapPerioderForSaksbehandling(ref, iayGrunnlag, vurderForOpptjeningsvilkår, opptjPeriode, DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom()), yrkesaktivitetFilter);

        // Validering før opptjening kan gå videre til beregningsvilkår
        long antall = opptjeningAktiviteter.stream()
            .filter(oa -> !oa.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD))
            .filter(oa -> !oa.getVurderingsStatus().equals(VurderingsStatus.UNDERKJENT))
            .filter(oa -> oa.getPeriode().inkluderer(stp) || oa.getPeriode().getTomDato().plusDays(1).equals(stp)).count();
        if (antall == 0 && innvilgelseMerknad != VilkårUtfallMerknad.VM_7847_A) {
            throw new IllegalArgumentException("Må ha opptjeningsaktivitet på skjæringstidspunktet for å kunne gå videre til beregingsvilkår. (Dersom 8-47 A er tilgjengelig, kan denne velges.) Skjæringstidspunkt=" + stp);
        }
        // Validering av 8-47 (dersom valgt)
        if (innvilgelseMerknad != null) {
            long antallAktiviteterPåStp = opptjeningAktiviteter.stream()
                .filter(oa -> !oa.getVurderingsStatus().equals(VurderingsStatus.UNDERKJENT))
                .filter(oa -> oa.getPeriode().inkluderer(stp))
                .filter(oa -> !oa.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD))
                .count();
            if (antallAktiviteterPåStp == 0) {
                if (innvilgelseMerknad != VilkårUtfallMerknad.VM_7847_A) {
                    throw new IllegalArgumentException("Må velge 8-47 A når det IKKE finnes aktivitet på skjæringstidspunkt=" + stp);
                }
            } else {
                if (innvilgelseMerknad != VilkårUtfallMerknad.VM_7847_B) {
                    throw new IllegalArgumentException("Må velge 8-47 B når det finnes aktivitet på skjæringstidspunkt=" + stp);
                }
            }
        }
    }

    private void lagHistorikkInnslag(AksjonspunktOppdaterParameter param, HistorikkEndretFeltVerdiType nyVerdi, String begrunnelse) {
        historikkAdapter.tekstBuilder()
            .medEndretFelt(HistorikkEndretFeltType.OPPTJENINGSVILKARET, null, nyVerdi);

        boolean erBegrunnelseForAksjonspunktEndret = param.erBegrunnelseEndret();
        historikkAdapter.tekstBuilder()
            .medBegrunnelse(begrunnelse, erBegrunnelseForAksjonspunktEndret)
            .medSkjermlenke(SkjermlenkeType.PUNKT_FOR_OPPTJENING);
    }
}
