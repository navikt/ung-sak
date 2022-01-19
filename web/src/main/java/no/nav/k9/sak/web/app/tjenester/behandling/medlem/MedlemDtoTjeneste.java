package no.nav.k9.sak.web.app.tjenester.behandling.medlem;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.medlem.VurderingsÅrsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.VurdertLøpendeMedlemskapEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.VurdertMedlemskap;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.VurdertMedlemskapPeriodeEntitet;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.medlem.MedlemTjeneste;
import no.nav.k9.sak.domene.medlem.VurderMedlemskap;
import no.nav.k9.sak.kontrakt.medlem.MedlemPeriodeDto;
import no.nav.k9.sak.kontrakt.medlem.MedlemV2Dto;
import no.nav.k9.sak.kontrakt.medlem.MedlemskapPerioderDto;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.personopplysning.PersonopplysningDtoTjeneste;

@ApplicationScoped
public class MedlemDtoTjeneste {
    private MedlemskapRepository medlemskapRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandlingRepository behandlingRepository;
    private MedlemTjeneste medlemTjeneste;
    private PersonopplysningDtoTjeneste personopplysningDtoTjeneste;

    @Inject
    public MedlemDtoTjeneste(MedlemskapRepository medlemskapRepository,
                             BehandlingRepository behandlingRepository,
                             SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                             MedlemTjeneste medlemTjeneste,
                             PersonopplysningDtoTjeneste personopplysningDtoTjeneste) {

        this.medlemskapRepository = medlemskapRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.medlemTjeneste = medlemTjeneste;
        this.personopplysningDtoTjeneste = personopplysningDtoTjeneste;
    }

    MedlemDtoTjeneste() {
        // CDI
    }

    private static List<MedlemskapPerioderDto> lagMedlemskapPerioderDto(Set<MedlemskapPerioderEntitet> perioder) {
        return perioder.stream().map(mp -> {
            var dto = new MedlemskapPerioderDto();
            dto.setFom(mp.getFom());
            dto.setTom(mp.getTom());
            dto.setMedlemskapType(mp.getMedlemskapType());
            dto.setKildeType(mp.getKildeType());
            dto.setDekningType(mp.getDekningType());
            dto.setBeslutningsdato(mp.getBeslutningsdato());
            return dto;
        }).collect(Collectors.toList());
    }

    public Optional<MedlemV2Dto> lagMedlemPeriodisertDto(Long behandlingId) {
        var medlemskapOpt = medlemskapRepository.hentMedlemskap(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var dto = new MedlemV2Dto();
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
        mapRegistrerteMedlPerioder(dto, medlemskapOpt.map(MedlemskapAggregat::getRegistrertMedlemskapPerioder).orElse(Collections.emptySet()));

        if (behandling.getAksjonspunkter().stream().map(Aksjonspunkt::getAksjonspunktDefinisjon).toList().contains(AksjonspunktDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP)) {
            mapAndrePerioder(dto, medlemskapOpt.flatMap(MedlemskapAggregat::getVurderingLøpendeMedlemskap).map(VurdertMedlemskapPeriodeEntitet::getPerioder).orElse(Collections.emptySet()), ref);
        }
        return Optional.of(dto);
    }

    private void mapRegistrerteMedlPerioder(MedlemV2Dto dto, Set<MedlemskapPerioderEntitet> perioder) {
        dto.setMedlemskapPerioder(lagMedlemskapPerioderDto(perioder));
    }

    private void mapAndrePerioder(MedlemV2Dto dto, Set<VurdertLøpendeMedlemskapEntitet> perioder, BehandlingReferanse ref) {
        var vurderingspunkter = medlemTjeneste.utledVurderingspunkterMedAksjonspunkt(ref);
        Set<MedlemPeriodeDto> dtoPerioder = dto.getPerioder() != null ? new HashSet<>(dto.getPerioder()) : new HashSet<>();
        for (var entrySet : vurderingspunkter.entrySet()) {
            var medlemPeriodeDto = mapTilPeriodeDto(ref.getBehandlingId(), finnVurderMedlemskap(perioder, entrySet), entrySet.getKey(), entrySet.getValue().getÅrsaker());
            medlemPeriodeDto.setAksjonspunkter(entrySet.getValue().getAksjonspunkter().stream().map(Kodeverdi::getKode).collect(Collectors.toSet()));
            dtoPerioder.add(medlemPeriodeDto);
        }
        dto.setPerioder(dtoPerioder);
    }

    private Optional<VurdertMedlemskap> finnVurderMedlemskap(Set<VurdertLøpendeMedlemskapEntitet> perioder, Map.Entry<LocalDate, VurderMedlemskap> entrySet) {
        return perioder.stream()
            .filter(it -> it.getVurderingsdato().equals(entrySet.getKey())).map(it -> (VurdertMedlemskap) it).findAny();
    }

    private MedlemPeriodeDto mapTilPeriodeDto(Long behandlingId, Optional<VurdertMedlemskap> vurdertMedlemskapOpt, LocalDate vurderingsdato, Set<VurderingsÅrsak> årsaker) {
        var dto = new MedlemPeriodeDto();
        dto.setÅrsaker(årsaker);
        personopplysningDtoTjeneste.lagPersonopplysningDto(behandlingId, vurderingsdato).ifPresent(dto::setPersonopplysninger);
        dto.setVurderingsdato(vurderingsdato);

        if (vurdertMedlemskapOpt.isPresent()) {
            var vurdertMedlemskap = vurdertMedlemskapOpt.get();
            dto.setBosattVurdering(vurdertMedlemskap.getBosattVurdering());
            dto.setOppholdsrettVurdering(vurdertMedlemskap.getOppholdsrettVurdering());
            dto.setLovligOppholdVurdering(vurdertMedlemskap.getLovligOppholdVurdering());
            dto.setErEosBorger(vurdertMedlemskap.getErEøsBorger());
            dto.setMedlemskapManuellVurderingType(vurdertMedlemskap.getMedlemsperiodeManuellVurdering());
            dto.setBegrunnelse(vurdertMedlemskap.getBegrunnelse());
        }
        return dto;
    }

    // TODO(OJR) Hack!!! kan fjernes hvis man ønsker å utføre en migrerning(kompleks) av gamle medlemskapvurdering i produksjon
    private String hentBegrunnelseFraAksjonspuntk(Set<Aksjonspunkt> aksjonspunkter) {
        return aksjonspunkter.stream().filter(a -> VilkårType.MEDLEMSKAPSVILKÅRET.equals(a.getAksjonspunktDefinisjon().getVilkårType())).findFirst().map(Aksjonspunkt::getBegrunnelse).orElse(null);
    }
}
