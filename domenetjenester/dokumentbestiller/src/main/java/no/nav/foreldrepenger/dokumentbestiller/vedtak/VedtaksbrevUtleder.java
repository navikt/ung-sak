package no.nav.foreldrepenger.dokumentbestiller.vedtak;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.dokumentbestiller.BrevFeil;
import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.kodeverk.vedtak.Vedtaksbrev;

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
        DokumentMalType dokumentMal = null;

        if (erLagetFritekstBrev(behandlingsresultat)) {
            dokumentMal = DokumentMalType.FRITEKST_DOK;
        } else if (erRevurderingMedUendretUtfall(behandlingVedtak)) {
            dokumentMal = DokumentMalType.UENDRETUTFALL_DOK;
        } else if (erInnvilget(behandlingVedtak)) {
            dokumentMal = DokumentMalType.INNVILGELSE_DOK;
        } else if (erAvlåttEllerOpphørt(behandlingVedtak)) {
            dokumentMal = behandlingsresultat.isBehandlingsresultatOpphørt() ? DokumentMalType.OPPHØR_DOK : DokumentMalType.AVSLAG__DOK;
        }
        if (dokumentMal == null) {
            throw BrevFeil.FACTORY.ingenBrevmalKonfigurert(behandlingsresultat.getBehandlingId()).toException();
        }
        return dokumentMal;
    }

    static boolean erRevurderingMedUendretUtfall(BehandlingVedtak vedtak) {
        return vedtak.isBeslutningsvedtak();
    }

}
