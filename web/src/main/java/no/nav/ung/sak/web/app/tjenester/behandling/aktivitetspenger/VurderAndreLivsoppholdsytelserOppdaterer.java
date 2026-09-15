package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetspengerInngangsvilkårResultatGrunnlag;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AndreLivsoppholdsytelserResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultat;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.livsopphold.VurderAndreLivsoppholdsytelserDto;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.del1.avkort.AvkortTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.historikkinnslag.HistorikkinnslagInput;
import no.nav.ung.ytelse.aktivitetspenger.historikkinnslag.VilkårsvurderingHistorikkinnslagTjeneste;

import java.time.LocalDateTime;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderAndreLivsoppholdsytelserDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderAndreLivsoppholdsytelserOppdaterer implements AksjonspunktOppdaterer<VurderAndreLivsoppholdsytelserDto> {

    private static final VilkårType AKTUELT_VILKÅR = VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR;

    private VilkårResultatRepository vilkårResultatRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;
    private AvkortTjeneste avkortTjeneste;
    private VilkårsvurderingHistorikkinnslagTjeneste vilkårsvurderingHistorikkinnslagTjeneste;

    VurderAndreLivsoppholdsytelserOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderAndreLivsoppholdsytelserOppdaterer(VilkårResultatRepository vilkårResultatRepository,
                                                    InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository,
                                                    InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste,
                                                    AvkortTjeneste avkortTjeneste,
                                                    VilkårsvurderingHistorikkinnslagTjeneste vilkårsvurderingHistorikkinnslagTjeneste) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
        this.inngangsvilkårVurderingTjeneste = inngangsvilkårVurderingTjeneste;
        this.avkortTjeneste = avkortTjeneste;
        this.vilkårsvurderingHistorikkinnslagTjeneste = vilkårsvurderingHistorikkinnslagTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderAndreLivsoppholdsytelserDto dto, AksjonspunktOppdaterParameter param) {
        LocalDateTimeline<VilkårsvurderingResultat> opprinneligVilkårsvurdering = inngangsvilkårVurderingRepository.hentVurderingTidslinje(param.getBehandlingId(), AKTUELT_VILKÅR);
        Vilkårene vilkårene = vilkårResultatRepository.hentHvisEksisterer(param.getBehandlingId()).orElseThrow();
        LocalDateTimeline<VilkårPeriode> perioderTilVurdering = vilkårene.getVilkårTimeline(AKTUELT_VILKÅR)
            .filterValue(v -> v.getUtfall() != Utfall.IKKE_RELEVANT);

        LocalDateTimeline<Boolean> inputOppdateres = new LocalDateTimeline<>(dto.getVurdertePerioder().stream().map(it -> new LocalDateSegment<>(it.periode().getFom(), it.periode().getTom(), true)).toList());

        LocalDateTimeline<Boolean> uforventedePerioder = inputOppdateres.disjoint(perioderTilVurdering);
        if (!uforventedePerioder.isEmpty()) {
            throw new IllegalArgumentException("Forsøker å vurdere perioder som ikke er til vurdering. Gjelder perioder: " + uforventedePerioder);
        }
        validerAvkortingBruktRiktig(dto, param.getBehandlingId());

        String vurdertAv = SubjectHandler.getSubjectHandler().getUid();
        LocalDateTime vurdertTidspunkt = LocalDateTime.now();
        var periodeVurderinger = dto.getVurdertePerioder().stream()
            .map(it -> new AndreLivsoppholdsytelserResultatPeriode(
                DatoIntervallEntitet.fraOgMedTilOgMed(it.periode().getFom(), it.periode().getTom()),
                it.erVilkårOppfylt(),
                it.avslagsårsak(),
                true,
                it.begrunnelse(),
                it.fritekstVurderingBrev(),
                vurdertAv,
                vurdertTidspunkt))
            .toList();
        inngangsvilkårVurderingRepository.lagreYtelseVurderinger(param.getBehandlingId(), periodeVurderinger);

        LocalDateTimeline<Boolean> vurdertEtterOppdatering = inngangsvilkårVurderingRepository.hentEksisterendeGrunnlag(param.getBehandlingId())
            .flatMap(AktivitetspengerInngangsvilkårResultatGrunnlag::getAndreLivsoppholdsytelserResultatHolder)
            .map(h -> new LocalDateTimeline<>(h.getVurderinger().stream()
                .map(v -> new LocalDateSegment<>(v.getPeriode().getFomDato(), v.getPeriode().getTomDato(), true))
                .toList()))
            .orElse(LocalDateTimeline.empty());
        LocalDateTimeline<?> manglendePerioder = perioderTilVurdering.disjoint(vurdertEtterOppdatering);
        if (!manglendePerioder.isEmpty()) {
            throw new IllegalArgumentException("Forventer at alle perioder til vurdering er vurdert. Mangler : " + manglendePerioder);
        }

        inngangsvilkårVurderingTjeneste.settAndreLivsoppholdsytelserResultat(param.getBehandlingId(), param.getVilkårResultatBuilder());

        HistorikkinnslagInput historikkinnslagInput = new HistorikkinnslagInput()
            .setSkjermlenkeType(SkjermlenkeType.VURDER_ANDRE_LIVSOPPHOLDSYTELSER)
            .setBehandlingId(param.getBehandlingId())
            .setEksisterendeVilkårVurderinger(opprinneligVilkårsvurdering)
            .setNyeVilkårVurderinger(inngangsvilkårVurderingRepository.hentVurderingTidslinje(param.getBehandlingId(), AKTUELT_VILKÅR))
            .setGjelderOpphør(false) //opphør ikke implementert enda for vilkåret
            .setHistorikkAktør(HistorikkAktør.LOKALKONTOR_SAKSBEHANDLER)
            .setSaksbehandlerIdent(vurdertAv);
        vilkårsvurderingHistorikkinnslagTjeneste.lagreHistorikkinnslag(historikkinnslagInput);

        return OppdateringResultat.nyttResultat();
    }

    private void validerAvkortingBruktRiktig(VurderAndreLivsoppholdsytelserDto dto, Long behandlingId) {
        LocalDateTimeline<Boolean> perioderSattTilAvkortet = new LocalDateTimeline<>(dto.getVurdertePerioder().stream()
            .filter(f -> f.avslagsårsak() == AndreLivsoppholdsytelserIkkeOppfyltÅrsak.AVKORTET)
            .map(it -> new LocalDateSegment<>(it.periode().getFom(), it.periode().getTom(), true))
            .toList());
        avkortTjeneste.validerAvkortBruktRiktig(behandlingId, perioderSattTilAvkortet, AKTUELT_VILKÅR);
    }

}
