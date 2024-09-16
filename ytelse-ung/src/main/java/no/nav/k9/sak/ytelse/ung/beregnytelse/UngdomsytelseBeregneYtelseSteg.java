package no.nav.k9.sak.ytelse.ung.beregnytelse;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.BEREGN_YTELSE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.math.BigDecimal;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.behandling.steg.beregnytelse.BeregneYtelseSteg;
import no.nav.k9.sak.vilkår.VilkårTjeneste;

@FagsakYtelseTypeRef(UNGDOMSYTELSE)
@BehandlingStegRef(value = BEREGN_YTELSE)
@BehandlingTypeRef
@ApplicationScoped
public class UngdomsytelseBeregneYtelseSteg implements BeregneYtelseSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private VilkårTjeneste vilkårTjeneste;

    protected UngdomsytelseBeregneYtelseSteg() {
        // for proxy
    }

    @Inject
    public UngdomsytelseBeregneYtelseSteg(BehandlingRepositoryProvider repositoryProvider,
                                          VilkårTjeneste vilkårTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårTjeneste = vilkårTjeneste;
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        var samletResultat = vilkårTjeneste.samletVilkårsresultat(behandlingId);

        var oppfyltePerioder = samletResultat.filterValue(r -> r.getSamletUtfall().equals(Utfall.OPPFYLT)).compress().getLocalDateIntervals();

        if (!oppfyltePerioder.isEmpty()) {
            var beregningsresultatEntitet = BeregningsresultatEntitet.builder()
                .medRegelInput("")
                .medRegelSporing("")
                .build();

            oppfyltePerioder.stream().forEach(p -> {
                var resultatPeriode = BeregningsresultatPeriode.builder()
                    .medBeregningsresultatPeriodeFomOgTom(p.getFomDato(), p.getTomDato())
                    .build(beregningsresultatEntitet);
                BeregningsresultatAndel.builder()
                    .medDagsats(1000)
                    .medInntektskategori(Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER)
                    .medUtbetalingsgradOppdrag(BigDecimal.valueOf(100))
                    .medUtbetalingsgrad(BigDecimal.valueOf(100))
                    .medBrukerErMottaker(true)
                    .buildFor(resultatPeriode);
            });

            beregningsresultatRepository.lagre(behandling, beregningsresultatEntitet);

        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }


    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        beregningsresultatRepository.deaktiverBeregningsresultat(behandling.getId(), kontekst.getSkriveLås());
    }

}
