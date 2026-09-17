package no.nav.ung.ytelse.aktivitetspenger.medlemskap;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapPeriode;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapRepository;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.kontrakt.vilkår.medlemskap.MedlemskapDto;
import no.nav.ung.sak.kontrakt.vilkår.medlemskap.UtenlandsoppholdDto;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Periode;

import java.util.*;

@Dependent
public class ForutgåendeMedlemskapTjeneste {

    private final OppgittForutgåendeMedlemskapRepository forutgåendeMedlemskapRepository;
    private final MottatteDokumentRepository mottatteDokumentRepository;
    private final BehandlingRepository behandlingRepository;

    @Inject
    public ForutgåendeMedlemskapTjeneste(OppgittForutgåendeMedlemskapRepository forutgåendeMedlemskapRepository, MottatteDokumentRepository mottatteDokumentRepository, BehandlingRepository behandlingRepository) {
        this.forutgåendeMedlemskapRepository = forutgåendeMedlemskapRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.behandlingRepository = behandlingRepository;
    }

    public Optional<MedlemskapDto> hentMedlemskapRelevantForBehandlingSomDto(Long behandlingId) {
        Optional<OppgittForutgåendeMedlemskapGrunnlag> grunnlagOpt = forutgåendeMedlemskapRepository.hentGrunnlagHvisEksisterer(behandlingId);
        if (grunnlagOpt.isEmpty()) {
            return Optional.empty();
        }
        var grunnlag = grunnlagOpt.get();

        //Da grunnlagene kopierer fra tidligere behandlinger finner vi den nyeste relevant for denne behandlingen.
        var nyesteJournalpostId = finnNyesteJournalpostIdForGrunnlaget(behandlingId, grunnlag);

        return grunnlag.getOppgittePerioder().stream()
            .filter(p -> p.getJournalpostId().equals(nyesteJournalpostId))
            .map(ForutgåendeMedlemskapTjeneste::mapTilDto)
            .findFirst();
    }

    private JournalpostId finnNyesteJournalpostIdForGrunnlaget(Long behandlingId, OppgittForutgåendeMedlemskapGrunnlag grunnlag) {
        var journalpostIderFraGrunnlag = grunnlag.getOppgittePerioder().stream()
            .map(OppgittForutgåendeMedlemskapPeriode::getJournalpostId)
            .toList();

        var fagsakId = behandlingRepository.hentBehandling(behandlingId).getFagsakId();

        var mottatteDokumenter = mottatteDokumentRepository.hentMottatteDokument(fagsakId, journalpostIderFraGrunnlag);
        return mottatteDokumenter.stream()
            .max(Comparator.comparing(MottattDokument::getMottattTidspunkt))
            .map(MottattDokument::getJournalpostId)
            .orElseThrow();
    }

    private static MedlemskapDto mapTilDto(OppgittForutgåendeMedlemskapPeriode m) {
        return new MedlemskapDto(
            m.harBoddINorge(),
            m.harJobbetINorge(),
            m.harJobbetUtenforNorge(),
            m.getJournalpostId().getVerdi(),
            m.getUtenlandsopphold().stream().map(u ->
                new UtenlandsoppholdDto(
                    new Periode(u.getPeriode().getFomDato(), u.getPeriode().getTomDato()),
                    mapLandTilNorskNavn(u.getLandkode()),
                    u.getLandkode(),
                    u.harJobbetIPerioden(),
                    u.getUtenlandskNasjonalId()
                )).toList());
    }

    private static final Map<String, String> LANDKODE_TIL_NORSK_NAVN = lagLandkodeTilNorskNavn();

    private static Map<String, String> lagLandkodeTilNorskNavn() {
        Map<String, String> result = new HashMap<>();
        for (String alpha2 : Locale.getISOCountries()) {
            try {
                Locale locale = new Locale.Builder().setRegion(alpha2).build();
                result.put(locale.getISO3Country(), locale.getDisplayCountry(Locale.forLanguageTag("nb-NO")));
            } catch (MissingResourceException | IllformedLocaleException ignored) {
            }
        }
        return Map.copyOf(result);
    }

    private static String mapLandTilNorskNavn(String landkodeAlpha3) {
        return LANDKODE_TIL_NORSK_NAVN.getOrDefault(landkodeAlpha3, landkodeAlpha3);
    }
}
