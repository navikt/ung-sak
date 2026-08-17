package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BostedsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bosted.ManuellVurderingBostedsvilkårDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bosted.VilkårBostedPeriodeVurderingDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.livsopphold.VurderAndreLivsoppholdsytelserDto;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.del1.avkort.AvkortTjeneste;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static no.nav.fpsak.tidsserie.LocalDateInterval.TIDENES_ENDE;
import static no.nav.ung.kodeverk.behandling.BehandlingÅrsakType.ENDRET_BOSTED;

/**
 * Oppdaterer for aksjonspunkt 5144 – manuell vurdering av bostedsvilkåret.
 * Brukes ved årsak ANNET, mottatt uttalelse fra bruker, eller auto-fakta fra søknad.
 */
@ApplicationScoped
@DtoTilServiceAdapter(dto = ManuellVurderingBostedsvilkårDto.class, adapter = AksjonspunktOppdaterer.class)
public class ManuellVurderingBostedsvilkårOppdaterer implements AksjonspunktOppdaterer<ManuellVurderingBostedsvilkårDto> {

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;
    private AvkortTjeneste avkortTjeneste;

    ManuellVurderingBostedsvilkårOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public ManuellVurderingBostedsvilkårOppdaterer(BehandlingRepository behandlingRepository,
                                                   VilkårResultatRepository vilkårResultatRepository,
                                                   InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository,
                                                   InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste,
                                                   HistorikkinnslagRepository historikkinnslagRepository,
                                                   AvkortTjeneste avkortTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
        this.inngangsvilkårVurderingTjeneste = inngangsvilkårVurderingTjeneste;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.avkortTjeneste = avkortTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(ManuellVurderingBostedsvilkårDto dto, AksjonspunktOppdaterParameter param) {
        Vilkårene vilkårene = vilkårResultatRepository.hentHvisEksisterer(param.getBehandlingId()).orElseThrow();
        LocalDateTimeline<VilkårPeriode> eksisterendeVilkårperioder = vilkårene.getVilkårTimeline(VilkårType.BOSTEDSVILKÅR)
            .filterValue(v -> v.getUtfall() != Utfall.IKKE_RELEVANT);

        var erEndretBosted =
            behandlingRepository.hentBehandling(param.getBehandlingId()).harBehandlingÅrsak(ENDRET_BOSTED);


        var senesteDatoFraVilkårsperiode = eksisterendeVilkårperioder.getMaxLocalDate();
        String vurdertAv = SubjectHandler.getSubjectHandler().getUid();
        LocalDateTime vurdertTidspunkt = LocalDateTime.now();

        var vurderteÅpnePerioder = new LocalDateTimeline<>(dto.getVurdertePerioder().stream()
            .filter(it -> it.periode().getTom() == null)
            .map(it -> new LocalDateSegment<>(it.periode().getFom(), hentMaksDatoVedÅpenPeriode(it.periode(), senesteDatoFraVilkårsperiode), it))
            .toList())
            .intersection(eksisterendeVilkårperioder);    // For å ikke innføre avslag der det er hull i eksisterende vilkårsperiode

        var vurderteLukkedePerioder = new LocalDateTimeline<>(dto.getVurdertePerioder().stream()
            .filter(it -> it.periode().getTom() != null)
            .map(it -> new LocalDateSegment<>(it.periode().getFom(), it.periode().getTom(), it))
            .toList());

        validerLukkedePerioder(vurderteLukkedePerioder, eksisterendeVilkårperioder, erEndretBosted);
        validerAvkortingBruktRiktig(dto, param.getBehandlingId());

        var periodeVurderinger = vurderteÅpnePerioder.crossJoin(vurderteLukkedePerioder).segmenter().stream()
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
                vurdertTidspunkt))
            .toList();

        inngangsvilkårVurderingRepository.lagreBostedVurderinger(param.getBehandlingId(), periodeVurderinger);

        inngangsvilkårVurderingTjeneste.settBostedsvilkårResultat(param.getBehandlingId(), param.getVilkårResultatBuilder());

        Behandling behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.LOKALKONTOR_SAKSBEHANDLER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medTittel(SkjermlenkeType.BOSTEDSVILKÅR)
            .addLinje("Manuell vurdering av bostedsvilkåret lagret")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);

        return OppdateringResultat.nyttResultat();
    }

    private static void validerLukkedePerioder(LocalDateTimeline<VilkårBostedPeriodeVurderingDto> vurderteLukkedePerioder, LocalDateTimeline<VilkårPeriode> eksisterendeVilkårperiode, boolean erEndretBosted) {
        LocalDateTimeline<VilkårBostedPeriodeVurderingDto> uforventedePerioder = vurderteLukkedePerioder.disjoint(eksisterendeVilkårperiode);
        if (!uforventedePerioder.isEmpty()) {
            throw new IllegalArgumentException("Forsøker å vurdere perioder som ikke er til vurdering. Gjelder perioder: " + uforventedePerioder);
        }

        LocalDateTimeline<?> manglendePerioder = eksisterendeVilkårperiode.disjoint(vurderteLukkedePerioder);
        if (!manglendePerioder.isEmpty() && !erEndretBosted) {
            // Brukers uttalelse kan føre til at ikke hele perioden opprinnelig satt til IKKE_VURDERT skal revurderes likevel.
            throw new IllegalArgumentException("Forventer at alle perioder til vurdering vurderes. Mangler : " + manglendePerioder);
        }
    }

    static LocalDate hentMaksDatoVedÅpenPeriode(Periode periode, LocalDate senesteTomVilkårsperiode) {
        var erÅpenPeriode = periode.getTom() == null || periode.getFom().equals(TIDENES_ENDE);
        return erÅpenPeriode ? senesteTomVilkårsperiode : periode.getTom();
    }

    private void validerAvkortingBruktRiktig(ManuellVurderingBostedsvilkårDto dto, Long behandlingId) {
        LocalDateTimeline<Boolean> perioderSattTilAvkortet = new LocalDateTimeline<>(dto.getVurdertePerioder().stream()
            .filter(f -> f.avslagsårsak() == BostedsvilkårIkkeOppfyltÅrsak.AVKORTET)
            .map(it -> new LocalDateSegment<>(it.periode().getFom(), it.periode().getTom(), true))
            .toList());
        LocalDateTimeline<Boolean> perioderSomKanSettesTilAvkortet = avkortTjeneste.tidslinjeForMuligAvkorting(behandlingId);
        LocalDateTimeline<Boolean> feilaktigAvkortet = perioderSomKanSettesTilAvkortet.disjoint(perioderSattTilAvkortet);
        if (!feilaktigAvkortet.isEmpty()) {
            throw new IllegalArgumentException("Følgende perioder kan ikke settes til avkortet: " + feilaktigAvkortet.getLocalDateIntervals());
        }
    }
}
