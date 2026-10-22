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
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
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
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BostedsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bosted.ManuellVurderingBostedsvilkårDto;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.AktivitetspengerVilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.InngangsvilkårVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.avkort.AvkortTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.historikkinnslag.HistorikkinnslagInput;
import no.nav.ung.ytelse.aktivitetspenger.historikkinnslag.VilkårsvurderingHistorikkinnslagTjeneste;

import java.time.LocalDateTime;

/**
 * Oppdaterer for aksjonspunkt 5140 – manuell vurdering av bostedsvilkåret for nye perioder.
 * Vurdering etter vilkårsavklaring håndteres av 5145.
 */
@ApplicationScoped
@DtoTilServiceAdapter(dto = ManuellVurderingBostedsvilkårDto.class, adapter = AksjonspunktOppdaterer.class)
public class ManuellVurderingBostedsvilkårOppdaterer implements AksjonspunktOppdaterer<ManuellVurderingBostedsvilkårDto> {

    private static final VilkårType AKTUELT_VILKÅR = VilkårType.BOSTEDSVILKÅR;

    private VilkårResultatRepository vilkårResultatRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;
    private AvkortTjeneste avkortTjeneste;
    private VilkårsvurderingHistorikkinnslagTjeneste vilkårsvurderingHistorikkinnslagTjeneste;
    private AktivitetspengerVilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste;

    ManuellVurderingBostedsvilkårOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public ManuellVurderingBostedsvilkårOppdaterer(VilkårResultatRepository vilkårResultatRepository,
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
    public OppdateringResultat oppdater(ManuellVurderingBostedsvilkårDto dto, AksjonspunktOppdaterParameter param) {
        HistorikkinnslagInput historikkinnslagInput = vilkårsvurderingHistorikkinnslagTjeneste.hentInitielleVerdier(param.getBehandlingId(), AKTUELT_VILKÅR);

        Vilkårene vilkårene = vilkårResultatRepository.hentHvisEksisterer(param.getBehandlingId()).orElseThrow();
        LocalDateTimeline<VilkårPeriode> eksisterendeVilkårperioder = vilkårene.getVilkårTimeline(AKTUELT_VILKÅR)
            .filterValue(v -> v.getUtfall() != Utfall.IKKE_RELEVANT && v.getAvslagsårsak() != Avslagsårsak.AVKORTET);
        var vilkårsPerioderTilVurderingForNySøknad = vilkårsPerioderTilVurderingTjeneste.utledPerÅrsak(param.getBehandlingId(), AKTUELT_VILKÅR)
            .getOrDefault(BehandlingÅrsakType.NY_SØKT_PERIODE, LocalDateTimeline.empty());
        LocalDateTimeline<Boolean> perioderTilVurdering = TidslinjeUtil.toBooleanTimeline(eksisterendeVilkårperioder).intersection(vilkårsPerioderTilVurderingForNySøknad);

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
        var periodeVurderinger = vurdertePerioderInput.segmenter().stream()
            .map(it ->
                new BostedsvilkårResultatPeriode(
                    DatoIntervallEntitet.fraOgMedTilOgMed(
                        it.getFom(),
                        it.getTom()
                    ),
                    it.getValue().erVilkårOppfylt(),
                    it.getValue().avslagsårsak(),
                    true,
                    it.getValue().begrunnelse(),
                    it.getValue().fritekstVurderingBrev(),
                    vurdertAv,
                    vurdertTidspunkt,
                    null
                ))
            .toList();

        inngangsvilkårVurderingRepository.lagreBostedVurderinger(param.getBehandlingId(), periodeVurderinger);
        inngangsvilkårVurderingTjeneste.settBostedsvilkårResultat(param.getBehandlingId(), param.getVilkårResultatBuilder());

        historikkinnslagInput
            .setSkjermlenkeType(SkjermlenkeType.BOSTEDSVILKÅR)
            .setNyeVilkårVurderinger(inngangsvilkårVurderingRepository.hentVurderingTidslinje(param.getBehandlingId(), AKTUELT_VILKÅR))
            .setHistorikkAktør(HistorikkAktør.NAV_KONTOR_SAKSBEHANDLER);
        vilkårsvurderingHistorikkinnslagTjeneste.lagreHistorikkinnslag(historikkinnslagInput);

        return OppdateringResultat.nyttResultat();
    }

    private void validerAvkortingBruktRiktig(ManuellVurderingBostedsvilkårDto dto, Long behandlingId) {
        LocalDateTimeline<Boolean> perioderSattTilAvkortet = new LocalDateTimeline<>(dto.getVurdertePerioder().stream()
            .filter(f -> f.avslagsårsak() == BostedsvilkårIkkeOppfyltÅrsak.AVKORTET)
            .map(it -> new LocalDateSegment<>(it.periode().getFom(), it.periode().getTom(), true))
            .toList());
        avkortTjeneste.validerAvkortBruktRiktig(behandlingId, perioderSattTilAvkortet, AKTUELT_VILKÅR);
    }
}
