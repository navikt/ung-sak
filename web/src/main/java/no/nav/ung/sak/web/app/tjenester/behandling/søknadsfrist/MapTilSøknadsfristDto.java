package no.nav.ung.sak.web.app.tjenester.behandling.søknadsfrist;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.ung.sak.behandlingslager.behandling.søknadsfrist.AvklartSøknadsfristResultat;
import no.nav.ung.sak.behandlingslager.behandling.søknadsfrist.KravDokumentHolder;
import no.nav.ung.sak.kontrakt.søknadsfrist.AvklarteOpplysninger;
import no.nav.ung.sak.kontrakt.søknadsfrist.KravDokumenType;
import no.nav.ung.sak.kontrakt.søknadsfrist.KravDokumentStatus;
import no.nav.ung.sak.kontrakt.søknadsfrist.SøknadsfristPeriodeDto;
import no.nav.ung.sak.kontrakt.søknadsfrist.SøknadsfristTilstandDto;
import no.nav.ung.sak.behandlingslager.behandling.startdato.VurdertSøktPeriode;
import no.nav.ung.sak.søknadsfrist.KravDokument;

public class MapTilSøknadsfristDto {

    public SøknadsfristTilstandDto mapTil(Map<KravDokument, List<VurdertSøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> relevanteVurderteKravdokumentMedPeriodeForBehandling, Optional<AvklartSøknadsfristResultat> avklartSøknadsfristResultat) {

        var dokumenterMedStatusOgPeriode = relevanteVurderteKravdokumentMedPeriodeForBehandling.entrySet().stream().map(it -> mapDokumentMedPerioder(it, avklartSøknadsfristResultat)).collect(Collectors.toList());

        return new SøknadsfristTilstandDto(dokumenterMedStatusOgPeriode);
    }

    private KravDokumentStatus mapDokumentMedPerioder(Map.Entry<KravDokument, List<VurdertSøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> it, Optional<AvklartSøknadsfristResultat> avklartSøknadsfristResultat) {
        var key = it.getKey();
        var avklarteOpplysninger = hentAvklarteOpplysninger(key, avklartSøknadsfristResultat);
        var overstyrteOpplysninger = hentOverstyrteOpplysninger(key, avklartSøknadsfristResultat);
        return new KravDokumentStatus(KravDokumenType.fraKode(key.getType().name()), mapPerioder(it.getValue()), key.getInnsendingsTidspunkt(), key.getJournalpostId(), avklarteOpplysninger, overstyrteOpplysninger);
    }

    private AvklarteOpplysninger hentOverstyrteOpplysninger(KravDokument key, Optional<AvklartSøknadsfristResultat> avklartSøknadsfristResultat) {
        return avklartSøknadsfristResultat.flatMap(søknadsfristResultat -> søknadsfristResultat.getOverstyrtHolder()
                .map(KravDokumentHolder::getDokumenter)
                .orElse(Set.of())
                .stream()
                .filter(it -> Objects.equals(it.getJournalpostId(), key.getJournalpostId()))
                .findFirst()
                .map(it -> new AvklarteOpplysninger(it.getErGodkjent(), it.getFraDato(), it.getBegrunnelse(), it.getOpprettetAv(), it.getOpprettetTidspunkt())))
            .orElse(null);
    }

    private AvklarteOpplysninger hentAvklarteOpplysninger(KravDokument key, Optional<AvklartSøknadsfristResultat> avklartSøknadsfristResultat) {
        return avklartSøknadsfristResultat.flatMap(søknadsfristResultat -> søknadsfristResultat.getAvklartHolder()
                .map(KravDokumentHolder::getDokumenter)
                .orElse(Set.of())
                .stream()
                .filter(it -> Objects.equals(it.getJournalpostId(), key.getJournalpostId()))
                .findFirst()
                .map(it -> new AvklarteOpplysninger(it.getErGodkjent(), it.getFraDato(), it.getBegrunnelse(), it.getOpprettetAv(), it.getOpprettetTidspunkt())))
            .orElse(null);
    }

    private List<SøknadsfristPeriodeDto> mapPerioder(List<VurdertSøktPeriode<VurdertSøktPeriode.SøktPeriodeData>> value) {
        return value.stream().map(it -> new SøknadsfristPeriodeDto(it.getPeriode().tilPeriode(), it.getUtfall())).collect(Collectors.toList());
    }
}
