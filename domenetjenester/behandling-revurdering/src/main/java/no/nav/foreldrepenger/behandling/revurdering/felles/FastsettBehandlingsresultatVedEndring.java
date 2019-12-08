package no.nav.foreldrepenger.behandling.revurdering.felles;

import java.util.ArrayList;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;

class FastsettBehandlingsresultatVedEndring {
    private FastsettBehandlingsresultatVedEndring() {
    }

    public static Behandlingsresultat fastsett(Behandling revurdering,
                                               Betingelser er,
                                               HarEtablertYtelse harEtablertYtelse) {
        List<KonsekvensForYtelsen> konsekvenserForYtelsen = utledKonsekvensForYtelsen(er.endringIBeregning);

        if (!harEtablertYtelse.vurder(er.minstEnInnvilgetBehandlingUtenPåfølgendeOpphør)) {
            return harEtablertYtelse.fastsettForIkkeEtablertYtelse(revurdering, konsekvenserForYtelsen);
        }

        Vedtaksbrev vedtaksbrev = utledVedtaksbrev(konsekvenserForYtelsen, er.varselOmRevurderingSendt);
        BehandlingResultatType behandlingResultatType = utledBehandlingResultatType(konsekvenserForYtelsen);
        return buildBehandlingsresultat(revurdering, behandlingResultatType, konsekvenserForYtelsen, vedtaksbrev);
    }

    private static Vedtaksbrev utledVedtaksbrev(List<KonsekvensForYtelsen> konsekvenserForYtelsen, boolean erVarselOmRevurderingSendt) {
        if (!erVarselOmRevurderingSendt && konsekvenserForYtelsen.contains(KonsekvensForYtelsen.INGEN_ENDRING)) {
            return Vedtaksbrev.INGEN;
        }
        return Vedtaksbrev.AUTOMATISK;
    }

    private static BehandlingResultatType utledBehandlingResultatType(List<KonsekvensForYtelsen> konsekvenserForYtelsen) {
        if (konsekvenserForYtelsen.contains(KonsekvensForYtelsen.INGEN_ENDRING)) {
            return BehandlingResultatType.INGEN_ENDRING;
        }
        return BehandlingResultatType.INNVILGET_ENDRING;
    }

    private static List<KonsekvensForYtelsen> utledKonsekvensForYtelsen(boolean erEndringIBeregning) {
        List<KonsekvensForYtelsen> konsekvensForYtelsen = new ArrayList<>();

        if (erEndringIBeregning) {
            konsekvensForYtelsen.add(KonsekvensForYtelsen.ENDRING_I_BEREGNING);
        }
        if (konsekvensForYtelsen.isEmpty()) {
            konsekvensForYtelsen.add(KonsekvensForYtelsen.INGEN_ENDRING);
        }
        return konsekvensForYtelsen;
    }

    protected static Behandlingsresultat buildBehandlingsresultat(Behandling revurdering, BehandlingResultatType behandlingResultatType,
                                                                  List<KonsekvensForYtelsen> konsekvenserForYtelsen, Vedtaksbrev vedtaksbrev) {
        Behandlingsresultat behandlingsresultat = revurdering.getBehandlingsresultat();
        Behandlingsresultat.Builder behandlingsresultatBuilder = Behandlingsresultat.builderEndreEksisterende(behandlingsresultat);
        behandlingsresultatBuilder.medBehandlingResultatType(behandlingResultatType);
        behandlingsresultatBuilder.medVedtaksbrev(vedtaksbrev);
        konsekvenserForYtelsen.forEach(behandlingsresultatBuilder::leggTilKonsekvensForYtelsen);
        return behandlingsresultatBuilder.buildFor(revurdering);
    }

    static class Betingelser {
        boolean endringIBeregning;
        boolean varselOmRevurderingSendt;
        boolean minstEnInnvilgetBehandlingUtenPåfølgendeOpphør;

        private Betingelser() {
        }

        public static Betingelser fastsett(boolean erEndringIBeregning, 
                                           boolean erVarselOmRevurderingSendt,
                                           boolean erMinstEnInnvilgetBehandlingUtenPåfølgendeOpphør) {
            Betingelser b = new Betingelser();
            b.endringIBeregning = erEndringIBeregning;
            b.varselOmRevurderingSendt = erVarselOmRevurderingSendt;
            b.minstEnInnvilgetBehandlingUtenPåfølgendeOpphør = erMinstEnInnvilgetBehandlingUtenPåfølgendeOpphør;
            return b;
        }
    }
}
