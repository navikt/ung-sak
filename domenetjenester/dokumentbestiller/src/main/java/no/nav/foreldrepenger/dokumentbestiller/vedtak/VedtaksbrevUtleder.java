package no.nav.foreldrepenger.dokumentbestiller.vedtak;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.dokumentbestiller.BrevFeil;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;

public class VedtaksbrevUtleder {

    private VedtaksbrevUtleder() {
    }

    static boolean erAvlåttEllerOpphørt(BehandlingVedtak behandlingVedtak) {
        return VedtakResultatType.AVSLAG.equals(behandlingVedtak.getVedtakResultatType());
    }

    static boolean erKlageBehandling(BehandlingVedtak behandlingVedtak) {
        return VedtakResultatType.VEDTAK_I_KLAGEBEHANDLING.equals(behandlingVedtak.getVedtakResultatType());
    }

    static boolean erAnkeBehandling(BehandlingVedtak behandlingVedtak) {
        return VedtakResultatType.VEDTAK_I_ANKEBEHANDLING.equals(behandlingVedtak.getVedtakResultatType());
    }

    static boolean erInnvilget(BehandlingVedtak behandlingVedtak) {
        return VedtakResultatType.INNVILGET.equals(behandlingVedtak.getVedtakResultatType());
    }

    static boolean erLagetFritekstBrev(Behandlingsresultat behandlingsresultat) {
        return Vedtaksbrev.FRITEKST.equals(behandlingsresultat.getVedtaksbrev());
    }

    public static DokumentMalType velgDokumentMalForVedtak(Behandlingsresultat behandlingsresultat,
                                                           BehandlingVedtak behandlingVedtak) {
        Behandling behandling = behandlingsresultat.getBehandling();

        DokumentMalType dokumentMal = null;

        if (erLagetFritekstBrev(behandlingsresultat)) {
            dokumentMal = DokumentMalType.FRITEKST_DOK;
        } else if (erRevurderingMedUendretUtfall(behandlingVedtak)) {
            dokumentMal = DokumentMalType.UENDRETUTFALL_DOK;
        } else if (erInnvilget(behandlingVedtak)) {
            dokumentMal = velgPositivtVedtaksmal(behandling);
        } else if (erAvlåttEllerOpphørt(behandlingVedtak)) {
            dokumentMal = velgNegativVedtaksmal(behandling, behandlingsresultat);
        }
        if (dokumentMal == null) {
            throw BrevFeil.FACTORY.ingenBrevmalKonfigurert(behandling.getId()).toException();
        }
        return dokumentMal;
    }

    static boolean erRevurderingMedUendretUtfall(BehandlingVedtak vedtak) {
        return vedtak.isBeslutningsvedtak();
    }

    public static DokumentMalType velgNegativVedtaksmal(@SuppressWarnings("unused") Behandling behandling, Behandlingsresultat behandlingsresultat) {
        if (behandlingsresultat.isBehandlingsresultatOpphørt()) {
            return DokumentMalType.OPPHØR_DOK;
        } else {
            return DokumentMalType.AVSLAG__DOK;
        }
    }

    public static DokumentMalType velgPositivtVedtaksmal(@SuppressWarnings("unused") Behandling behandling) {
        return DokumentMalType.INNVILGELSE_DOK;
    }
}
