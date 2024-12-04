package no.nav.ung.sak.ytelse.ung.uttak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.sak.ytelse.ung.beregning.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.ytelse.ung.periode.UngdomsprogramPeriodeTjeneste;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_ANTALL_DAGER;
import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;
import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_BEGYNNELSE;

@FagsakYtelseTypeRef(UNGDOMSYTELSE)
@BehandlingStegRef(value = VURDER_ANTALL_DAGER)
@BehandlingTypeRef
@ApplicationScoped
public class VurderAntallDagerSteg implements BehandlingSteg {

    private VilkårTjeneste vilkårTjeneste;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;

    @Inject
    public VurderAntallDagerSteg(VilkårTjeneste vilkårTjeneste, UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository, UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste) {
        this.vilkårTjeneste = vilkårTjeneste;
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
    }

    public VurderAntallDagerSteg() {
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        var samletVilkårResultatTidslinje = vilkårTjeneste.samletVilkårsresultat(behandlingId);
        var godkjentePerioder = samletVilkårResultatTidslinje
            .mapValue(it -> it.getSamletUtfall().equals(Utfall.OPPFYLT))
            .filterValue(Boolean.TRUE::equals);

        var ungdomsprogramtidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandlingId);
        if (!godkjentePerioder.isEmpty()) {
            ungdomsprogramtidslinje = ungdomsprogramtidslinje
                // Fjerner deler av programperiode som er etter søkte perioder.
                .intersection(new LocalDateTimeline<>(TIDENES_BEGYNNELSE, godkjentePerioder.getMaxLocalDate(), true));
        }

        var ungdomsytelseUttakPerioder = VurderAntallDagerTjeneste.vurderAntallDagerOgLagUttaksperioder(godkjentePerioder, ungdomsprogramtidslinje);
        ungdomsytelseUttakPerioder.ifPresent(it -> ungdomsytelseGrunnlagRepository.lagre(behandlingId, it));
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }


}
