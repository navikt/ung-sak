package no.nav.ung.ytelse.aktivitetspenger.medlemskap;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapPeriode;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapRepository;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.kontrakt.vilkår.medlemskap.MedlemskapDto;
import no.nav.ung.sak.kontrakt.vilkår.medlemskap.UtenlandsoppholdDto;
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

    public List<MedlemskapDto> hentMedlemskapForBehandlingSomDto(Long behandlingId) {
        Optional<OppgittForutgåendeMedlemskapGrunnlag> grunnlagOpt = forutgåendeMedlemskapRepository.hentGrunnlagHvisEksisterer(behandlingId);
        if (grunnlagOpt.isEmpty()) {
            return Collections.emptyList();
        }
        var grunnlag = grunnlagOpt.get();

        return grunnlag.getOppgittePerioder().stream()
            .map(ForutgåendeMedlemskapTjeneste::mapTilDto)
            .toList();
    }

    private static MedlemskapDto mapTilDto(OppgittForutgåendeMedlemskapPeriode m) {
        return new MedlemskapDto(
            m.getPeriode().tilPeriode(),
            m.harBoddINorge(),
            m.harJobbetINorge(),
            m.harJobbetUtenforNorge(),
            m.getJournalpostId().getVerdi(),
            m.getUtenlandsopphold().stream().map(u ->
                new UtenlandsoppholdDto(
                    new Periode(u.getPeriode().getFomDato(), u.getPeriode().getTomDato()),
                    mapLandTilNorskNavn(u.getLand().getKode()),
                    u.getLand().getKode(),
                    TrygdeavtaleLandOppslag.erGyldigTrygdeavtaleLand(u.getLand(), u.getPeriode().getFomDato()),
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
