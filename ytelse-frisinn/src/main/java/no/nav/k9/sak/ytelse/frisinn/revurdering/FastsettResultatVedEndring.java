package no.nav.k9.sak.ytelse.frisinn.revurdering;

import java.util.ArrayList;
import java.util.List;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.KonsekvensForYtelsen;
import no.nav.k9.kodeverk.vedtak.Vedtaksbrev;
import no.nav.k9.sak.behandling.revurdering.felles.HarEtablertYtelse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarsel;

class FastsettResultatVedEndring {

    FastsettResultatVedEndring() {
    }

    static VedtakVarsel fastsett(Behandling revurdering,
                                 VedtakVarsel vedtakVarsel,
                                 Betingelser er,
                                 HarEtablertYtelse harEtablertYtelse) {
        List<KonsekvensForYtelsen> konsekvenserForYtelsen = utledKonsekvensForYtelsen(er.endringIBeregning);

        if (!harEtablertYtelse.vurder(er.minstEnInnvilgetBehandlingUtenPåfølgendeOpphør)) {
            return harEtablertYtelse.fastsettForIkkeEtablertYtelse(revurdering);
        }
        Vedtaksbrev vedtaksbrev = utledVedtaksbrev(konsekvenserForYtelsen, er.varselOmRevurderingSendt);
        BehandlingResultatType behandlingResultatType = utledBehandlingResultatType(konsekvenserForYtelsen);

        vedtakVarsel.setVedtaksbrev(vedtaksbrev);
        revurdering.setBehandlingResultatType(behandlingResultatType);

        return vedtakVarsel;
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
