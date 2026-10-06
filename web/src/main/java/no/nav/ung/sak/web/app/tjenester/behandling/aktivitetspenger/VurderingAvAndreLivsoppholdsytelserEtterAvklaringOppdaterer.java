package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.vilkår.IkkeOppfyltDetaljertÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AndreLivsoppholdsytelserResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AndreLivsoppholdsytelserVurderingResultat;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultat;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlag;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.livsopphold.VurderingAvAndreLivsoppholdsytelserEtterAvklaringDto;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.historikkinnslag.HistorikkinnslagInput;
import no.nav.ung.ytelse.aktivitetspenger.historikkinnslag.VilkårsvurderingHistorikkinnslagTjeneste;

import java.util.Set;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderingAvAndreLivsoppholdsytelserEtterAvklaringDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderingAvAndreLivsoppholdsytelserEtterAvklaringOppdaterer implements AksjonspunktOppdaterer<VurderingAvAndreLivsoppholdsytelserEtterAvklaringDto> {

    private static final VilkårType AKTUELT_VILKÅR = VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR;

    private VurderingAvVilkårEtterAvklaringTjeneste vurderingAvVilkårEtterAvklaringTjeneste;
    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;
    private VilkårsvurderingHistorikkinnslagTjeneste vilkårsvurderingHistorikkinnslagTjeneste;

    VurderingAvAndreLivsoppholdsytelserEtterAvklaringOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderingAvAndreLivsoppholdsytelserEtterAvklaringOppdaterer(VurderingAvVilkårEtterAvklaringTjeneste vurderingAvVilkårEtterAvklaringTjeneste,
                                                                       VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository,
                                                                       InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository,
                                                                       InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste,
                                                                       VilkårsvurderingHistorikkinnslagTjeneste vilkårsvurderingHistorikkinnslagTjeneste) {
        this.vurderingAvVilkårEtterAvklaringTjeneste = vurderingAvVilkårEtterAvklaringTjeneste;
        this.vilkårsavklaringGrunnlagRepository = vilkårsavklaringGrunnlagRepository;
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
        this.inngangsvilkårVurderingTjeneste = inngangsvilkårVurderingTjeneste;
        this.vilkårsvurderingHistorikkinnslagTjeneste = vilkårsvurderingHistorikkinnslagTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderingAvAndreLivsoppholdsytelserEtterAvklaringDto dto, AksjonspunktOppdaterParameter param) {
        long behandlingId = param.getBehandlingId();

        HistorikkinnslagInput historikkinnslagInput = vilkårsvurderingHistorikkinnslagTjeneste.hentInitielleVerdier(behandlingId, AKTUELT_VILKÅR);

        var foreslåtteAvklaringer = hentForeslåtteAvklaringer(behandlingId);
        var resultatTidslinje = vurderingAvVilkårEtterAvklaringTjeneste.utled(
            behandlingId, AKTUELT_VILKÅR, dto.getVurdertePerioder(), hentÅrsakTidslinje(foreslåtteAvklaringer)
        );

        var periodeVurderinger = resultatTidslinje
            .combine(hentFritekstTilVarsel(foreslåtteAvklaringer), VurderingAvAndreLivsoppholdsytelserEtterAvklaringOppdaterer::medYtelseNavn, LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .segmenter().stream()
            .map(s -> new AndreLivsoppholdsytelserResultatPeriode(
                DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()), s.getValue()))
            .toList();

        inngangsvilkårVurderingRepository.lagreYtelseVurderinger(behandlingId, periodeVurderinger);
        inngangsvilkårVurderingTjeneste.settAndreLivsoppholdsytelserResultat(behandlingId, param.getVilkårResultatBuilder());

        historikkinnslagInput.setSkjermlenkeType(SkjermlenkeType.VURDER_ANDRE_LIVSOPPHOLDSYTELSER)
            .setNyeVilkårVurderinger(inngangsvilkårVurderingRepository.hentVurderingTidslinje(behandlingId, AKTUELT_VILKÅR))
            .setHistorikkAktør(HistorikkAktør.LOKALKONTOR_SAKSBEHANDLER);
        vilkårsvurderingHistorikkinnslagTjeneste.lagreHistorikkinnslag(historikkinnslagInput);

        return OppdateringResultat.nyttResultat();
    }

    // Fordi avklaring for andre livsoppholdytelser ikke både har ytelsenavn og fritekst, bruker vi fritekstfeltet som ytelsenavn.
    private static LocalDateSegment<AndreLivsoppholdsytelserVurderingResultat> medYtelseNavn(LocalDateInterval intervall,
                                                                            LocalDateSegment<VilkårsvurderingResultat> vurdering,
                                                                            LocalDateSegment<String> fritekstTilVarsel) {
        var navn = fritekstTilVarsel == null ? null : fritekstTilVarsel.getValue();
        return new LocalDateSegment<>(intervall, AndreLivsoppholdsytelserVurderingResultat.fra(vurdering.getValue(), navn));
    }

    private Set<VilkårPeriodeAvklaring> hentForeslåtteAvklaringer(long behandlingId) {
        return vilkårsavklaringGrunnlagRepository
            .hentGrunnlagHvisEksisterer(behandlingId, AKTUELT_VILKÅR)
            .map(VilkårsavklaringGrunnlag::getForeslåtteAvklaringer)
            .orElse(Set.of());
    }

    private static LocalDateTimeline<String> hentFritekstTilVarsel(Set<VilkårPeriodeAvklaring> foreslåtteAvklaringer) {
        return new LocalDateTimeline<>(foreslåtteAvklaringer.stream()
            .filter(a -> a.getFritekstTilVarsel() != null)
            .map(a -> new LocalDateSegment<>(a.getPeriode().getFomDato(), a.getPeriode().getTomDato(), a.getFritekstTilVarsel()))
            .toList());
    }

    private static LocalDateTimeline<IkkeOppfyltDetaljertÅrsak> hentÅrsakTidslinje(Set<VilkårPeriodeAvklaring> foreslåtteAvklaringer) {
        return new LocalDateTimeline<>(foreslåtteAvklaringer.stream()
            .map(a -> new LocalDateSegment<>(
                a.getPeriode().getFomDato(),
                a.getPeriode().getTomDato(),
                IkkeOppfyltDetaljertÅrsak.fraKode(AKTUELT_VILKÅR, a.getIkkeOppfyltÅrsakKode())))
            .toList());
    }
}
