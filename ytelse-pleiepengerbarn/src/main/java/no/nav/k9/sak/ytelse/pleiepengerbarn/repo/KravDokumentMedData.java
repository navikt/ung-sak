package no.nav.k9.sak.ytelse.pleiepengerbarn.repo;

import java.util.Objects;

import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;

public class KravDokumentMedData implements Comparable<KravDokumentMedData> {
    private final KravDokument kravDokument;
    private final PerioderFraSøknad perioderFraSøknad;

    public KravDokumentMedData(KravDokument kravDokument, PerioderFraSøknad perioderFraSøknad) {
        this.kravDokument = Objects.requireNonNull(kravDokument);
        this.perioderFraSøknad = perioderFraSøknad;
    }

    @Override
    public int compareTo(KravDokumentMedData o) {
        return kravDokument.compareTo(o.kravDokument);
    }

    public KravDokument getKravDokument() {
        return kravDokument;
    }

    public PerioderFraSøknad getPerioderFraSøknad() {
        return perioderFraSøknad;
    }

    @Override
    public String toString() {
        return "KravDokumentMedData{" +
            "kravDokument=" + kravDokument +
            '}';
    }
}
