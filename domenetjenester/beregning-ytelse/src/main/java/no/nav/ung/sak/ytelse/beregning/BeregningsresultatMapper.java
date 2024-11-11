package no.nav.ung.sak.ytelse.beregning;

import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatDto;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatMedUtbetaltePeriodeDto;

public interface BeregningsresultatMapper {

    BeregningsresultatDto map(Behandling behandling, BehandlingBeregningsresultatEntitet beregningsresultatAggregat);

    BeregningsresultatMedUtbetaltePeriodeDto mapMedUtbetaltePerioder(Behandling behandling, BehandlingBeregningsresultatEntitet bresAggregat);
}
