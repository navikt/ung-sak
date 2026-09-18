package no.nav.ung.ytelse.aktivitetspenger.medlemskap;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapPeriode;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.kontrakt.aktivitetspenger.medlemskap.MedlemskapAvslagsÅrsakType;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.kontrakt.vilkår.medlemskap.ForutgåendeMedlemskapResponse;
import no.nav.ung.sak.kontrakt.vilkår.medlemskap.MedlemskapDto;
import no.nav.ung.sak.kontrakt.vilkår.medlemskap.MedlemskapPeriodeInfoDto;
import no.nav.ung.sak.kontrakt.vilkår.medlemskap.UtenlandsoppholdDto;
import no.nav.ung.sak.typer.Periode;

import java.util.*;

@Dependent
public class ForutgåendeMedlemskapTjeneste {

    private final OppgittForutgåendeMedlemskapRepository forutgåendeMedlemskapRepository;
    private final BehandlingRepository behandlingRepository;
    private final StartdatoRepository startdatoRepository;
    private final VilkårResultatRepository vilkårResultatRepository;

    @Inject
    public ForutgåendeMedlemskapTjeneste(OppgittForutgåendeMedlemskapRepository forutgåendeMedlemskapRepository, BehandlingRepository behandlingRepository, VilkårResultatRepository vilkårResultatRepository, StartdatoRepository startdatoRepository) {
        this.forutgåendeMedlemskapRepository = forutgåendeMedlemskapRepository;
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.startdatoRepository = startdatoRepository;
    }


    public ForutgåendeMedlemskapResponse hentMedlemskapOgVilkårSomDto(BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());

        var medlemskap = hentMedlemskapForBehandlingSomDto(behandling.getId());

        var vilkår = vilkårResultatRepository.hent(behandling.getId())
            .getVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET)
            .orElseThrow(() -> new IllegalStateException("Mangler vilkårsvurdering av forutgående medlemskap"));

        var startdatoGrunnlag = startdatoRepository.hentGrunnlag(behandling.getId()).orElseThrow();
        var medlemskapsperiodeInfo = vilkår.getPerioder().stream()
            .filter(it -> it.getUtfall() != Utfall.IKKE_RELEVANT)
            .map(vp -> new MedlemskapPeriodeInfoDto(
                new Periode(vp.getPeriode().getFomDato(), vp.getPeriode().getTomDato()),
                vp.getGjeldendeUtfall(),
                mapAvslagsårsak(vp.getAvslagsårsak()),
                vp.getBegrunnelse(),
                finnOppgittMedlemskapRelevantForPerioden(vp, medlemskap, startdatoGrunnlag)
            ))
            .toList();

        return new ForutgåendeMedlemskapResponse(medlemskapsperiodeInfo);
    }

    private static MedlemskapDto finnOppgittMedlemskapRelevantForPerioden(VilkårPeriode vp, List<MedlemskapDto> medlemskap, StartdatoGrunnlag startdatoGrunnlag) {
        var relevantStartdatoGrunnlag = startdatoGrunnlag.getOppgitteStartdatoer().getStartdatoer().stream()
            .filter(startdato -> vp.getPeriode().overlapper(startdato.getStartdato(), startdato.getStartdato()))
            .findFirst();
        return relevantStartdatoGrunnlag
            .flatMap(v -> medlemskap.stream()
                .filter(m -> m.journalpostId().equals(v.getJournalpostId().getVerdi()))
                .findFirst())
            .orElse(null);

    }

    private static MedlemskapAvslagsÅrsakType mapAvslagsårsak(Avslagsårsak avslagsårsak) {
        if (avslagsårsak == null) return null;
        return switch (avslagsårsak) {
            case SØKER_ER_IKKE_MEDLEM -> MedlemskapAvslagsÅrsakType.SØKER_IKKE_MEDLEM;
            default -> throw new IllegalStateException("Unexpected value: " + avslagsårsak);
        };
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
