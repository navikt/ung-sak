package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
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
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.VurderAndreLivsoppholdsytelserDto;
import no.nav.ung.sak.kontrakt.vilkår.VilkårPeriodeVurderingDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderAndreLivsoppholdsytelserDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderAndreLivsoppholdsytelserOppdaterer implements AksjonspunktOppdaterer<VurderAndreLivsoppholdsytelserDto> {

    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    VurderAndreLivsoppholdsytelserOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderAndreLivsoppholdsytelserOppdaterer(BehandlingRepository behandlingRepository,
                                                    HistorikkinnslagRepository historikkinnslagRepository, VilkårResultatRepository vilkårResultatRepository) {
        this.behandlingRepository = behandlingRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    @Override
    public OppdateringResultat oppdater(VurderAndreLivsoppholdsytelserDto dto, AksjonspunktOppdaterParameter param) {
        Vilkårene vilkårene = vilkårResultatRepository.hentHvisEksisterer(param.getBehandlingId()).orElseThrow();
        LocalDateTimeline<VilkårPeriode> perioderTilVurdering = vilkårene.getVilkårTimeline(VilkårType.BOSTEDSVILKÅR)
            .filterValue(v -> v.getUtfall() != Utfall.IKKE_RELEVANT);

        LocalDateTimeline<Boolean> inputOppdateres = new LocalDateTimeline<>(dto.getVurdertePerioder().stream().map(it -> new LocalDateSegment<>(it.periode().getFom(), it.periode().getTom(), true)).toList());

        LocalDateTimeline<Boolean> uforventedePerioder = inputOppdateres.disjoint(perioderTilVurdering);
        if (!uforventedePerioder.isEmpty()) {
            throw new IllegalArgumentException("Forsøker å vurdere perioder som ikke er til vurdering. Gjelder perioder: " + uforventedePerioder);
        }
        LocalDateTimeline<?> manglendePerioder = perioderTilVurdering.disjoint(inputOppdateres);
        if (!manglendePerioder.isEmpty()) {
            throw new IllegalArgumentException("Forventer at alle perioder til vurdering vurderes. Mangler : " + manglendePerioder);
        }

        var resultatBuilder = param.getVilkårResultatBuilder();
        var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR);
        for (VilkårPeriodeVurderingDto vurdertPeriode : dto.getVurdertePerioder()) {
            Utfall utfall = vurdertPeriode.erVilkårOppfylt() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(vurdertPeriode.periode().getFom(), vurdertPeriode.periode().getTom())
                .medUtfallManuell(utfall)
                .medAvslagsårsak(vurdertPeriode.avslagsårsak())
                .medBegrunnelse(vurdertPeriode.begrunnelse()));
        }
        resultatBuilder.leggTil(vilkårBuilder);

        Behandling behandling = behandlingRepository.hentBehandling(param.getBehandlingId());

        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.LOKALKONTOR_SAKSBEHANDLER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medTittel(SkjermlenkeType.VURDER_ANDRE_LIVSOPPHOLDSYTELSER)
            .addLinje("Det ble vurdert om søker har andre livsoppholdsytelser som ikke er forenelig med denne ytelsen")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);

        return OppdateringResultat.nyttResultat();
    }

}
