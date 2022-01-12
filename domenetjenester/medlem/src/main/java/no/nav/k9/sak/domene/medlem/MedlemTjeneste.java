package no.nav.k9.sak.domene.medlem;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.medlem.VurderingsÅrsak;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.diff.DiffResult;
import no.nav.k9.sak.domene.medlem.api.Medlemskapsperiode;
import no.nav.k9.sak.domene.medlem.impl.HentMedlemskapFraRegister;
import no.nav.k9.sak.domene.medlem.impl.MedlemResultat;
import no.nav.k9.sak.typer.AktørId;

@Dependent
public class MedlemTjeneste {

    private static Map<MedlemResultat, AksjonspunktDefinisjon> mapMedlemResulatTilAkDef = new EnumMap<>(MedlemResultat.class);

    static {
        mapMedlemResulatTilAkDef.put(MedlemResultat.AVKLAR_OM_ER_BOSATT, AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT);
        mapMedlemResulatTilAkDef.put(MedlemResultat.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE, AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE);
        mapMedlemResulatTilAkDef.put(MedlemResultat.AVKLAR_LOVLIG_OPPHOLD, AksjonspunktDefinisjon.AVKLAR_LOVLIG_OPPHOLD);
        mapMedlemResulatTilAkDef.put(MedlemResultat.AVKLAR_OPPHOLDSRETT, AksjonspunktDefinisjon.AVKLAR_OPPHOLDSRETT);
    }

    private MedlemskapRepository medlemskapRepository;
    private HentMedlemskapFraRegister hentMedlemskapFraRegister;
    private UtledVurderingsdatoerForMedlemskapTjeneste utledVurderingsdatoerTjeneste;
    private VurderMedlemskapTjeneste vurderMedlemskapTjeneste;

    MedlemTjeneste() {
        // CDI
    }

    @Inject
    public MedlemTjeneste(BehandlingRepositoryProvider repositoryProvider,
                          HentMedlemskapFraRegister hentMedlemskapFraRegister,
                          UtledVurderingsdatoerForMedlemskapTjeneste utledVurderingsdatoerForMedlemskapTjeneste,
                          VurderMedlemskapTjeneste vurderMedlemskapTjeneste) {
        this.hentMedlemskapFraRegister = hentMedlemskapFraRegister;
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.utledVurderingsdatoerTjeneste = utledVurderingsdatoerForMedlemskapTjeneste;
        this.vurderMedlemskapTjeneste = vurderMedlemskapTjeneste;
    }

    /**
     * Finn medlemskapsperioder i MEDL2 register for en person.
     *
     * @param aktørId aktøren det skal innhentes informasjon om.
     * @param fom     periode start for innhenting
     * @param tom     periode slutt for innhenting
     * @return Liste av medlemsperioder funnet
     */
    public List<Medlemskapsperiode> finnMedlemskapPerioder(AktørId aktørId, LocalDate fom, LocalDate tom) {
        return hentMedlemskapFraRegister.finnMedlemskapPerioder(aktørId, fom, tom);
    }

    public Optional<MedlemskapAggregat> hentMedlemskap(Long behandlingId) {
        return medlemskapRepository.hentMedlemskap(behandlingId);
    }

    public EndringsresultatSnapshot finnAktivGrunnlagId(Long behandlingId) {
        Optional<Long> funnetId = medlemskapRepository.hentIdPåAktivMedlemskap(behandlingId);
        return funnetId
            .map(id -> EndringsresultatSnapshot.medSnapshot(MedlemskapAggregat.class, id))
            .orElse(EndringsresultatSnapshot.utenSnapshot(MedlemskapAggregat.class));
    }

    public Map<LocalDate, VurderMedlemskap> utledVurderingspunkterMedAksjonspunkt(BehandlingReferanse ref) {
        final Map<LocalDate, Set<VurderingsÅrsak>> vurderingsdatoer = utledVurderingsdatoerTjeneste.finnVurderingsdatoerMedÅrsak(ref.getBehandlingId());
        final HashMap<LocalDate, VurderMedlemskap> map = new HashMap<>();
        for (Map.Entry<LocalDate, Set<VurderingsÅrsak>> entry : vurderingsdatoer.entrySet()) {
            LocalDate vurderingsdato = entry.getKey();
            final Set<MedlemResultat> vurderinger = vurderMedlemskapTjeneste.vurderMedlemskap(ref, vurderingsdato);
            if (!vurderinger.isEmpty()) {
                map.put(vurderingsdato, mapTilVurderMeldemspa(vurderinger, entry.getValue()));
            }
        }
        return map;
    }

    private VurderMedlemskap mapTilVurderMeldemspa(Set<MedlemResultat> vurderinger, Set<VurderingsÅrsak> vurderingsÅrsaks) {
        final Set<AksjonspunktDefinisjon> aksjonspunkter = vurderinger.stream()
            .map(vu -> mapMedlemResulatTilAkDef.get(vu))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        return new VurderMedlemskap(aksjonspunkter, vurderingsÅrsaks);
    }

    public DiffResult diffResultat(EndringsresultatDiff idDiff, boolean kunSporedeEndringer) {
        Objects.requireNonNull(idDiff.getGrunnlagId1(), "kan ikke diffe når id1 ikke er oppgitt");
        Objects.requireNonNull(idDiff.getGrunnlagId2(), "kan ikke diffe når id2 ikke er oppgitt");

        return medlemskapRepository.diffResultat((Long) idDiff.getGrunnlagId1(), (Long) idDiff.getGrunnlagId2(), kunSporedeEndringer);
    }
}
