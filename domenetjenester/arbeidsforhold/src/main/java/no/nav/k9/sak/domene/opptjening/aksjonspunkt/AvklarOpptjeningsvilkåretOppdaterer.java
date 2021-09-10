package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.felles.util.Tuple;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.domene.opptjening.Opptjeningsfeil;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.opptjening.AvklarOpptjeningsvilkårDto;
import no.nav.k9.sak.kontrakt.vilkår.VilkårPeriodeVurderingDto;
import no.nav.k9.sak.typer.Periode;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarOpptjeningsvilkårDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarOpptjeningsvilkåretOppdaterer implements AksjonspunktOppdaterer<AvklarOpptjeningsvilkårDto> {

    private OpptjeningRepository opptjeningRepository;
    private HistorikkTjenesteAdapter historikkAdapter;

    AvklarOpptjeningsvilkåretOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarOpptjeningsvilkåretOppdaterer(OpptjeningRepository opptjeningRepository,
                                               HistorikkTjenesteAdapter historikkAdapter) {

        this.opptjeningRepository = opptjeningRepository;
        this.historikkAdapter = historikkAdapter;
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
            lagHistorikkInnslag(param, nyttUtfall, dto.getBegrunnelse());

            if (nyttUtfall.equals(Utfall.OPPFYLT)) {
                sjekkOmVilkåretKanSettesTilOppfylt(param.getBehandlingId(), opptjeningPeriode, innvilgelseMerknad);
            }
            oppdaterUtfallOgLagre(nyttUtfall, vilkårPeriodeVurdering, vilkårBuilder);
        }
        builder.leggTil(vilkårBuilder);
        return OppdateringResultat.utenOveropp();
    }

    // TODO: Bedre løsning på sikt at endepunkt opptjening-v2 kobler sammen vilkårsperioder med opptjeningsperioder.
    //  Her kobler man heller aksjonspunktet man får i retur
    private List<Tuple<VilkårPeriodeVurderingDto, Periode>> sammenkoblePerioder(AvklarOpptjeningsvilkårDto dto) {
        var vilkårPeriodeVurderinger = dto.getVilkårPeriodeVurderinger();
        var opptjeningPerioder = dto.getOpptjeningPerioder();

        validerKonsistens(vilkårPeriodeVurderinger, opptjeningPerioder);

        var sammenkobledePerioder = IntStream.range(0, vilkårPeriodeVurderinger.size())
            .mapToObj(i -> new Tuple<>(vilkårPeriodeVurderinger.get(i), opptjeningPerioder.get(i)))
            .collect(Collectors.toList());
        return sammenkobledePerioder;
    }

    private void oppdaterUtfallOgLagre(Utfall utfallType, VilkårPeriodeVurderingDto vilkårPeriode, VilkårBuilder vilkårBuilder) {
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(vilkårPeriode.getPeriode().getFom(), vilkårPeriode.getPeriode().getTom())
            .medUtfall(utfallType)
            .medMerknad(utfallType.equals(Utfall.OPPFYLT) ? VilkårUtfallMerknad.fraKode(vilkårPeriode.getInnvilgelseMerknadKode()): null)
            .medAvslagsårsak(!utfallType.equals(Utfall.OPPFYLT) ? Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING : null));
    }

    private void sjekkOmVilkåretKanSettesTilOppfylt(Long behandlingId, Periode opptjeningPeriode, VilkårUtfallMerknad innvilgelseMerknad) {
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(opptjeningPeriode.getFom(), opptjeningPeriode.getTom());
        var stp = periode.getTomDato().plusDays(1);
        var opptjening = opptjeningRepository.finnOpptjening(behandlingId).flatMap(it -> it.finnOpptjening(periode))
            .orElseThrow(() -> Opptjeningsfeil.FACTORY.opptjeningPreconditionFailed().toException());
        var opptjeningAktiviteter = opptjening.getOpptjeningAktivitet();

        // Validering før opptjening kan gå videre til beregningsvilkår
        long antall = opptjeningAktiviteter.stream()
            .filter(oa -> !oa.getAktivitetType().equals(OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD)).count();
        if (antall == 0 && innvilgelseMerknad != VilkårUtfallMerknad.VM_7847_A) {
            throw new IllegalArgumentException("Må ha opptjeningsaktivitet, , eller velge 8-47 A, for å kunne gå videre til beregingsvilkår. Skjæringstidspunkt=" + stp);
        }
        // Validering av 8-47 (dersom valgt)
        if (innvilgelseMerknad != null) {
            long antallAktiviteterPåStp = opptjeningAktiviteter.stream()
                .filter(oa -> oa.getTom().plusDays(1).equals(stp))
                .filter(oa -> !oa.getAktivitetType().equals(OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD))
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

    private void lagHistorikkInnslag(AksjonspunktOppdaterParameter param, Utfall nyVerdi, String begrunnelse) {
        historikkAdapter.tekstBuilder()
            .medEndretFelt(HistorikkEndretFeltType.OPPTJENINGSVILKARET, null, nyVerdi);

        boolean erBegrunnelseForAksjonspunktEndret = param.erBegrunnelseEndret();
        historikkAdapter.tekstBuilder()
            .medBegrunnelse(begrunnelse, erBegrunnelseForAksjonspunktEndret)
            .medSkjermlenke(SkjermlenkeType.PUNKT_FOR_OPPTJENING);
    }

    private void validerKonsistens(List<VilkårPeriodeVurderingDto> vilkårPeriodeVurderinger, List<Periode> opptjeningPerioder) {
        if (vilkårPeriodeVurderinger.size() != opptjeningPerioder.size()) {
            throw new IllegalArgumentException("Antall vilkårsperioder, " + vilkårPeriodeVurderinger.size()
                + ", matcher ikke antall korresponderende opptjeningsperioder, " + opptjeningPerioder.size());
        }

        var sortertVp = vilkårPeriodeVurderinger.stream()
            .sorted(Comparator.comparing(VilkårPeriodeVurderingDto::getPeriode))
            .collect(Collectors.toList())
            .equals(vilkårPeriodeVurderinger);
        if (!sortertVp) {
            throw new IllegalArgumentException("Vilkårsperioder er ikke sortert etter periode");
        }

        var sortertOp = opptjeningPerioder.stream()
            .sorted()
            .collect(Collectors.toList())
            .equals(opptjeningPerioder);
        if (!sortertOp) {
            throw new IllegalArgumentException("Opptjeningsperioder er ikke sortert etter periode");
        }
    }
}
