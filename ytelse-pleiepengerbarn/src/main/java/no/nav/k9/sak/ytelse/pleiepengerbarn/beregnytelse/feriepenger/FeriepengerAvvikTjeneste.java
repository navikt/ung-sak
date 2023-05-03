package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse.feriepenger;

import java.math.RoundingMode;
import java.time.Year;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingModell;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.ytelse.beregning.BeregnFeriepengerTjeneste;
import no.nav.k9.sak.ytelse.beregning.regelmodell.MottakerType;
import no.nav.k9.sak.ytelse.beregning.regler.feriepenger.FeriepengeOppsummering;

@Dependent
public class FeriepengerAvvikTjeneste {

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private BehandlingModellRepository behandlingModellRepository;
    private Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjenester;

    @Inject
    public FeriepengerAvvikTjeneste(FagsakRepository fagsakRepository,
                                    BehandlingRepository behandlingRepository,
                                    BeregningsresultatRepository beregningsresultatRepository,
                                    BehandlingModellRepository behandlingModellRepository,
                                    @Any Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjenester) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.behandlingModellRepository = behandlingModellRepository;
        this.beregnFeriepengerTjenester = beregnFeriepengerTjenester;
    }

    public boolean harKommetTilTilkjentYtelse(BehandlingReferanse ref) {
        final var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        final BehandlingStegType steg = behandling.getAktivtBehandlingSteg();
        final BehandlingModell modell = behandlingModellRepository.getModell(behandling.getType(), behandling.getFagsakYtelseType());
        return !modell.erStegAFørStegB(steg, BehandlingStegType.BEREGN_YTELSE);
    }

    public Set<Year> opptjeningsårFeirepengerMåReberegnes(BehandlingReferanse referanse) {
        BeregningsresultatEntitet beregningsresultatEntitet = beregningsresultatRepository.hentEndeligBeregningsresultat(referanse.getBehandlingId()).orElse(null);

        FeriepengeOppsummering innvilgedeFeriepenger = finnInnvilgedeFeriepenger(beregningsresultatEntitet);
        FeriepengeOppsummering forskuttertResultatAvRevurdering = forskuttertResultatAvNyBeregning(referanse, beregningsresultatEntitet);

        return FeriepengeOppsummering.utledOpptjeningsårSomHarDifferanse(innvilgedeFeriepenger, forskuttertResultatAvRevurdering);
    }

    private FeriepengeOppsummering finnInnvilgedeFeriepenger(BeregningsresultatEntitet beregningsresultatEntitet) {
        if (beregningsresultatEntitet == null) {
            return FeriepengeOppsummering.tom();
        }
        FeriepengeOppsummering.Builder feriepengeOppsummeringBuilder = new FeriepengeOppsummering.Builder();
        for (BeregningsresultatPeriode brPeriode : beregningsresultatEntitet.getBeregningsresultatPerioder()) {
            for (BeregningsresultatAndel brAndel : brPeriode.getBeregningsresultatAndelList()) {
                long beløp = brAndel.getFeriepengerÅrsbeløp() != null
                    ? brAndel.getFeriepengerÅrsbeløp().getVerdi().setScale(0, RoundingMode.UNNECESSARY).longValue()
                    : 0L;
                int fomYear = brPeriode.getPeriode().getFomDato().getYear();
                int tomYear = brPeriode.getPeriode().getTomDato().getYear();
                if (fomYear != tomYear) {
                    throw new IllegalArgumentException("Har BeregningsresultatPeriode som både har feriepenger og krysser år, skal ikke forekomme");
                }
                feriepengeOppsummeringBuilder.leggTil(Year.of(fomYear), brAndel.erBrukerMottaker() ? MottakerType.BRUKER : MottakerType.ARBEIDSGIVER, brAndel.erBrukerMottaker() ? null : brAndel.getArbeidsforholdIdentifikator(), beløp);
            }
        }
        return feriepengeOppsummeringBuilder.build();
    }

    private FeriepengeOppsummering forskuttertResultatAvNyBeregning(BehandlingReferanse referanse, BeregningsresultatEntitet beregningsresultatEntitet) {
        if (beregningsresultatEntitet == null) {
            return FeriepengeOppsummering.tom();
        }
        BeregnFeriepengerTjeneste beregnFeriepengerTjeneste = BeregnFeriepengerTjeneste.finnTjeneste(beregnFeriepengerTjenester, referanse.getFagsakYtelseType());
        return beregnFeriepengerTjeneste.beregnFeriepengerOppsummering(referanse, beregningsresultatEntitet);
    }

}
