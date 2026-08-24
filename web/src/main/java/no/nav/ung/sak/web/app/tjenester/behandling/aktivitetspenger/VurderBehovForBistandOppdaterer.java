package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
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
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BistandsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand.VurderBehovForBistandDto;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.del1.avkort.AvkortTjeneste;

import java.time.LocalDateTime;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderBehovForBistandDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderBehovForBistandOppdaterer implements AksjonspunktOppdaterer<VurderBehovForBistandDto> {

    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;
    private AvkortTjeneste avkortTjeneste;

    VurderBehovForBistandOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderBehovForBistandOppdaterer(BehandlingRepository behandlingRepository,
                                           HistorikkinnslagRepository historikkinnslagRepository,
                                           VilkårResultatRepository vilkårResultatRepository,
                                           InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository,
                                           InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste,
                                           AvkortTjeneste avkortTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
        this.inngangsvilkårVurderingTjeneste = inngangsvilkårVurderingTjeneste;
        this.avkortTjeneste = avkortTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderBehovForBistandDto dto, AksjonspunktOppdaterParameter param) {
        Vilkårene vilkårene = vilkårResultatRepository.hentHvisEksisterer(param.getBehandlingId()).orElseThrow();
        LocalDateTimeline<VilkårPeriode> perioderTilVurdering = vilkårene.getVilkårTimeline(VilkårType.BISTANDSVILKÅR)
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
            .map(it -> new BistandsvilkårResultatPeriode(
                DatoIntervallEntitet.fraOgMedTilOgMed(it.periode().getFom(), it.periode().getTom()),
                it.erVilkårOppfylt(),
                it.avslagsårsak(),
                true,
                it.begrunnelse(),
                it.fritekstVurderingBrev(),
                vurdertAv,
                vurdertTidspunkt))
            .toList();
        inngangsvilkårVurderingRepository.lagreBistandsVurderinger(param.getBehandlingId(), periodeVurderinger);

        inngangsvilkårVurderingTjeneste.settBistandsvilkårResultat(param.getBehandlingId(), param.getVilkårResultatBuilder());

        Behandling behandling = behandlingRepository.hentBehandling(param.getBehandlingId());

        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.LOKALKONTOR_SAKSBEHANDLER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medTittel(SkjermlenkeType.BISTANDSVILKÅR)
            .addLinje("Bistandsvilkår ble vurdert")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);

        return OppdateringResultat.nyttResultat();
    }

    private void validerAvkortingBruktRiktig(VurderBehovForBistandDto dto, Long behandlingId) {
        LocalDateTimeline<Boolean> perioderSattTilAvkortet = new LocalDateTimeline<>(dto.getVurdertePerioder().stream()
            .filter(f -> f.avslagsårsak() == BistandsvilkårIkkeOppfyltÅrsak.AVKORTET)
            .map(it -> new LocalDateSegment<>(it.periode().getFom(), it.periode().getTom(), true))
            .toList());
        avkortTjeneste.validerAvkortBruktRiktig(behandlingId, perioderSattTilAvkortet, VilkårType.BISTANDSVILKÅR);
    }

}
