package no.nav.k9.sak.web.app.tjenester.behandling.søknadsfrist;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.k9.sak.kontrakt.søknadsfrist.KravDokumenType;
import no.nav.k9.sak.kontrakt.søknadsfrist.KravDokumentStatus;
import no.nav.k9.sak.kontrakt.søknadsfrist.SøknadsfristPeriodeDto;
import no.nav.k9.sak.kontrakt.søknadsfrist.SøknadsfristTilstandDto;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;

public class MapTilSøknadsfristDto {

    public SøknadsfristTilstandDto mapTil(Map<KravDokument, List<VurdertSøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> relevanteVurderteKravdokumentMedPeriodeForBehandling) {

        var dokumenterMedStatusOgPeriode = relevanteVurderteKravdokumentMedPeriodeForBehandling.entrySet().stream().map(this::mapDokumentMedPerioder).collect(Collectors.toList());

        return new SøknadsfristTilstandDto(dokumenterMedStatusOgPeriode);
    }

    private KravDokumentStatus mapDokumentMedPerioder(Map.Entry<KravDokument, List<VurdertSøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> it) {
        var key = it.getKey();
        return new KravDokumentStatus(KravDokumenType.fraKode(key.getType().name()), mapPerioder(it.getValue()), key.getInnsendingsTidspunkt(), key.getJournalpostId());
    }

    private List<SøknadsfristPeriodeDto> mapPerioder(List<VurdertSøktPeriode<VurdertSøktPeriode.SøktPeriodeData>> value) {
        return value.stream().map(it -> new SøknadsfristPeriodeDto(it.getPeriode().tilPeriode(), it.getUtfall())).collect(Collectors.toList());
    }
}
