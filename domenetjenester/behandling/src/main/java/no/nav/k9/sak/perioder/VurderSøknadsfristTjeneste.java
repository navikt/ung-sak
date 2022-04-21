package no.nav.k9.sak.perioder;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.perioder.VurdertSøktPeriode.SøktPeriodeData;

public interface VurderSøknadsfristTjeneste<T extends SøktPeriodeData> {

    static <T extends SøktPeriodeData> VurderSøknadsfristTjeneste<T> finnSøknadsfristTjeneste(Instance<VurderSøknadsfristTjeneste<T>> søknadsfristTjenester, FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseTypeRef.Lookup.find(søknadsfristTjenester, fagsakYtelseType)
            .orElseThrow(() -> new IllegalStateException("Har ikke " + VurderSøknadsfristTjeneste.class.getSimpleName() + " for ytelse=" + fagsakYtelseType));
    }

    Map<KravDokument, List<VurdertSøktPeriode<T>>> vurderSøknadsfrist(BehandlingReferanse referanse);

    Map<KravDokument, List<SøktPeriode<T>>> hentPerioderTilVurdering(BehandlingReferanse referanse);

    /**
     * Kjøres på nytt etter løsing av aksjonspunkt for å sikre at alle periodene er tatt hensyn til.
     * NB! Husk å sjekk om periodene har blitt satt til OK eller IKKE OK av saksbehandler og sett status i henhold
     *
     * @param behandlingId
     * @param søknaderMedPerioder periodene
     * @return resultatet
     */
    Map<KravDokument, List<VurdertSøktPeriode<T>>> vurderSøknadsfrist(Long behandlingId, Map<KravDokument, List<SøktPeriode<T>>> søknaderMedPerioder);

    /**
     * Henter ut kravdokumenter som har tilkommet i denne behandlingen
     * Tar ikke hensyn til manuell revurdering (dvs viser kun dokumenter tilkommet i behandlingen)
     *
     * @param referanse referansen til behandlingen
     * @return kravdokumenter
     */
    public default Set<KravDokument> relevanteKravdokumentForBehandling(BehandlingReferanse referanse) {
        return relevanteKravdokumentForBehandling(referanse, false);
    }

    Set<KravDokument> relevanteKravdokumentForBehandling(BehandlingReferanse referanse, boolean taHensynTilManuellRevurdering);

    /**
     * Henter ut kravdokumenter med perioder som har tilkommet i denne behandlingen
     *
     * @param referanse referansen til behandlingen
     * @return kravdokumenter
     */
    public default Map<KravDokument, List<SøktPeriode<T>>> relevanteKravdokumentMedPeriodeForBehandling(BehandlingReferanse referanse) {
        var kravDokumentListMap = hentPerioderTilVurdering(referanse);
        var relevanteKrav = relevanteKravdokumentForBehandling(referanse);
        return kravDokumentListMap.entrySet()
            .stream()
            .filter(it -> relevanteKrav.stream().anyMatch(at -> at.getJournalpostId().equals(it.getKey().getJournalpostId())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    /**
     * Henter ut kravdokumenter med perioder som har tilkommet i denne behandlingen(tar hensyn til manuell revurdering og lister alle dokumenter)
     * og vurder status
     *
     * @param referanse referansen til behandlingen
     * @return kravdokumenter
     */
    public default Map<KravDokument, List<VurdertSøktPeriode<T>>> relevanteVurderteKravdokumentMedPeriodeForBehandling(BehandlingReferanse referanse) {
        var kravDokumentListMap = vurderSøknadsfrist(referanse);
        var relevanteKrav = relevanteKravdokumentForBehandling(referanse, true);
        return kravDokumentListMap.entrySet()
            .stream()
            .filter(it -> relevanteKrav.stream().anyMatch(at -> at.getJournalpostId().equals(it.getKey().getJournalpostId())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
