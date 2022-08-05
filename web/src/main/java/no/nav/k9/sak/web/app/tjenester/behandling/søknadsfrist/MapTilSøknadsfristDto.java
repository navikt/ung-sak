package no.nav.k9.sak.web.app.tjenester.behandling.søknadsfrist;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingslager.behandling.søknadsfrist.AvklartSøknadsfristResultat;
import no.nav.k9.sak.behandlingslager.behandling.søknadsfrist.KravDokumentHolder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.søknadsfrist.AvklarteOpplysninger;
import no.nav.k9.sak.kontrakt.søknadsfrist.KravDokumenType;
import no.nav.k9.sak.kontrakt.søknadsfrist.KravDokumentStatus;
import no.nav.k9.sak.kontrakt.søknadsfrist.SøknadsfristPeriodeDto;
import no.nav.k9.sak.kontrakt.søknadsfrist.SøknadsfristTilstandDto;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;

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
                .map(it -> new AvklarteOpplysninger(it.getErGodkjent(), it.getFraDato(), it.getBegrunnelse())))
            .orElse(null);
    }

    private AvklarteOpplysninger hentAvklarteOpplysninger(KravDokument key, Optional<AvklartSøknadsfristResultat> avklartSøknadsfristResultat) {
        return avklartSøknadsfristResultat.flatMap(søknadsfristResultat -> søknadsfristResultat.getAvklartHolder()
                .map(KravDokumentHolder::getDokumenter)
                .orElse(Set.of())
                .stream()
                .filter(it -> Objects.equals(it.getJournalpostId(), key.getJournalpostId()))
                .findFirst()
                .map(it -> new AvklarteOpplysninger(it.getErGodkjent(), it.getFraDato(), it.getBegrunnelse())))
            .orElse(null);
    }

    private List<SøknadsfristPeriodeDto> mapPerioder(List<VurdertSøktPeriode<VurdertSøktPeriode.SøktPeriodeData>> value) {
        return value.stream().map(it -> new SøknadsfristPeriodeDto(it.getPeriode().tilPeriode(), it.getUtfall())).collect(Collectors.toList());
    }

    public SøknadsfristTilstandDto mapTilV2(Map<KravDokument, List<VurdertSøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> kravDokumentListMap, Optional<AvklartSøknadsfristResultat> avklartSøknadsfristResultat, LocalDateTimeline<KravDokument> kravrekkefølgeForPerioderIBehandlingen) {
        var kravstatuser = kravrekkefølgeForPerioderIBehandlingen.stream()
            .map(segment -> mapPerioderV2(segment, kravDokumentListMap, avklartSøknadsfristResultat))
            .collect(Collectors.toList());
        return new SøknadsfristTilstandDto(kravstatuser);
    }

    private KravDokumentStatus mapPerioderV2(LocalDateSegment<KravDokument> segment, Map<KravDokument, List<VurdertSøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> kravDokumentListMap, Optional<AvklartSøknadsfristResultat> avklartSøknadsfristResultat) {
        return mapTilKravStatusForPeriode(segment, kravDokumentListMap, avklartSøknadsfristResultat);
    }

    private KravDokumentStatus mapTilKravStatusForPeriode(LocalDateSegment<KravDokument> segment, Map<KravDokument, List<VurdertSøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> kravDokumentListMap, Optional<AvklartSøknadsfristResultat> avklartSøknadsfristResultat) {
        var kravDokument = segment.getValue();
        var vurdertSøktPeriodes = kravDokumentListMap.get(kravDokument).stream().map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), it)).toList();
        var dokumentTimeline = new LocalDateTimeline<>(vurdertSøktPeriodes);

        var overlappendePerioder = dokumentTimeline.intersection(segment.getLocalDateInterval());
        var periodeDtoer = overlappendePerioder.stream().map(it -> new SøknadsfristPeriodeDto(DatoIntervallEntitet.fra(it.getLocalDateInterval()).tilPeriode(), it.getValue().getUtfall())).toList();

        return new KravDokumentStatus(KravDokumenType.fraKode(kravDokument.getType().name()),
            periodeDtoer,
            kravDokument.getInnsendingsTidspunkt(),
            kravDokument.getJournalpostId(),
            hentAvklarteOpplysninger(kravDokument, avklartSøknadsfristResultat),
            hentOverstyrteOpplysninger(kravDokument, avklartSøknadsfristResultat));
    }
}
