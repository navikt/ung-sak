package no.nav.k9.sak.dokument.bestill.vedtak;

import no.nav.k9.formidling.kontrakt.kodeverk.DokumentMalType;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.kodeverk.vedtak.Vedtaksbrev;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.dokument.bestill.BrevFeil;

public class VedtaksbrevUtleder {

    private VedtaksbrevUtleder() {
    }

    static boolean erAvlåttEllerOpphørt(BehandlingVedtak behandlingVedtak) {
        return VedtakResultatType.AVSLAG.equals(behandlingVedtak.getVedtakResultatType());
    }

    static boolean erInnvilget(BehandlingVedtak behandlingVedtak) {
        return VedtakResultatType.INNVILGET.equals(behandlingVedtak.getVedtakResultatType());
    }

    static boolean erLagetFritekstBrev(Vedtaksbrev behandlingsresultat) {
        return Vedtaksbrev.FRITEKST.equals(behandlingsresultat);
    }

    public static DokumentMalType velgDokumentMalForVedtak(BehandlingReferanse ref,
                                                           Vedtaksbrev vedtaksbrev,
                                                           BehandlingVedtak behandlingVedtak) {
        DokumentMalType dokumentMal = null;

        if (erLagetFritekstBrev(vedtaksbrev)) {
            dokumentMal = DokumentMalType.FRITEKST_DOK;
        } else if (erRevurderingMedUendretUtfall(behandlingVedtak)) {
            dokumentMal = DokumentMalType.UENDRETUTFALL_DOK;
        } else if (erInnvilget(behandlingVedtak)) {
            dokumentMal = DokumentMalType.INNVILGELSE_DOK;
        } else if (erAvlåttEllerOpphørt(behandlingVedtak)) {
            dokumentMal = ref.getBehandlingResultat().isBehandlingsresultatOpphørt() ? DokumentMalType.OPPHØR_DOK : DokumentMalType.AVSLAG__DOK;
        }
        if (dokumentMal == null) {
            throw BrevFeil.FACTORY.ingenBrevmalKonfigurert(behandlingVedtak.getBehandlingId()).toException();
        }
        return dokumentMal;
    }

    static boolean erRevurderingMedUendretUtfall(BehandlingVedtak vedtak) {
        return vedtak.isBeslutningsvedtak();
    }

}
