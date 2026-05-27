package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.bosatt.FraflyttingsÅrsak;
import no.nav.ung.kodeverk.bosatt.Kilde;
import no.nav.ung.kodeverk.bosatt.OpphørKilde;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårJsonObjectMapper;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsAvklaringHolder;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.bosatt.OpphørResultat;
import no.nav.ung.sak.behandlingslager.bosatt.OpphørResultatRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.vilkår.ManuelleVilkårRekkefølgeTjeneste;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.sak.vilkår.VilkårVurderingSteg;

import java.time.LocalDate;
import java.util.*;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_BOSTEDVILKÅR;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_BOSTEDVILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class VurderBosattVilkårSteg extends VilkårVurderingSteg {

    private static final VilkårJsonObjectMapper VILKAR_JSON_OBJECT_MAPPER = new VilkårJsonObjectMapper();

    private ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private BostedsGrunnlagRepository bostedsGrunnlagRepository;
    private OpphørResultatRepository opphørResultatRepository;

    VurderBosattVilkårSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderBosattVilkårSteg(ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste,
                                  VilkårResultatRepository vilkårResultatRepository,
                                  VilkårTjeneste vilkårTjeneste,
                                  BehandlingRepository behandlingRepository,
                                  BostedsGrunnlagRepository bostedsGrunnlagRepository,
                                  OpphørResultatRepository opphørResultatRepository,
                                  @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste) {
        super(vilkårResultatRepository, vilkårTjeneste, behandlingRepository, vilkårsPerioderTilVurderingTjeneste);
        this.manuelleVilkårRekkefølgeTjeneste = manuelleVilkårRekkefølgeTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.bostedsGrunnlagRepository = bostedsGrunnlagRepository;
        this.opphørResultatRepository = opphørResultatRepository;
    }

    @Override
    public VilkårType getAktuellVilkårType() {
        return VilkårType.BOSTEDSVILKÅR;
    }

    @Override
    public Set<VilkårType> getVilkårAvhengigheter(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        EnumSet<VilkårType> avhengigheter = EnumSet.noneOf(VilkårType.class);
        avhengigheter.add(VilkårType.ALDERSVILKÅR);
        avhengigheter.add(VilkårType.SØKNADSFRIST);
        avhengigheter.addAll(manuelleVilkårRekkefølgeTjeneste.finnManuelleVilkårSomErFør(getAktuellVilkårType(), ytelseType, behandlingType));
        return avhengigheter;
    }

    @Override
    public BehandleStegResultat utførResten(BehandlingskontrollKontekst kontekst) {
        long behandlingId = kontekst.getBehandlingId();
        LocalDateTimeline<Boolean> tidslinjeTilVurdering = finnPerioderSomSkalVurderes(kontekst);

        if (tidslinjeTilVurdering.isEmpty()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        List<OpphørResultat> aktiveOpphørResultater = opphørResultatRepository.hentAktiveForBehandling(behandlingId);
        if (aktiveOpphørResultater.isEmpty()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        Map<LocalDate, OpphørResultat> opphørPerStp = new HashMap<>();
        for (OpphørResultat or : aktiveOpphørResultater) {
            opphørPerStp.put(or.getSkjæringstidspunkt(), or);
        }

        var grunnlag = bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Forventer grunnlag med bostedsavklaringer"));
        BostedsAvklaringHolder holder = grunnlag.getHolder();

        Vilkårene vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Forventer vilkårresultat for behandling " + behandlingId));

        var builder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.BOSTEDSVILKÅR);

        for (var segment : tidslinjeTilVurdering.toSegments()) {
            LocalDate stp = segment.getFom();
            OpphørResultat opphør = opphørPerStp.get(stp);

            if (opphør == null) {
                var periodeBuilder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fra(segment.getLocalDateInterval()));
                holder.getPeriodeAvklaring(stp).ifPresent(avklaring ->
                    periodeBuilder.medRegelInput(lagRegelInput(avklaring))
                );
                periodeBuilder.medUtfall(Utfall.OPPFYLT);
                vilkårBuilder.leggTil(periodeBuilder);
            } else {
                BostedsPeriodeAvklaring avklaring = holder.getPeriodeAvklaring(stp)
                    .orElseThrow(() -> new IllegalStateException("Forventer bostedsavklaring for stp " + stp));
                String regelInput = lagRegelInput(avklaring);
                Avslagsårsak avslagsårsak = opphør.getOpphørÅrsak();
                LocalDate opphørDato = opphør.getOpphørDato();

                if (opphørDato.isBefore(stp)) {
                    // Ikke bosatt på stp
                    var periodeBuilder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fra(segment.getLocalDateInterval()))
                        .medRegelInput(regelInput)
                        .medUtfall(Utfall.IKKE_OPPFYLT)
                        .medAvslagsårsak(avslagsårsak);

                    if (opphør.getKilde() == OpphørKilde.AUTOMATISK) {
                        periodeBuilder
                            .nullstillFritekstvurderinger()
                            .medManueltVurdert(false);
                    }

                    vilkårBuilder.leggTil(periodeBuilder);
                } else if (opphørDato.isAfter(segment.getTom())) {
                    // OpphørDato etter perioden — OPPFYLT hele perioden
                    var periodeBuilder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fra(segment.getLocalDateInterval()))
                        .medRegelInput(regelInput)
                        .medUtfall(Utfall.OPPFYLT);

                    vilkårBuilder.leggTil(periodeBuilder);
                } else {
                    // Splitt: OPPFYLT frem til opphørDato, IKKE_OPPFYLT fra opphørDato
                    var oppfyltBuilder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fraOgMedTilOgMed(stp, opphørDato.minusDays(1)));
                    oppfyltBuilder
                        .medRegelInput(regelInput)
                        .medUtfall(Utfall.OPPFYLT);

                    if (opphør.getKilde() == OpphørKilde.AUTOMATISK) {
                        oppfyltBuilder
                            .nullstillFritekstvurderinger()
                            .medManueltVurdert(false);
                    }
                    vilkårBuilder.leggTil(oppfyltBuilder);

                    var ikkeOppfyltBuilder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fraOgMedTilOgMed(opphørDato, segment.getTom()));
                    ikkeOppfyltBuilder.medRegelInput(regelInput)
                        .medUtfall(Utfall.IKKE_OPPFYLT)
                        .medAvslagsårsak(avslagsårsak);

                    if (opphør.getKilde() == OpphørKilde.AUTOMATISK) {
                        ikkeOppfyltBuilder
                            .nullstillFritekstvurderinger()
                            .medManueltVurdert(false);
                    }
                    vilkårBuilder.leggTil(ikkeOppfyltBuilder);
                }
            }
        }

        builder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandlingId, builder.build());

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private static String lagRegelInput(BostedsPeriodeAvklaring periodeAvklaring) {
        return VILKAR_JSON_OBJECT_MAPPER.writeValueAsString(new RegelInput(
            periodeAvklaring.getReferanse(),
            periodeAvklaring.getSkjæringstidspunkt(),
            periodeAvklaring.isErBosattITrondheim(),
            periodeAvklaring.getFraflyttingsDato(),
            periodeAvklaring.getFraflyttingsÅrsak(),
            periodeAvklaring.getKilde()));
    }

    private record RegelInput(UUID referanse,
                              LocalDate skjaeringstidspunkt,
                              boolean erBosattITrondheim,
                              LocalDate fraflyttingsDato,
                              FraflyttingsÅrsak fraflyttingsAarsak,
                              Kilde kilde) {
    }
}
