package no.nav.ung.ytelse.aktivitetspenger.medlemskap;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapRepository;
import no.nav.ung.sak.kontrakt.vilkår.medlemskap.MedlemskapsPeriodeDto;
import no.nav.ung.sak.typer.Periode;

import java.util.*;

@Dependent
public class ForutgåendeMedlemskapTjeneste {

    private final OppgittForutgåendeMedlemskapRepository forutgåendeMedlemskapRepository;

    @Inject
    public ForutgåendeMedlemskapTjeneste(OppgittForutgåendeMedlemskapRepository forutgåendeMedlemskapRepository) {
        this.forutgåendeMedlemskapRepository = forutgåendeMedlemskapRepository;
    }

    public List<MedlemskapsPeriodeDto> hentBostederSomDto(Long behandlingId) {
        return forutgåendeMedlemskapRepository.hentGrunnlagHvisEksisterer(behandlingId)
            .map(ForutgåendeMedlemskapTjeneste::mapTilDto)
            .orElse(List.of());
    }

    private static List<MedlemskapsPeriodeDto> mapTilDto(OppgittForutgåendeMedlemskapGrunnlag grunnlag) {
        return grunnlag.getOppgittePerioder().stream()
            .flatMap(p -> p.getBostederUtland().stream().map(bosted -> {
                var landkode = bosted.getLandkode();
                var periode = bosted.getPeriode();

                return new MedlemskapsPeriodeDto(
                    new Periode(periode.getFomDato(), periode.getTomDato()),
                    mapLandTilNorskNavn(landkode),
                    landkode,
                    TrygdeavtaleLandOppslag.erGyldigTrygdeavtaleLand(landkode, periode.getFomDato()),
                    p.getJournalpostId().getVerdi()
                );
            })).toList();
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
