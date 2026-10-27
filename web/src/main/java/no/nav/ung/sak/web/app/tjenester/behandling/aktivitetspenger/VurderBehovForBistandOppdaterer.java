package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BistandsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand.VurderBehovForBistandDto;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.AktivitetspengerVilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.InngangsvilkårVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.avkort.AvkortTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.historikkinnslag.HistorikkinnslagInput;
import no.nav.ung.ytelse.aktivitetspenger.historikkinnslag.VilkårsvurderingHistorikkinnslagTjeneste;

import java.time.LocalDateTime;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderBehovForBistandDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderBehovForBistandOppdaterer implements AksjonspunktOppdaterer<VurderBehovForBistandDto> {

    private static final VilkårType AKTUELT_VILKÅR = VilkårType.BISTANDSVILKÅR;

    private VilkårResultatRepository vilkårResultatRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;
    private AvkortTjeneste avkortTjeneste;
    private VilkårsvurderingHistorikkinnslagTjeneste vilkårsvurderingHistorikkinnslagTjeneste;
    private AktivitetspengerVilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste;

    VurderBehovForBistandOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderBehovForBistandOppdaterer(VilkårResultatRepository vilkårResultatRepository,
                                           InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository,
                                           InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste,
                                           AvkortTjeneste avkortTjeneste,
                                           VilkårsvurderingHistorikkinnslagTjeneste vilkårsvurderingHistorikkinnslagTjeneste,
                                           @FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER) AktivitetspengerVilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
        this.inngangsvilkårVurderingTjeneste = inngangsvilkårVurderingTjeneste;
        this.avkortTjeneste = avkortTjeneste;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.vilkårsvurderingHistorikkinnslagTjeneste = vilkårsvurderingHistorikkinnslagTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderBehovForBistandDto dto, AksjonspunktOppdaterParameter param) {
        HistorikkinnslagInput historikkinnslagInput = vilkårsvurderingHistorikkinnslagTjeneste.hentInitielleVerdier(param.getBehandlingId(), AKTUELT_VILKÅR);

        Vilkårene vilkårene = vilkårResultatRepository.hentHvisEksisterer(param.getBehandlingId()).orElseThrow();
        LocalDateTimeline<VilkårPeriode>  eksisterendeVilkårsperioder = vilkårene.getVilkårTimeline(AKTUELT_VILKÅR).filterValue(v -> v.getUtfall() != Utfall.IKKE_RELEVANT);
        var vilkårsPerioderTilVurderingForNySøknad = vilkårsPerioderTilVurderingTjeneste.utledPerÅrsak(param.getBehandlingId(), AKTUELT_VILKÅR)
            .getOrDefault(BehandlingÅrsakType.NY_SØKT_PERIODE, LocalDateTimeline.empty());
        LocalDateTimeline<Boolean> perioderTilVurdering = TidslinjeUtil.toBooleanTimeline(eksisterendeVilkårsperioder).intersection(vilkårsPerioderTilVurderingForNySøknad);

        var vurdertePerioderInput = new LocalDateTimeline<>(dto.getVurdertePerioder().stream()
            .map(it -> new LocalDateSegment<>(it.periode().getFom(), it.periode().getTom(), it)).toList()
        );

        var uforventedePerioder = vurdertePerioderInput.disjoint(perioderTilVurdering);
        if (!uforventedePerioder.isEmpty()) {
            throw new IllegalArgumentException("Forsøker å vurdere perioder som ikke er til vurdering. Gjelder perioder: " + uforventedePerioder);
        }
        LocalDateTimeline<Boolean> manglendePerioder = perioderTilVurdering.disjoint(vurdertePerioderInput);
        if (!manglendePerioder.isEmpty()) {
            throw new IllegalArgumentException("Forventer at alle perioder til vurdering vurderes. Mangler : " + manglendePerioder);
        }
        validerAvkortingBruktRiktig(dto, param.getBehandlingId());

        String vurdertAv = SubjectHandler.getSubjectHandler().getUid();
        LocalDateTime vurdertTidspunkt = LocalDateTime.now();
        var periodeVurderinger = dto.getVurdertePerioder().stream()
            .map(it -> new BistandsvilkårResultatPeriode(
                DatoIntervallEntitet.fraOgMedTilOgMed(it.periode().getFom(), it.periode().getTom()),
                it.erVilkårOppfylt(),
                it.avslagsårsak(),
                true,
                it.begrunnelse(),
                it.fritekstVurderingBrev(),
                vurdertAv,
                vurdertTidspunkt,
                null
            ))
            .toList();
        inngangsvilkårVurderingRepository.lagreBistandsVurderinger(param.getBehandlingId(), periodeVurderinger);

        inngangsvilkårVurderingTjeneste.settBistandsvilkårResultat(param.getBehandlingId(), param.getVilkårResultatBuilder());

        historikkinnslagInput
            .setSkjermlenkeType(SkjermlenkeType.BISTANDSVILKÅR)
            .setNyeVilkårVurderinger(inngangsvilkårVurderingRepository.hentVurderingTidslinje(param.getBehandlingId(), AKTUELT_VILKÅR))
            .setHistorikkAktør(HistorikkAktør.NAV_KONTOR_SAKSBEHANDLER);
        vilkårsvurderingHistorikkinnslagTjeneste.lagreHistorikkinnslag(historikkinnslagInput);

        return OppdateringResultat.nyttResultat();
    }

    private void validerAvkortingBruktRiktig(VurderBehovForBistandDto dto, Long behandlingId) {
        LocalDateTimeline<Boolean> perioderSattTilAvkortet = new LocalDateTimeline<>(dto.getVurdertePerioder().stream()
            .filter(f -> f.avslagsårsak() == BistandsvilkårIkkeOppfyltÅrsak.AVKORTET)
            .map(it -> new LocalDateSegment<>(it.periode().getFom(), it.periode().getTom(), true))
            .toList());
        avkortTjeneste.validerAvkortBruktRiktig(behandlingId, perioderSattTilAvkortet, AKTUELT_VILKÅR);
    }

}
