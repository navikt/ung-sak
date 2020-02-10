package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetEntitet;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.AktivitetTomDatoMappingDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.AvklarAktiviteterDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.BeregningAktivitetDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FaktaOmBeregningDto;

@ApplicationScoped
public class AvklarAktiviteterDtoTjeneste {

    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    AvklarAktiviteterDtoTjeneste() {
        // For CDI
    }

    @Inject
    public AvklarAktiviteterDtoTjeneste(ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }

    /**  Modifiserer dto for fakta om beregning og setter dto for avklaring av aktiviteter på denne.
     *
     *
     * @param skjæringstidspunkt Skjæringstidspunkt for beregning
     * @param registerAktivitetAggregat aggregat for registeraktiviteter
     * @param saksbehandletAktivitetAggregat aggregat for saksbehandlede aktiviteter
     * @param forrigeRegisterAggregat aggregat for registeraktiviteter ved forrige bekreftelse av aksjonspunkt for avklaring av aktiviteter
     * @param forrigeSaksbehandletAggregat aggregat for saksbehandlede aktiviteter ved forrige bekreftelse av aksjonspunkt for avklaring av aktiviteter
     * @param faktaOmBeregningDto Dto for fakta om beregning som modifiseres
     */
    void lagAvklarAktiviteterDto(LocalDate skjæringstidspunkt,
                                 BeregningAktivitetAggregatEntitet registerAktivitetAggregat,
                                 Optional<BeregningAktivitetAggregatEntitet> saksbehandletAktivitetAggregat,
                                 Optional<BeregningAktivitetAggregatEntitet> forrigeRegisterAggregat,
                                 Optional<BeregningAktivitetAggregatEntitet> forrigeSaksbehandletAggregat,
                                 Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon,
                                 FaktaOmBeregningDto faktaOmBeregningDto) {
        AvklarAktiviteterDto avklarAktiviteterDto = new AvklarAktiviteterDto();
        List<BeregningAktivitetEntitet> beregningAktiviteter = registerAktivitetAggregat.getBeregningAktiviteter();
        List<BeregningAktivitetEntitet> saksbehandletAktiviteter = saksbehandletAktivitetAggregat
            .map(BeregningAktivitetAggregatEntitet::getBeregningAktiviteter)
            .orElse(Collections.emptyList());
        List<BeregningAktivitetEntitet> forrigeAktiviteter = forrigeRegisterAggregat
            .map(BeregningAktivitetAggregatEntitet::getBeregningAktiviteter)
            .orElse(Collections.emptyList());
        List<BeregningAktivitetEntitet> forrigeSaksbehandletAktiviteter = forrigeSaksbehandletAggregat
            .map(BeregningAktivitetAggregatEntitet::getBeregningAktiviteter)
            .orElse(Collections.emptyList());

        avklarAktiviteterDto.setAktiviteterTomDatoMapping(map(beregningAktiviteter, saksbehandletAktiviteter,
            forrigeAktiviteter, forrigeSaksbehandletAktiviteter, skjæringstidspunkt, arbeidsforholdInformasjon));
        faktaOmBeregningDto.setAvklarAktiviteter(avklarAktiviteterDto);
    }

    /** Lager map for fastsettelse av skjæringstidspunkt for beregning.
     *
     * Mapper fra mulige skjæringstidspunkt til aktiviteter som er aktive på dette tidspunktet.
     * Disse aktivitetene vil bli med videre i beregning.
     *
     * @param beregningAktiviteter registeraktiviteter
     * @param saksbehandletAktiviteter saksbehandlede aktiviteter
     * @param forrigeAktiviteter registeraktiviteter forrige gang aksjonspunktet ble løst
     * @param forrigeSaksbehandletAktiviteter saksbehandlede aktiviteter forrige gang aksjonspunktet ble løst
     * @param skjæringstidspunkt Skjæringstidspunkt for beregning
     * @return Liste med mappingobjekter som knytter eit mulig skjæringstidspunkt for beregning til eit sett med aktiviteter
     */
    private List<AktivitetTomDatoMappingDto> map(List<BeregningAktivitetEntitet> beregningAktiviteter, List<BeregningAktivitetEntitet> saksbehandletAktiviteter,
                                                 List<BeregningAktivitetEntitet> forrigeAktiviteter, List<BeregningAktivitetEntitet> forrigeSaksbehandletAktiviteter,
                                                 LocalDate skjæringstidspunkt, Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
        Map<LocalDate, List<BeregningAktivitetDto>> collect = beregningAktiviteter.stream()
            .map(aktivitet -> MapBeregningAktivitet.mapBeregningAktivitet(aktivitet, saksbehandletAktiviteter, forrigeAktiviteter, forrigeSaksbehandletAktiviteter, arbeidsforholdInformasjon, arbeidsgiverTjeneste))
            .collect(Collectors.groupingBy(beregningAktivitetDto -> finnTidligste(beregningAktivitetDto.getTom().plusDays(1), skjæringstidspunkt), Collectors.toList()));
        return collect.entrySet().stream()
            .map(entry -> {
                AktivitetTomDatoMappingDto dto = new AktivitetTomDatoMappingDto();
                dto.setTom(entry.getKey());
                dto.setAktiviteter(entry.getValue());
                return dto;
            })
            .sorted(Comparator.comparing(AktivitetTomDatoMappingDto::getTom).reversed())
            .collect(Collectors.toList());
    }

    private LocalDate finnTidligste(LocalDate tom, LocalDate skjæringstidspunkt) {
        if (tom.isAfter(skjæringstidspunkt)) {
            return skjæringstidspunkt;
        }
        return tom;
    }

}
